<template>
  <div class="page-container">
    <a-page-header :title="task.stepName" :sub-title="task.formName" @back="() => $router.push({ name: 'task-list' })" />

    <a-spin :spinning="loading" tip="正在加载任务详情...">
      <div v-if="!loading" style="padding: 24px;">
        <!-- 表单区域 -->
        <a-card :title="isRejectedTask ? '修改表单' : '表单详情'">
          <a-form
              :model="formData"
              layout="vertical"
              ref="formRef"
              style="max-width: 800px; margin: 0 auto;"
          >
            <a-form-item
                v-for="field in formSchema.fields"
                :key="field.id"
                :label="field.label"
                :name="field.id"
                :rules="isRejectedTask ? field.rules : []"
            >
              <!-- 审批模式：只读展示 -->
              <span v-if="!isRejectedTask">{{ formatValue(formData[field.id]) }}</span>
              <!-- 修改模式：可编辑表单 -->
              <component
                  v-else
                  :is="getComponentByType(field.type)"
                  v-model:value="formData[field.id]"
                  :placeholder="field.props.placeholder"
                  :options="field.type === 'Select' || field.type === 'UserPicker' ? getOptionsForField(field) : undefined"
              />
            </a-form-item>
          </a-form>
        </a-card>

        <!-- 操作区域 -->
        <a-card :title="isRejectedTask ? '提交操作' : '审批操作'" style="margin-top: 24px;">
          <!-- 审批模式 -->
          <div v-if="!isRejectedTask">
            <a-textarea v-model:value="comment" placeholder="请输入审批意见 (可选)" :rows="4" />
            <a-space style="margin-top: 16px; float: right;">
              <a-button type="primary" danger @click="handleApproval('REJECTED')" :loading="submitting">拒绝</a-button>
              <a-button type="primary" @click="handleApproval('APPROVED')" :loading="submitting">同意</a-button>
            </a-space>
          </div>
          <!-- 修改模式 -->
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
import { ref, onMounted, computed } from 'vue';
import { getTaskById, getSubmissionById, getFormById, completeTask, submitForm } from '@/api';
import { useUserStore } from '@/stores/user';
import { message } from 'ant-design-vue';
import { useRouter } from 'vue-router';
import {
  Input as AInput,
  Textarea as ATextarea,
  Select as ASelect,
  Checkbox as ACheckbox,
  DatePicker as ADatePicker,
} from 'ant-design-vue';

const props = defineProps({ taskId: String });
const router = useRouter();
const userStore = useUserStore();

const loading = ref(true);
const submitting = ref(false);
const task = ref({});
const formSchema = ref({ fields: [] });
const formData = ref({});
const comment = ref('');
const formRef = ref();

// 判断当前任务是否为被驳回的任务
const isRejectedTask = computed(() => {
  const taskName = task.value.stepName || '';
  return taskName.includes('修改') || taskName.includes('调整');
});

const getComponentByType = (type) => ({
  Input: AInput,
  Textarea: ATextarea,
  Select: ASelect,
  Checkbox: ACheckbox,
  DatePicker: ADatePicker,
  UserPicker: ASelect, // 用户选择器也用 Select 组件
}[type] || AInput);

const getOptionsForField = (field) => {
  if (field.type === 'Select') {
    return field.props.options.map(o => ({ label: o, value: o }));
  }
  if (field.type === 'UserPicker') {
    return userStore.allUsers.map(u => ({ label: `${u.name} (${u.id})`, value: u.id }));
  }
  return undefined;
};

const formatValue = (value) => {
  if (value === null || value === undefined) return '(未填写)';

  // 检查是否为日期格式
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/.test(value)) {
    return new Date(value).toLocaleString();
  }

  // 检查字段是否为用户选择器
  const field = formSchema.value.fields.find(f => formData.value[f.id] === value && f.type === 'UserPicker');
  if (field) {
    const user = userStore.allUsers.find(u => u.id === value);
    return user ? `${user.name} (${user.id})` : value;
  }

  if (typeof value === 'boolean') return value ? '是' : '否';
  return value;
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

    // 重新提交本质上也是 completeTask，但决策是 'APPROVED'
    // 后端BPMN流程需要配置，当发起人提交后，自动流转回审批节点
    await completeTask(props.taskId, {
      decision: 'APPROVED',
      approvalComment: comment.value,
      // 将更新后的表单数据作为流程变量传递
      updatedFormData: JSON.stringify(formData.value)
    });

    message.success('申请已重新提交！');
    router.push({ name: 'task-list' });
  } catch(error) {
    if(error.errorFields) {
      message.warn('请填写所有必填项');
    } else {
      message.error('提交失败');
    }
  } finally {
    submitting.value = false;
  }
};

</script>