<template>
  <div class="page-container">
    <a-page-header :title="pageTitle" @back="() => $router.go(-1)">
      <!-- 【核心修改】根据页面模式动态渲染按钮 -->
      <template #extra>
        <a-space>
          <!-- 场景一: 新建模式 -->
          <template v-if="isNewMode">
            <a-button @click="handleSaveDraft" :loading="submitting">保存草稿</a-button>
            <a-button type="primary" @click="handleAction('proceed')" :loading="submitting">提交申请</a-button>
          </template>

          <!-- 场景二: 编辑草稿模式 -->
          <template v-else-if="isDraftMode">
            <a-button @click="handleSaveDraft" :loading="submitting">保存草稿</a-button>
            <a-button type="primary" @click="handleAction('proceed')" :loading="submitting">提交申请</a-button>
            <a-button type="primary" danger @click="handleDeleteDraft" :loading="submitting">删除草稿</a-button>
          </template>

          <!-- 场景三: 修改并重新提交模式 -->
          <template v-else-if="isResubmitMode">
            <a-button type="primary" @click="handleAction('resubmit')" :loading="submitting">重新提交</a-button>
          </template>

          <a-button @click="$router.go(-1)">取消</a-button>
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
import { ref, reactive, onMounted, watch, computed, defineAsyncComponent } from 'vue';
import { useRouter, useRoute } from 'vue-router';
// --- 【核心修改】引入 deleteMyDraft API ---
import { getFormById, getSubmissionById, submitForm, createDraft, updateMyDraft, submitDraft, completeTask, deleteMyDraft } from '@/api';
import { useUserStore } from '@/stores/user';
import { message, Modal } from 'ant-design-vue';
import { flattenFields, initFormData } from '@/utils/formUtils.js';

const FormItemRenderer = defineAsyncComponent(() => import('./viewer-components/FormItemRenderer.vue'));

const props = defineProps({ formId: [String, Number] });
const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

const loading = ref(true);
const submitting = ref(false);
const formDefinition = ref({ schema: { fields: [] } });
const formData = reactive({});
const formRef = ref();

// --- 【核心修改】增加模式判断计算属性 ---
const submissionIdFromQuery = computed(() => route.query.submissionId);
const taskIdFromQuery = computed(() => route.query.taskId);

const isNewMode = computed(() => !submissionIdFromQuery.value && !taskIdFromQuery.value);
const isDraftMode = computed(() => !!submissionIdFromQuery.value && !taskIdFromQuery.value);
const isResubmitMode = computed(() => !!taskIdFromQuery.value && !!submissionIdFromQuery.value); // 重新提交必须同时有 taskId 和 submissionId

const pageTitle = computed(() => {
  const baseName = formDefinition.value.name || '...';
  if (isResubmitMode.value) return `修改申请: ${baseName}`;
  if (isDraftMode.value) return `编辑草稿: ${baseName}`;
  return `新建申请: ${baseName}`;
});
// --- 【修改结束】 ---

const initializeForm = async () => {
  loading.value = true;
  try {
    const formDef = await getFormById(props.formId);
    formDef.schema = JSON.parse(formDef.schemaJson);
    formDefinition.value = formDef;

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
      // 创建成功后，跳转到编辑草稿模式，防止用户重复创建
      router.replace({ query: { submissionId: response.id } });
    }
  } catch (error) {
    // 错误已由全局拦截器处理
  } finally {
    submitting.value = false;
  }
};

// --- 【核心新增】删除草稿的处理逻辑 ---
const handleDeleteDraft = () => {
  Modal.confirm({
    title: '确认删除草稿？',
    content: '此操作将永久删除当前草稿，且无法恢复。',
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      submitting.value = true;
      try {
        await deleteMyDraft(submissionIdFromQuery.value);
        message.success('草稿已删除！');
        router.push({ name: 'my-submissions' });
      } catch (error) {
        // 错误已由全局拦截器处理
      } finally {
        submitting.value = false;
      }
    },
  });
};
// --- 【新增结束】 ---

const handleAction = async (action) => {
  try {
    await formRef.value.validate();
    submitting.value = true;

    const allFields = flattenFields(formDefinition.value.schema.fields);
    const attachmentFields = allFields.filter(f => f.type === 'FileUpload');
    const attachmentIds = attachmentFields.flatMap(f => formData[f.id]?.map(file => file.id) || []);

    // 重新提交模式
    if (action === 'resubmit') {
      const payload = {
        decision: 'APPROVED',
        updatedFormData: JSON.stringify(formData),
        attachmentIds: attachmentIds,
      };
      await completeTask(taskIdFromQuery.value, payload);
      message.success('申请已重新提交！');
      router.push({ name: 'my-submissions' });
    }
    // 提交模式 (包括新建提交和草稿提交)
    else if (action === 'proceed') {
      const payload = {
        dataJson: JSON.stringify(formData),
        attachmentIds: attachmentIds,
        initialAction: 'proceed',
      };

      if (isDraftMode.value) {
        await submitDraft(submissionIdFromQuery.value, payload);
      } else {
        await submitForm(props.formId, payload);
      }
      message.success('申请提交成功！');
      router.push({ name: 'my-submissions' });
    }

    await userStore.fetchPendingTasksCount();

  } catch (errorInfo) {
    if (errorInfo && errorInfo.errorFields) {
      message.warn('请填写所有必填项');
    }
    // API错误已由全局拦截器处理
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