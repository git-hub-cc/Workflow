<template>
  <div class="page-container">
    <a-page-header :title="task.stepName" :sub-title="task.formName" @back="() => $router.push({ name: 'task-list' })" />

    <a-spin :spinning="loading" tip="正在加载任务详情...">
      <div v-if="!loading" class="detail-layout">
        <!-- 左侧主内容区: 表单 & 操作 -->
        <div class="main-content">
          <a-card :title="isRejectedTask ? '修改表单' : '表单详情'">
            <!-- 【核心修改】当为只读模式时，使用 a-descriptions 优化展示，并支持图片预览 -->
            <template v-if="mode === 'readonly'">
              <a-descriptions bordered :column="1">
                <template v-for="field in flattenedFields" :key="field.id">
                  <!-- 文件上传字段特殊渲染 -->
                  <a-descriptions-item v-if="field.type === 'FileUpload'" :label="field.label">
                    <div v-if="formData[field.id] && formData[field.id].length > 0">
                      <div v-for="file in formData[field.id]" :key="file.id" class="attachment-item">
                        <div v-if="isImage(file)" class="image-preview-container">
                          <a-image :width="64" :height="64" :src="`/api/files/${file.id}`" />
                          <div class="file-info">
                            <span class="filename" :title="file.originalFilename">{{ file.originalFilename }}</span>
                            <a @click.prevent="handleDownload(file.id, file.originalFilename)" href="#" class="download-action">下载</a>
                          </div>
                        </div>
                        <a v-else @click.prevent="handleDownload(file.id, file.originalFilename)" href="#" class="file-link">
                          <PaperClipOutlined /> {{ file.originalFilename }}
                        </a>
                      </div>
                    </div>
                    <span v-else>(无附件)</span>
                  </a-descriptions-item>
                  <!-- 其他字段的只读渲染 -->
                  <a-descriptions-item v-else :label="field.label">
                    <div v-if="field.type === 'RichText'" v-html="formData[field.id]" class="readonly-richtext"></div>
                    <span v-else>{{ getReadonlyDisplayValue(field, formData[field.id]) }}</span>
                  </a-descriptions-item>
                </template>
              </a-descriptions>
            </template>
            <!-- 编辑模式保持不变 -->
            <a-form
                v-else
                :model="formData"
                layout="vertical"
                ref="formRef"
            >
              <form-item-renderer
                  v-for="field in formSchema.fields"
                  :key="field.id"
                  :field="field"
                  :form-data="formData"
                  :mode="'edit'"
                  @update:form-data="updateFormData"
              />
            </a-form>
          </a-card>

          <a-card :title="isRejectedTask ? '提交操作' : '审批操作'" style="margin-top: 24px;">
            <div v-if="!isRejectedTask">
              <a-textarea v-model:value="comment" placeholder="请输入审批意见 (可选)" :rows="4" />
              <a-space style="margin-top: 16px; float: right;">
                <a-button type="primary" danger @click="handleApproval('REJECTED')" :loading="submitting">拒绝</a-button>
                <a-button type="primary" @click="handleApproval('APPROVED')" :loading="submitting">同意</a-button>
              </a-space>
            </div>
            <div v-else>
              <a-textarea v-model:value="comment" placeholder="请输入修改说明 (可选)" :rows="4" />
              <a-space style="margin-top: 16px; float: right;">
                <a-button type="primary" @click="handleResubmit" :loading="submitting">重新提交</a-button>
              </a-space>
            </div>
          </a-card>
        </div>

        <!-- 右侧辅助信息区: 流程历史 -->
        <div class="side-content">
          <a-card title="流程历史">
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
          </a-card>
        </div>
      </div>
    </a-spin>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, reactive } from 'vue';
import { getTaskById, getSubmissionById, getFormById, completeTask, getWorkflowHistory, downloadFile } from '@/api';
import { message } from 'ant-design-vue';
import { useRouter } from 'vue-router';
import FormItemRenderer from './viewer-components/FormItemRenderer.vue';
import { flattenFields } from '@/utils/formUtils.js';
// 【核心新增】引入图标
import { PaperClipOutlined } from '@ant-design/icons-vue';

const props = defineProps({ taskId: String });
const router = useRouter();

const loading = ref(true);
const submitting = ref(false);
const task = ref({});
const formSchema = ref({ fields: [] });
const formData = reactive({});
const comment = ref('');
const formRef = ref();
const history = ref([]);

const isRejectedTask = computed(() => {
  const taskName = task.value.stepName || '';
  return taskName.includes('修改') || taskName.includes('调整') || taskName.includes('重新');
});

// 【核心新增】根据 isRejectedTask 决定表单模式
const mode = computed(() => isRejectedTask.value ? 'edit' : 'readonly');

