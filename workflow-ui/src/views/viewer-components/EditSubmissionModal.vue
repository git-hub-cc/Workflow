<template>
  <a-modal
      :open="open"
      :title="`编辑: ${formDefinition.name || '记录'}`"
      @update:open="(val) => emit('update:open', val)"
      :width="isMobile ? '95%' : '800px'"
      :confirm-loading="saving"
      @ok="handleSave"
      destroyOnClose
  >
    <a-spin :spinning="loading" tip="正在加载数据...">
      <div style="max-height: 70vh; overflow-y: auto; padding: 24px;">
        <a-form
            v-if="!loading"
            :model="formData"
            layout="vertical"
            ref="formRef"
        >
          <FormItemRenderer
              v-for="field in formDefinition.schema.fields"
              :key="field.id"
              :field="field"
              :form-data="formData"
              :mode="'edit'"
              @update:form-data="updateFormData"
          />
        </a-form>
      </div>
    </a-spin>
  </a-modal>
</template>

<script setup>
import { ref, reactive, watch, defineAsyncComponent, onMounted, onBeforeUnmount } from 'vue';
import { message } from 'ant-design-vue';
import { getFormById, getSubmissionById, updateSubmission } from '@/api';
import { flattenFields } from '@/utils/formUtils.js';

const FormItemRenderer = defineAsyncComponent(() => import('@/views/viewer-components/FormItemRenderer.vue'));

const props = defineProps({
  open: Boolean,
  submissionId: [String, Number],
});
const emit = defineEmits(['update:open', 'refresh']);

const loading = ref(false);
const saving = ref(false);
const formRef = ref();
const formDefinition = ref({ schema: { fields: [] } });
const formData = reactive({});

// --- 【核心新增】响应式断点逻辑 ---
const isMobile = ref(window.innerWidth < 768);
const handleResize = () => { isMobile.value = window.innerWidth < 768; };
onMounted(() => { window.addEventListener('resize', handleResize); });
onBeforeUnmount(() => { window.removeEventListener('resize', handleResize); });
// --- 响应式逻辑结束 ---


watch(() => props.open, (newVal) => {
  if (newVal && props.submissionId) {
    loadData();
  }
});

const loadData = async () => {
  loading.value = true;
  try {
    const submission = await getSubmissionById(props.submissionId);
    const formDef = await getFormById(submission.formDefinitionId);

    formDef.schema = JSON.parse(formDef.schemaJson);
    formDefinition.value = formDef;

    Object.keys(formData).forEach(key => delete formData[key]);
    Object.assign(formData, JSON.parse(submission.dataJson));

    if (submission.attachments) {
      const fileUploadField = flattenFields(formDef.schema.fields).find(f => f.type === 'FileUpload');
      if (fileUploadField) {
        formData[fileUploadField.id] = submission.attachments;
      }
    }

  } catch (error) {
    message.error("加载编辑数据失败");
    emit('update:open', false);
  } finally {
    loading.value = false;
  }
};

const updateFormData = (fieldId, value) => {
  formData[fieldId] = value;
};

const handleSave = async () => {
  try {
    await formRef.value.validate();
    saving.value = true;

    const allFields = flattenFields(formDefinition.value.schema.fields);
    const attachmentFields = allFields.filter(f => f.type === 'FileUpload');
    const attachmentIds = attachmentFields.flatMap(f => formData[f.id]?.map(file => file.id) || []);

    const payload = {
      dataJson: JSON.stringify(formData),
      attachmentIds: attachmentIds,
    };

    await updateSubmission(props.submissionId, payload);
    message.success("更新成功！");
    emit('refresh');
    emit('update:open', false);

  } catch (errorInfo) {
    if (errorInfo && errorInfo.errorFields) {
      message.warn('请填写所有必填项');
    }
  } finally {
    saving.value = false;
  }
};
</script>