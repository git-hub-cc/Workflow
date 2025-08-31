<template>
  <div class="page-container">
    <a-page-header
        :title="formDefinition.name || '申请详情'"
        :sub-title="submission.submitterName ? `提交人: ${submission.submitterName}` : ''"
        @back="() => $router.go(-1)"
    />

    <a-spin :spinning="loading" tip="正在加载详情...">
      <!-- 【核心修改】当加载失败时，显示错误提示 -->
      <a-empty
          v-if="loadError"
          :description="loadError"
          style="padding-top: 100px;"
      >
        <a-button type="primary" @click="$router.push('/')">返回首页</a-button>
      </a-empty>

      <!-- 仅在加载成功后显示内容 -->
      <div v-else-if="!loading" class="detail-layout">
        <!-- 左侧主内容区: 表单详情 -->
        <div class="main-content">
          <a-card title="表单内容">
            <a-descriptions bordered :column="1">
              <template v-for="field in flattenedFields" :key="field.id">
                <!-- 1. 富文本 -->
                <a-descriptions-item v-if="field.type === 'RichText'" :label="field.label" :span="1">
                  <div v-html="formData[field.id]" class="readonly-richtext"></div>
                </a-descriptions-item>

                <!-- 2. 子表单 -->
                <a-descriptions-item v-else-if="field.type === 'Subform'" :label="field.label" :span="1">
                  <a-table :columns="getSubformColumns(field)" :data-source="formData[field.id]" :pagination="false" bordered size="small" />
                </a-descriptions-item>

                <!-- 3. 键值对 -->
                <a-descriptions-item v-else-if="field.type === 'KeyValue'" :label="field.label" :span="1">
                  <a-descriptions :column="1" size="small" bordered>
                    <a-descriptions-item v-for="(item, idx) in formData[field.id]" :key="idx" :label="item.key">{{ item.value }}</a-descriptions-item>
                  </a-descriptions>
                </a-descriptions-item>

                <!-- 4. 图标选择器 -->
                <a-descriptions-item v-else-if="field.type === 'IconPicker'" :label="field.label">
                  <component v-if="formData[field.id]" :is="iconMap[formData[field.id]]" style="font-size: 24px;" />
                  <span v-else>(未选择)</span>
                </a-descriptions-item>

                <!-- 5. 【核心优化】文件上传字段：直接渲染附件列表 -->
                <a-descriptions-item v-else-if="field.type === 'FileUpload'" :label="field.label">
                  <div v-if="formData[field.id] && formData[field.id].length > 0" class="attachments-list">
                    <div v-for="file in formData[field.id]" :key="file.id" class="attachment-item">
                      <!-- 图片预览 -->
                      <div v-if="isImage(file)" class="image-preview-container">
                        <a-image :width="80" :height="80" :src="`/api/files/${file.id}`" />
                        <div class="file-info">
                          <span class="filename" :title="file.originalFilename">{{ file.originalFilename }}</span>
                          <a @click.prevent="handleDownload(file.id, file.originalFilename)" href="#" class="download-action">下载</a>
                        </div>
                      </div>
                      <!-- 非图片文件卡片 -->
                      <a v-else @click.prevent="handleDownload(file.id, file.originalFilename)" href="#" class="file-link">
                        <PaperClipOutlined />
                        <span :title="file.originalFilename">{{ file.originalFilename }}</span>
                      </a>
                    </div>
                  </div>
                  <span v-else>(无附件)</span>
                </a-descriptions-item>

                <!-- 6. 其他普通字段 -->
                <a-descriptions-item v-else :label="field.label">
                  {{ formatDisplayValue(field, formData[field.id]) }}
                </a-descriptions-item>
              </template>
            </a-descriptions>
          </a-card>
        </div>

        <!-- 右侧辅助信息区: 流程历史 -->
        <div class="side-content">
          <a-card>
            <a-tabs v-model:activeKey="activeTabKey">
              <a-tab-pane key="historyList" tab="历史列表">
                <a-timeline>
                  <a-timeline-item v-for="item in history" :key="item.activityId" :color="getTimelineColor(item)">
                    <h4>{{ item.activityName }}</h4>
                    <p v-if="item.assigneeName">处理人: {{ item.assigneeName }}</p>
                    <p>开始: {{ item.startTime ? new Date(item.startTime).toLocaleString() : 'N/A' }}</p>
                    <p v-if="item.endTime">结束: {{ new Date(item.endTime).toLocaleString() }}</p>
                    <p v-if="item.durationInMillis">耗时: {{ formatDuration(item.durationInMillis) }}</p>
                    <p v-if="item.comment" class="approval-comment">
                      <strong>意见:</strong> {{ item.comment }}
                    </p>
                  </a-timeline-item>
                </a-timeline>
              </a-tab-pane>
              <a-tab-pane key="diagram" tab="流程图">
                <!-- 【核心修改】增加包裹容器和放大按钮 -->
                <div class="diagram-preview-wrapper">
                  <ProcessDiagramViewer
                      v-if="bpmnXml"
                      :bpmn-xml="bpmnXml"
                      :history-activities="history"
                  />
                  <a-empty v-else description="暂无流程图信息" />
                  <a-button
                      class="fullscreen-btn"
                      type="primary"
                      shape="circle"
                      size="large"
                      @click="isDiagramModalVisible = true"
                      title="全屏查看"
                  >
                    <template #icon><FullscreenOutlined /></template>
                  </a-button>
                </div>
              </a-tab-pane>
            </a-tabs>
          </a-card>
        </div>
      </div>
    </a-spin>

    <!-- 【核心新增】流程图模态框 -->
    <ProcessDiagramModal
        v-if="isDiagramModalVisible"
        v-model:open="isDiagramModalVisible"
        :bpmn-xml="bpmnXml"
        :history-activities="history"
    />
  </div>
