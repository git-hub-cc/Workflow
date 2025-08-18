<template>
  <div class="page-container">
    <a-page-header :title="task.stepName" :sub-title="task.formName" @back="() => $router.push({ name: 'task-list' })" />

    <a-spin :spinning="loading" tip="正在加载任务详情...">
      <div v-if="!loading" class="detail-layout">
        <!-- 左侧主内容区: 表单 & 操作 -->
        <div class="main-content">
          <a-card :title="isRejectedTask ? '修改表单' : '表单详情'">
            <a-form
                :model="formData"
                layout="vertical"
                ref="formRef"
            >
              <form-item-renderer
                  v-for="field in formSchema.fields"
                  :key="field.id"
                  :field="field"
                  :form-data="formData"
                  :mode="isRejectedTask ? 'edit' : 'readonly'"
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
// ✨ 核心变更：新增 getWorkflowHistory API 的导入
import { getTaskById, getSubmissionById, getFormById, completeTask, getWorkflowHistory } from '@/api';
import { message } from 'ant-design-vue';
import { useRouter } from 'vue-router';
import FormItemRenderer from './viewer-components/FormItemRenderer.vue';
import { flattenFields, initFormData } from '@/utils/formUtils.js';

const props = defineProps({ taskId: String });
const router = useRouter();

const loading = ref(true);
const submitting = ref(false);
const task = ref({});
const formSchema = ref({ fields: [] });
const formData = reactive({});
const comment = ref('');
const formRef = ref();
// ✨ 核心变更：新增 state 用于存储流程历史
const history = ref([]);

const isRejectedTask = computed(() => {
  const taskName = task.value.stepName || '';
  return taskName.includes('修改') || taskName.includes('调整') || taskName.includes('重新');
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
    if (subRes.attachments) {
      formData.__attachments = subRes.attachments;
      // 初始化文件上传组件的数据
      const formDefForFile = await getFormById(subRes.formDefinitionId);
      const allFields = flattenFields(JSON.parse(formDefForFile.schemaJson).fields);
      allFields.forEach(field => {
        if (field.type === 'FileUpload') {
          formData[field.id] = subRes.attachments;
        }
      });
    }

    // ✨ 核心变更：并发获取表单定义和流程历史，提高加载效率
    const [formRes, historyRes] = await Promise.all([
      getFormById(subRes.formDefinitionId),
      getWorkflowHistory(taskRes.formSubmissionId)
    ]);

    formSchema.value = JSON.parse(formRes.schemaJson);
    history.value = historyRes;

  } catch (error) {
    message.error('加载任务详情失败');
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
    message.error('处理失败');
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
    } else {
      message.error('提交失败');
    }
  } finally {
    submitting.value = false;
  }
};

// ✨ 核心变更：新增用于渲染时间线的辅助函数
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
</script>

<!-- ✨ 核心变更：新增样式以匹配新布局 -->
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
</style>