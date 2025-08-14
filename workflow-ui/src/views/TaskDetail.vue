<template>
  <div class="page-container">
    <a-page-header :title="task.stepName" :sub-title="task.formName" @back="() => $router.push({ name: 'task-list' })" />

    <a-spin :spinning="loading" tip="正在加载任务详情...">
      <div v-if="!loading" style="padding: 24px;">
        <!-- 表单数据展示 -->
        <a-descriptions title="表单详情" bordered>
          <a-descriptions-item v-for="field in formSchema.fields" :key="field.id" :label="field.label">
            {{ formatValue(formData[field.id]) }}
          </a-descriptions-item>
        </a-descriptions>

        <!-- 审批操作 -->
        <a-card title="审批操作" style="margin-top: 24px;">
          <a-textarea v-model:value="comment" placeholder="请输入审批意见 (可选)" :rows="4" />
          <a-space style="margin-top: 16px; float: right;">
            <a-button type="primary" danger @click="handleApproval('REJECTED')" :loading="submitting">拒绝</a-button>
            <a-button type="primary" @click="handleApproval('APPROVED')" :loading="submitting">同意</a-button>
          </a-space>
        </a-card>
      </div>
    </a-spin>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { getTaskById, getSubmissionById, getFormById, completeTask } from '@/api';
import { message } from 'ant-design-vue';
import { useRouter } from 'vue-router';

const props = defineProps({ taskId: String });
const router = useRouter();

const loading = ref(true);
const submitting = ref(false);
const task = ref({});
const formSchema = ref({ fields: [] });
const formData = ref({});
const comment = ref('');

const formatValue = (value) => {
  if (typeof value === 'boolean') return value ? '是' : '否';
  if (value instanceof Date) return value.toLocaleString();
  return value || '(未填写)';
};

onMounted(async () => {
  try {
    const taskRes = await getTaskById(props.taskId);
    task.value = taskRes;

    const subRes = await getSubmissionById(taskRes.formSubmissionId);
    formData.value = JSON.parse(subRes.dataJson);

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
    await completeTask(props.taskId, { decision, comment: comment.value });
    message.success('任务处理成功！');
    router.push({ name: 'task-list' });
  } catch (error) {
    message.error('处理失败');
  } finally {
    submitting.value = false;
  }
};
</script>