</template>

<script setup>
// 【核心修改】引入 defineAsyncComponent
import { ref, reactive, onMounted, computed, defineAsyncComponent } from 'vue';
// 【核心修改】引入 getWorkflowDiagram 和 FullscreenOutlined
import { getSubmissionById, getFormById, getWorkflowHistory, downloadFile, getWorkflowDiagram } from '@/api';
import { message } from 'ant-design-vue';
import { PaperClipOutlined, FullscreenOutlined } from '@ant-design/icons-vue';
import { flattenFields } from '@/utils/formUtils.js';
import { iconMap } from '@/utils/iconLibrary.js';
import { useSystemStore } from '@/stores/system';
// 【核心修改】引入新的流程图组件和模态框
import ProcessDiagramViewer from '@/components/ProcessDiagramViewer.vue';
// 【核心修改】使用 defineAsyncComponent 动态加载流程图模态框
const ProcessDiagramModal = defineAsyncComponent(() => import('@/components/ProcessDiagramModal.vue'));


const props = defineProps({ submissionId: String });
const systemStore = useSystemStore();

const loading = ref(true);
const loadError = ref(null); // 【核心新增】错误状态
const submission = ref({});
const formDefinition = ref({ schema: { fields: [] } });
const formData = reactive({});
const history = ref([]);
const bpmnXml = ref(null);
const activeTabKey = ref('historyList');
// --- 【核心新增】模态框状态 ---
const isDiagramModalVisible = ref(false);


const flattenedFields = computed(() => {
  if (!formDefinition.value.schema) return [];
  return flattenFields(formDefinition.value.schema.fields)
      .filter(f => !['GridRow', 'GridCol', 'Collapse', 'CollapsePanel', 'StaticText', 'DescriptionList'].includes(f.type));
});

