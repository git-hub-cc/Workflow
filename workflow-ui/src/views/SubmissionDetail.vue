<template>
  <div class="page-container">
    <a-page-header
        :title="formDefinition.name || '申请详情'"
        :sub-title="`提交人: ${submission.submitterName}`"
        @back="() => $router.go(-1)"
    />

    <a-spin :spinning="loading" tip="正在加载详情...">
      <div v-if="!loading" class="detail-layout">
        <!-- 左侧主内容区: 表单详情 -->
        <div class="main-content">
          <a-card title="表单内容">
            <!-- 使用 a-descriptions 组件以更美观地展示键值对数据 -->
            <a-descriptions bordered :column="1">
              <template v-for="field in flattenedFields" :key="field.id">
                <!-- 对富文本、子表单等复杂组件进行特殊渲染 -->
                <a-descriptions-item v-if="field.type === 'RichText'" :label="field.label" :span="1">
                  <div v-html="formData[field.id]" class="readonly-richtext"></div>
                </a-descriptions-item>

                <a-descriptions-item v-else-if="field.type === 'Subform'" :label="field.label" :span="1">
                  <a-table
                      :columns="getSubformColumns(field)"
                      :data-source="formData[field.id]"
                      :pagination="false"
                      bordered
                      size="small"
                  />
                </a-descriptions-item>

                <a-descriptions-item v-else-if="field.type === 'KeyValue'" :label="field.label" :span="1">
                  <a-descriptions :column="1" size="small" bordered>
                    <a-descriptions-item v-for="(item, idx) in formData[field.id]" :key="idx" :label="item.key">{{ item.value }}</a-descriptions-item>
                  </a-descriptions>
                </a-descriptions-item>

                <a-descriptions-item v-else-if="field.type === 'IconPicker'" :label="field.label">
                  <component v-if="formData[field.id]" :is="iconMap[formData[field.id]]" style="font-size: 24px;" />
                  <span v-else>(未选择)</span>
                </a-descriptions-item>

                <!-- 对普通字段进行默认渲染 -->
                <a-descriptions-item v-else :label="field.label">
                  {{ formatDisplayValue(field, formData[field.id]) }}
                </a-descriptions-item>
              </template>
            </a-descriptions>
          </a-card>
        </div>

        <!-- 右侧辅助信息区: 流程历史 & 附件 -->
        <div class="side-content">
          <a-card title="流程历史" style="margin-bottom: 24px;">
            <a-timeline>
              <a-timeline-item v-for="item in history" :key="item.activityId" :color="getTimelineColor(item)">
                <h4>{{ item.activityName }}</h4>
                <p v-if="item.assigneeName">处理人: {{ item.assigneeName }}</p>
                <p>开始: {{ item.startTime ? new Date(item.startTime).toLocaleString() : 'N/A' }}</p>
                <p v-if="item.endTime">结束: {{ new Date(item.endTime).toLocaleString() }}</p>
                <p v-if="item.durationInMillis">耗时: {{ formatDuration(item.durationInMillis) }}</p>
                <!-- ✨ 核心功能：展示审批意见 -->
                <p v-if="item.comment" class="approval-comment">
                  <strong>意见:</strong> {{ item.comment }}
                </p>
              </a-timeline-item>
            </a-timeline>
          </a-card>

          <a-card v-if="submission.attachments && submission.attachments.length > 0" title="相关附件">
            <div v-for="file in submission.attachments" :key="file.id" class="attachment-item">
              <a @click.prevent="handleDownload(file.id, file.originalFilename)" href="#">
                <PaperClipOutlined /> {{ file.originalFilename }}
              </a>
            </div>
          </a-card>
        </div>
      </div>
    </a-spin>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { getSubmissionById, getFormById, getWorkflowHistory, downloadFile } from '@/api';
import { message } from 'ant-design-vue';
import { PaperClipOutlined } from '@ant-design/icons-vue';
import { flattenFields } from '@/utils/formUtils.js';
import { iconMap } from '@/utils/iconLibrary.js';

const props = defineProps({ submissionId: String });

const loading = ref(true);
const submission = ref({});
const formDefinition = ref({ schema: { fields: [] } });
const formData = reactive({});
const history = ref([]);

