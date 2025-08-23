<template>
  <div class="page-container">
    <a-page-header :title="pageTitle" @back="() => $router.go(-1)">
      <template #extra>
        <a-space>
          <a-button @click="$router.go(-1)">取消</a-button>
          <!-- 【最终修复】在重新提交模式下隐藏“保存草稿”和“终止”按钮 -->
          <a-button v-if="!isResubmitMode" @click="handleSaveDraft" :loading="submitting">保存草稿</a-button>
          <a-button v-if="!isResubmitMode" type="primary" danger @click="handleAction('terminate')" :loading="submitting">终止</a-button>
          <!-- 【最终修复】根据模式显示不同文本的提交按钮 -->
          <a-button type="primary" @click="handleAction('proceed')" :loading="submitting">
            {{ isResubmitMode ? '重新提交' : '提交申请' }}
          </a-button>
        </a-space>
      </template>
    </a-page-header>

    <a-spin :spinning="loading" tip="正在加载表单...">
      <a-form
          v-if="!loading"
          :model="formData"
          layout="vertical"
          ref="formRef"
          style="max-width: 800px; margin: 24px auto;"
      >
        <form-item-renderer
            v-for="field in formDefinition.schema.fields"
            :key="field.id"
            :field="field"
            :form-data="formData"
            :mode="'edit'"
            @update:form-data="updateFormData"
        />
      </a-form>
    </a-spin>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
// --- 【最终修复】引入 completeTask API ---
import { getFormById, getSubmissionById, submitForm, createDraft, updateMyDraft, submitDraft, completeTask } from '@/api';
import { useUserStore } from '@/stores/user';
import { message, Modal } from 'ant-design-vue';
import FormItemRenderer from './viewer-components/FormItemRenderer.vue';
import { flattenFields, initFormData } from '@/utils/formUtils.js';

const props = defineProps({ formId: [String, Number] });
const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

const loading = ref(true);
const submitting = ref(false);
const formDefinition = ref({ schema: { fields: [] } });
const formData = reactive({});
const formRef = ref();

// --- 【最终修复】增加对路由 query 中 taskId 的响应 ---
const submissionIdFromQuery = computed(() => route.query.submissionId);
const taskIdFromQuery = computed(() => route.query.taskId);
const isDraftMode = computed(() => !!submissionIdFromQuery.value && !taskIdFromQuery.value);
const isResubmitMode = computed(() => !!taskIdFromQuery.value);
const pageTitle = computed(() => {
  if (isResubmitMode.value) return `修改申请: ${formDefinition.value.name}`;
  if (isDraftMode.value) return `编辑草稿: ${formDefinition.value.name}`;
  return formDefinition.value.name;
});

const initializeForm = async () => {
  loading.value = true;
  try {
    const formDef = await getFormById(props.formId);
    formDef.schema = JSON.parse(formDef.schemaJson);
    formDefinition.value = formDef;

    // 如果是编辑草稿或重新提交模式，需要加载已有数据
    if (isDraftMode.value || isResubmitMode.value) {
      const submission = await getSubmissionById(submissionIdFromQuery.value);
      Object.assign(formData, JSON.parse(submission.dataJson));
      if (submission.attachments) {
        const fileUploadField = flattenFields(formDef.schema.fields).find(f => f.type === 'FileUpload');
        if (fileUploadField) {
          formData[fileUploadField.id] = submission.attachments;
        }
      }
    } else {
      // 否则是新建模式，初始化空表单
      initFormData(formDef.schema.fields, formData);
    }
  } catch (error) {
    message.error('加载表单失败');
  } finally {
    loading.value = false;
  }
};

onMounted(initializeForm);

watch(() => props.formId, (newFormId, oldFormId) => {
  if (newFormId && newFormId !== oldFormId) {
    initializeForm();
  }
});

const updateFormData = (fieldId, value) => {
  formData[fieldId] = value;
};

const handleSaveDraft = async () => {
  submitting.value = true;
  try {
    const allFields = flattenFields(formDefinition.value.schema.fields);
    const attachmentFields = allFields.filter(f => f.type === 'FileUpload');
    const attachmentIds = attachmentFields.flatMap(f => formData[f.id]?.map(file => file.id) || []);

    const payload = {
      dataJson: JSON.stringify(formData),
      attachmentIds: attachmentIds,
    };

    if (isDraftMode.value) {
      await updateMyDraft(submissionIdFromQuery.value, payload);
      message.success('草稿更新成功！');
    } else {
      const response = await createDraft(props.formId, payload);
      message.success('草稿创建成功！');
      router.replace({ query: { submissionId: response.id } });
    }
  } catch (error) {
  } finally {
    submitting.value = false;
  }
};

// --- 【最终修复】增强 handleAction 以处理重新提交 ---
const handleAction = async (action) => {
  try {
    await formRef.value.validate();

    if (action === 'terminate') {
      await new Promise((resolve, reject) => {
        Modal.confirm({
          title: '确认终止申请？',
          content: '终止后，此申请将直接结束，无法继续流转。',
          okText: '确认终止',
          okType: 'danger',
          cancelText: '取消',
          onOk: resolve,
          onCancel: reject,
        });
      });
    }

    submitting.value = true;

    const allFields = flattenFields(formDefinition.value.schema.fields);
    const attachmentFields = allFields.filter(f => f.type === 'FileUpload');
    const attachmentIds = attachmentFields.flatMap(f => formData[f.id]?.map(file => file.id) || []);

    // 如果是重新提交模式
    if (isResubmitMode.value) {
      const payload = {
        decision: 'APPROVED', // 重新提交后，流程应继续，故决策为 'APPROVED'
        updatedFormData: JSON.stringify(formData),
        attachmentIds: attachmentIds,
      };
      await completeTask(taskIdFromQuery.value, payload);
      message.success('申请已重新提交！');
      router.push({ name: 'my-submissions' });
    } else {
      // 保持原有逻辑
      const payload = {
        dataJson: JSON.stringify(formData),
        attachmentIds: attachmentIds,
        initialAction: action,
      };

      if (isDraftMode.value) {
        await submitDraft(submissionIdFromQuery.value, payload);
      } else {
        await submitForm(props.formId, payload);
      }

      if (action === 'proceed') {
        message.success('申请提交成功！');
        router.push({ name: 'my-submissions' });
      } else if (action === 'terminate') {
        message.success('申请已终止。');
        router.push({ name: 'my-submissions' });
      }
    }
    await userStore.fetchPendingTasksCount();

  } catch (errorInfo) {
    if (!errorInfo) return;
    if (errorInfo && errorInfo.errorFields) {
      message.warn('请填写所有必填项');
    }
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
.page-container {
  background-color: #fff;
  border-radius: 4px;
}
</style>