<template>
  <div class="page-container">
    <a-page-header :title="task.stepName" :sub-title="task.formName" @back="() => $router.push({ name: 'task-list' })" />

    <a-spin :spinning="loading" tip="正在加载任务详情...">
      <div v-if="!loading" style="padding: 24px;">
        <a-card :title="isRejectedTask ? '修改表单' : '表单详情'">
          <a-form
              :model="formData"
              layout="vertical"
              ref="formRef"
              style="max-width: 800px; margin: 0 auto;"
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
    </a-spin>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, reactive } from 'vue';
import { getTaskById, getSubmissionById, getFormById, completeTask } from '@/api';
import { message } from 'ant-design-vue';
import { useRouter } from 'vue-router';
import FormItemRenderer from './viewer-components/FormItemRenderer.vue';
// --- 【路径已修改】 ---
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
      const allFields = flattenFields(JSON.parse((await getFormById(subRes.formDefinitionId)).schemaJson).fields);
      allFields.forEach(field => {
        if (field.type === 'FileUpload') {
          formData[field.id] = subRes.attachments;
        }
      });
    }

    const formRes = await getFormById(subRes.formDefinitionId);
    formSchema.value = JSON.parse(formRes.schemaJson);

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
</script>