// 将表单字段扁平化，并过滤掉纯布局和静态文本组件，方便在描述列表中渲染
const flattenedFields = computed(() => {
  if (!formDefinition.value.schema) return [];
  return flattenFields(formDefinition.value.schema.fields)
      .filter(f => !['GridRow', 'GridCol', 'Collapse', 'CollapsePanel', 'StaticText', 'DescriptionList'].includes(f.type));
});

onMounted(async () => {
  try {
    const subRes = await getSubmissionById(props.submissionId);
    submission.value = subRes;
    Object.assign(formData, JSON.parse(subRes.dataJson));

    // 为文件上传字段填充附件数据
    if (subRes.attachments) {
      const formDefForFile = await getFormById(subRes.formDefinitionId);
      const allFields = flattenFields(JSON.parse(formDefForFile.schemaJson).fields);
      const fileUploadField = allFields.find(f => f.type === 'FileUpload');
      if (fileUploadField) {
        formData[fileUploadField.id] = subRes.attachments;
      }
    }

    const [formRes, historyRes] = await Promise.all([
      getFormById(subRes.formDefinitionId),
      getWorkflowHistory(props.submissionId),
    ]);

    formRes.schema = JSON.parse(formRes.schemaJson);
    formDefinition.value = formRes;
    history.value = historyRes;

  } catch (error) {
    message.error('加载详情失败');
  } finally {
    loading.value = false;
  }
});

// --- 用于显示和格式化的辅助函数 ---

const getSubformColumns = (field) => field.props.columns.map(col => ({ title: col.label, dataIndex: col.id, key: col.id }));

const formatDisplayValue = (field, value) => {
  if (value === null || value === undefined || value === '') return '(未填写)';

  if (field.type === 'DatePicker' && value) {
    try { return new Date(value).toLocaleString(); } catch (e) { return value; }
  }

  if (typeof value === 'boolean') return value ? '是' : '否';

  if (Array.isArray(value)) return value.join(', ');

  // 尝试从静态数据源中查找显示标签
  if (field.dataSource && field.dataSource.options) {
    const findLabel = (options, val) => {
      for(const opt of options) {
        if (opt.value === val) return opt.label || opt.title;
        if (opt.children) {
          const found = findLabel(opt.children, val);
          if (found) return found;
        }
      }
      return null;
    }
    return findLabel(field.dataSource.options, value) || value;
  }

  return value;
};

const handleDownload = async (fileId, filename) => {
  try {
    const response = await downloadFile(fileId);
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', filename);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  } catch (error) {
    message.error('文件下载失败');
  }
};

const getTimelineColor = (item) => {
  if (item.activityType.endsWith('EndEvent')) {
    return item.decision === 'REJECTED' ? 'red' : 'green';
  }
  if (item.endTime) return 'blue'; // 已完成的节点
  return 'gray'; // 进行中或未开始的节点
};

const formatDuration = (ms) => {
  if (!ms) return 'N/A';
  let seconds = Math.floor(ms / 1000);
  let minutes = Math.floor(seconds / 60);
  let hours = Math.floor(minutes / 60);
  seconds %= 60;
  minutes %= 60;
  return `${hours}h ${minutes}m ${seconds}s`;
};
</script>

<style scoped>
.detail-layout {
  display: flex;
  gap: 24px;
  padding: 24px;
  align-items: flex-start; /* 顶部对齐 */
}
.main-content {
  flex: 2;
  min-width: 0;
}
.side-content {
  flex: 1;
  min-width: 0;
}

/* 审批意见的专属样式 */
.approval-comment {
  background-color: #fafafa;
  border: 1px solid #f0f0f0;
  padding: 8px 12px;
  border-radius: 4px;
  margin-top: 8px;
  font-size: 14px;
  color: #595959;
  word-wrap: break-word; /* 确保长文本能正常换行 */
}
.approval-comment strong {
  color: #262626;
  margin-right: 8px;
}

/* 只读富文本的样式，确保图片自适应 */
.readonly-richtext :deep(img) {
  max-width: 100%;
  height: auto;
}

/* 附件列表项的样式 */
.attachment-item {
  padding: 4px 0;
}
.attachment-item a {
  color: #1890ff;
  transition: color 0.3s;
}
.attachment-item a:hover {
  color: #40a9ff;
}
.attachment-item .anticon {
  margin-right: 8px;
}
</style>