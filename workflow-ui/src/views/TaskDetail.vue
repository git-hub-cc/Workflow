<template>
  <div class="page-container">
    <a-page-header :title="task.stepName" :sub-title="task.formName" @back="() => $router.push({ name: 'task-list' })" />

    <a-spin :spinning="loading" tip="正在加载任务详情...">
      <div v-if="!loading" class="detail-layout">
        <!-- 左侧主内容区: 表单 & 操作 -->
        <div class="main-content">
          <a-card :title="isEditMode ? '修改表单' : '表单详情'">
            <template v-if="mode === 'readonly'">
              <a-descriptions bordered :column="1">
                <template v-for="field in flattenedFields" :key="field.id">
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
                  <a-descriptions-item v-else :label="field.label">
                    <div v-if="field.type === 'RichText'" v-html="formData[field.id]" class="readonly-richtext"></div>
                    <span v-else>{{ getReadonlyDisplayValue(field, formData[field.id]) }}</span>
                  </a-descriptions-item>
                </template>
              </a-descriptions>
            </template>
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

          <!-- 【核心修改】将审批操作区改为动态按钮组 -->
          <a-card :title="isEditMode ? '提交操作' : '审批操作'" style="margin-top: 24px;">
            <a-form layout="vertical">
              <a-form-item :label="isEditMode ? '修改说明 (可选)' : '审批意见 (可选)'" name="comment">
                <a-textarea v-model:value="comment" :rows="4" />
              </a-form-item>
              <a-form-item>
                <a-space style="float: right;">
                  <!-- 重新提交按钮 (仅在编辑模式下显示) -->
                  <a-button v-if="isEditMode" type="primary" @click="handleAction('APPROVED')" :loading="submitting">
                    重新提交
                  </a-button>

                  <!-- 审批按钮组 (仅在审批模式下显示) -->
                  <template v-else>
                    <a-button
                        v-if="can('REJECTED')"
                        type="primary" danger
                        @click="handleAction('REJECTED')"
                        :loading="submitting"
                    >
                      拒绝
                    </a-button>

                    <!-- 打回按钮 -->
                    <a-dropdown v-if="can('RETURN_TO_INITIATOR') || can('RETURN_TO_PREVIOUS')">
                      <template #overlay>
                        <a-menu @click="({ key }) => handleAction(key)">
                          <a-menu-item key="RETURN_TO_INITIATOR" v-if="can('RETURN_TO_INITIATOR')">
                            <RollbackOutlined /> 打回至发起人
                          </a-menu-item>
                          <a-menu-item key="RETURN_TO_PREVIOUS" v-if="can('RETURN_TO_PREVIOUS')">
                            <UndoOutlined /> 打回至上一节点
                          </a-menu-item>
                        </a-menu>
                      </template>
                      <a-button :loading="submitting">
                        打回 <DownOutlined />
                      </a-button>
                    </a-dropdown>

                    <a-button
                        v-if="can('APPROVED')"
                        type="primary"
                        @click="handleAction('APPROVED')"
                        :loading="submitting"
                    >
                      同意
                    </a-button>
                  </template>
                </a-space>
              </a-form-item>
            </a-form>
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
import { ref, onMounted, computed, reactive } from 'vue';
import { getTaskById, getSubmissionById, getFormById, completeTask, getWorkflowHistory, downloadFile, getWorkflowDiagram } from '@/api';
import { message } from 'ant-design-vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import FormItemRenderer from './viewer-components/FormItemRenderer.vue';
import { flattenFields } from '@/utils/formUtils.js';
// --- 【核心新增】引入新图标和模态框 ---
import { PaperClipOutlined, DownOutlined, RollbackOutlined, UndoOutlined, FullscreenOutlined } from '@ant-design/icons-vue';
import ProcessDiagramViewer from '@/components/ProcessDiagramViewer.vue';
import ProcessDiagramModal from '@/components/ProcessDiagramModal.vue';

const props = defineProps({ taskId: String });
const router = useRouter();
const userStore = useUserStore();

const loading = ref(true);
const submitting = ref(false);
const task = ref({});
const formSchema = ref({ fields: [] });
const formData = reactive({});
const comment = ref('');
const formRef = ref();
const history = ref([]);
const bpmnXml = ref(null);
const activeTabKey = ref('historyList');
// --- 【核心新增】模态框状态 ---
const isDiagramModalVisible = ref(false);

const isEditMode = computed(() => {
  const taskName = task.value.stepName || '';
  return taskName.includes('修改') || taskName.includes('调整') || taskName.includes('重新');
});

const mode = computed(() => isEditMode.value ? 'edit' : 'readonly');

const flattenedFields = computed(() => {
  if (!formSchema.value) return [];
  return flattenFields(formSchema.value.fields)
      .filter(f => !['GridRow', 'GridCol', 'Collapse', 'CollapsePanel', 'StaticText', 'DescriptionList'].includes(f.type));
});

const can = (decision) => {
  return task.value.availableDecisions?.includes(decision);
};

const updateFormData = (fieldId, value) => {
  formData[fieldId] = value;
};


onMounted(async () => {
  try {
    const taskRes = await getTaskById(props.taskId);
    task.value = taskRes;

    const [subRes, historyRes, diagramRes] = await Promise.all([
      getSubmissionById(taskRes.formSubmissionId),
      getWorkflowHistory(taskRes.formSubmissionId),
      getWorkflowDiagram(taskRes.formSubmissionId)
    ]);

    Object.assign(formData, JSON.parse(subRes.dataJson));
    history.value = historyRes;
    bpmnXml.value = diagramRes.bpmnXml;

    const formRes = await getFormById(subRes.formDefinitionId);
    formSchema.value = JSON.parse(formRes.schemaJson);

    if (subRes.attachments && subRes.attachments.length > 0) {
      const allFields = flattenFields(formSchema.value.fields);
      const fileUploadField = allFields.find(f => f.type === 'FileUpload');
      if (fileUploadField) {
        formData[fileUploadField.id] = subRes.attachments;
      }
    }

  } catch (error) {
    // 错误由全局拦截器处理
  } finally {
    loading.value = false;
  }
});

const handleAction = async (decision) => {
  if (decision.startsWith('RETURN_') && !comment.value.trim()) {
    message.warn('打回操作必须填写意见！');
    return;
  }

  if (isEditMode.value) {
    try {
      await formRef.value.validate();
    } catch(errorInfo) {
      if(errorInfo && errorInfo.errorFields) {
        message.warn('请填写所有必填项');
      }
      return;
    }
  }

  submitting.value = true;
  try {
    const payload = {
      decision: decision,
      approvalComment: comment.value,
    };

    if (isEditMode.value) {
      const allFields = flattenFields(formSchema.value.fields);
      const attachmentFields = allFields.filter(f => f.type === 'FileUpload');
      const attachmentIds = attachmentFields.flatMap(f => formData[f.id]?.map(file => file.id) || []);

      payload.updatedFormData = JSON.stringify(formData);
      payload.attachmentIds = attachmentIds;
    }

    await completeTask(props.taskId, payload);
    await userStore.fetchPendingTasksCount();
    message.success('任务处理成功！');
    router.push({ name: 'task-list' });
  } catch (error) {
    // 错误由全局拦截器处理
  } finally {
    submitting.value = false;
  }
};


// --- 其他辅助函数 (无变化) ---

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