onMounted(async () => {
  try {
    const [subRes, historyRes, diagramRes] = await Promise.all([
      getSubmissionById(props.submissionId),
      getWorkflowHistory(props.submissionId),
      getWorkflowDiagram(props.submissionId)
    ]);

    submission.value = subRes;
    Object.assign(formData, JSON.parse(subRes.dataJson));
    history.value = historyRes;
    bpmnXml.value = diagramRes.bpmnXml;

    const formRes = await getFormById(subRes.formDefinitionId);
    formRes.schema = JSON.parse(formRes.schemaJson);
    formDefinition.value = formRes;

    if (subRes.attachments && subRes.attachments.length > 0) {
      const allFields = flattenFields(formRes.schema.fields);
      const fileUploadField = allFields.find(f => f.type === 'FileUpload');
      if (fileUploadField) {
        formData[fileUploadField.id] = subRes.attachments;
      }
    }

  } catch (error) {
    // 【核心修改】捕获错误并设置错误状态
    loadError.value = '您访问的申请不存在或已被删除。';
    console.error('加载详情失败:', error);
  } finally {
    loading.value = false;
  }
});


// --- 用于显示和格式化的辅助函数 (无变化) ---

const isImage = (file) => {
  if (!file || !file.originalFilename) return false;
  const imageNameRegex = /\.(jpg|jpeg|png|gif|svg|webp)$/i;
  return imageNameRegex.test(file.originalFilename);
};

const getSubformColumns = (field) => field.props.columns.map(col => ({ title: col.label, dataIndex: col.id, key: col.id }));

const formatDisplayValue = (field, value) => {
  if (value === null || value === undefined || value === '') return '(未填写)';
  if (field.type === 'DatePicker' && value) {
    if (Array.isArray(value)) {
      if (value.length === 2 && field.props.pickerMode === 'range') {
        try {
          const start = new Date(value[0]).toLocaleDateString();
          const end = new Date(value[1]).toLocaleDateString();
          return `${start} 至 ${end}`;
        } catch (e) { return value.join(' 至 '); }
      }
      try {
        return value.map(d => new Date(d).toLocaleDateString()).join(', ');
      } catch (e) { return value.join(', '); }
    }
    try { return new Date(value).toLocaleString(); } catch (e) { return value; }
  }
  if (typeof value === 'boolean') return value ? '是' : '否';
  if (Array.isArray(value)) return value.join(', ');

  if (field.dataSource && field.dataSource.options) {
    const findLabel = (options, val) => {
      for (const opt of options) {
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
  if (item.endTime) return systemStore.settings.THEME_COLOR || 'blue';
  return 'gray';
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
.detail-layout { display: flex; gap: 24px; padding: 24px; align-items: flex-start; }
.main-content { flex: 2; min-width: 0; }
.side-content { flex: 1; min-width: 0; }
.approval-comment { background-color: #fafafa; border: 1px solid #f0f0f0; padding: 8px 12px; border-radius: 4px; margin-top: 8px; font-size: 14px; color: #595959; word-wrap: break-word; }
.approval-comment strong { color: #262626; margin-right: 8px; }
.readonly-richtext :deep(img) { max-width: 100%; height: auto; }

/* --- 【核心优化】附件列表样式 --- */
.attachments-list { display: flex; flex-wrap: wrap; gap: 16px; }
.image-preview-container { display: flex; flex-direction: column; align-items: center; gap: 8px; width: 100px; text-align: center; }
.image-preview-container :deep(.ant-image) { flex-shrink: 0; }
.image-preview-container :deep(.ant-image-img) { object-fit: cover; border-radius: 4px; border: 1px solid #f0f0f0; }
.file-info { display: flex; flex-direction: column; justify-content: center; min-width: 0; width: 100%; }
.filename { font-size: 12px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.download-action { font-size: 12px; color: var(--ant-primary-color); }
.download-action:hover { color: var(--ant-primary-color-hover); }

.file-link { color: rgba(0, 0, 0, 0.88); transition: all 0.2s; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; width: 100px; height: 100px; padding: 8px; border: 1px solid #f0f0f0; border-radius: 4px; text-align: center; }
.file-link:hover { color: var(--ant-primary-color); border-color: var(--ant-primary-color); box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09); }
.file-link .anticon { font-size: 28px; }
.file-link span { font-size: 12px; word-break: break-all; line-height: 1.2; }

/* --- 【核心新增】流程图预览容器样式 --- */
.diagram-preview-wrapper {
  position: relative;
}
.diagram-preview-wrapper :deep(.diagram-container) {
  height: 400px; /* 限制预览区域的高度 */
}
.fullscreen-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}
</style>