// 【核心新增】扁平化字段，用于 a-descriptions 渲染
const flattenedFields = computed(() => {
  if (!formSchema.value) return [];
  return flattenFields(formSchema.value.fields)
      .filter(f => !['GridRow', 'GridCol', 'Collapse', 'CollapsePanel', 'StaticText', 'DescriptionList'].includes(f.type));
});

const updateFormData = (fieldId, value) => {
  formData[fieldId] = value;
};


onMounted(async () => {
  try {
    const taskRes = await getTaskById(props.taskId);
    task.value = taskRes;

    const subRes = await getSubmissionById(taskRes.formSubmissionId);
    Object.assign(formData, JSON.parse(subRes.dataJson));

    const [formRes, historyRes] = await Promise.all([
      getFormById(subRes.formDefinitionId),
      getWorkflowHistory(taskRes.formSubmissionId)
    ]);

    formSchema.value = JSON.parse(formRes.schemaJson);

    // 【核心修改】确保附件数据能正确地填充到 formData 中，以便 a-descriptions 和 FormItemRenderer 都能使用
    if (subRes.attachments && subRes.attachments.length > 0) {
      const allFields = flattenFields(formSchema.value.fields);
      const fileUploadField = allFields.find(f => f.type === 'FileUpload');
      if (fileUploadField) {
        formData[fileUploadField.id] = subRes.attachments;
      }
    }

    history.value = historyRes;

  } catch (error) {
    // 错误由全局拦截器处理，这里无需显示消息
  } finally {
    loading.value = false;
  }
});

const handleApproval = async (decision) => {
  submitting.value = true;
  try {
    await completeTask(props.taskId, { decision, approvalComment: comment.value });
    message.success('任务处理成功！');
    router.push({ name: 'task-list' });
  } catch (error) {
    // 错误由全局拦截器处理
  } finally {
    submitting.value = false;
  }
};

const handleResubmit = async () => {
  try {
    await formRef.value.validate();
    submitting.value = true;

    const allFields = flattenFields(formSchema.value.fields);
    const attachmentFields = allFields.filter(f => f.type === 'FileUpload');
    const attachmentIds = attachmentFields.flatMap(f => formData[f.id]?.map(file => file.id) || []);

    await completeTask(props.taskId, {
      decision: 'APPROVED',
      approvalComment: comment.value,
      updatedFormData: JSON.stringify(formData),
      attachmentIds: attachmentIds,
    });

    message.success('申请已重新提交！');
    router.push({ name: 'task-list' });
  } catch(error) {
    if(error && error.errorFields) {
      message.warn('请填写所有必填项');
    }
  } finally {
    submitting.value = false;
  }
};

const getTimelineColor = (item) => {
  if (item.activityType.endsWith('EndEvent')) {
    return item.decision === 'REJECTED' ? 'red' : 'green';
  }
  if (item.endTime) return 'blue';
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

// --- 【核心新增】用于只读模式的辅助函数 ---
const isImage = (file) => {
  if (!file || !file.originalFilename) return false;
  const imageNameRegex = /\.(jpg|jpeg|png|gif|svg|webp)$/i;
  return imageNameRegex.test(file.originalFilename);
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

const getReadonlyDisplayValue = (field, value) => {
  if (value === null || value === undefined || value === '') return '(未填写)';
  // 【核心修改】增强 DatePicker 的显示逻辑
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
    try { return new Date(value).toLocaleString(); } catch(e) { return value; }
  }
  if (typeof value === 'boolean') return value ? '是' : '否';
  if (Array.isArray(value)) return value.join(', ');
  return value;
}
</script>

<style scoped>
.detail-layout {
  display: flex;
  gap: 24px;
  padding: 24px;
  align-items: flex-start;
}
.main-content {
  flex: 2;
  min-width: 0;
}
.side-content {
  flex: 1;
  min-width: 0;
}
.approval-comment {
  background-color: #fafafa;
  border: 1px solid #f0f0f0;
  padding: 8px 12px;
  border-radius: 4px;
  margin-top: 8px;
  font-size: 14px;
  color: #595959;
  word-wrap: break-word;
}
.approval-comment strong {
  color: #262626;
  margin-right: 8px;
}
.readonly-richtext :deep(img) {
  max-width: 100%;
  height: auto;
}
/* --- 【核心新增】附件列表样式 --- */
.attachment-item {
  padding: 8px 0;
}
.attachment-item:first-child {
  padding-top: 0;
}
.image-preview-container {
  display: flex;
  align-items: center;
  gap: 12px;
}
.image-preview-container :deep(.ant-image) {
  flex-shrink: 0;
}
.image-preview-container :deep(.ant-image-img) {
  object-fit: cover;
  border-radius: 4px;
}
.file-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}
.filename {
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.download-action {
  font-size: 12px;
}
.file-link {
  display: inline-flex;
  align-items: center;
}
.file-link .anticon {
  margin-right: 8px;
}
</style>