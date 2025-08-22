<template>
  <div class="page-container">
    <a-page-header :title="formDefinition.name" @back="() => $router.go(-1)">
      <template #extra>
        <a-button @click="$router.go(-1)">取消</a-button>
        <a-button type="primary" @click="handleSubmit" :loading="submitting">提交申请</a-button>
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
// --- 【核心修复1】从 'vue' 导入 watch ---
import { ref, reactive, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { getFormById, submitForm } from '@/api';
import { message } from 'ant-design-vue';
import FormItemRenderer from './viewer-components/FormItemRenderer.vue';
import { flattenFields, initFormData } from '@/utils/formUtils.js';


const props = defineProps({ formId: [String, Number] });
const router = useRouter();

const loading = ref(true);
const submitting = ref(false);
const formDefinition = ref({ schema: { fields: [] } });
const formData = reactive({});
const formRef = ref();

// --- 【核心修复2】将数据加载逻辑封装成一个可复用的函数 ---
const initializeForm = async () => {
  if (!props.formId) return;
  loading.value = true;
  try {
    const res = await getFormById(props.formId);
    res.schema = JSON.parse(res.schemaJson);
    // 重新初始化 formData 以清空旧表单的数据
    initFormData(res.schema.fields, formData);
    formDefinition.value = res;
  } catch (error) {
    message.error('加载表单失败');
  } finally {
    loading.value = false;
  }
};

// --- 【核心修复3】onMounted 只负责首次加载 ---
onMounted(initializeForm);

// --- 【核心修复4】使用 watch 侦听 props.formId 的变化，以处理组件复用时的内容更新 ---
watch(() => props.formId, (newFormId, oldFormId) => {
  // 确保 formId 确实发生了变化，再重新加载数据
  if (newFormId && newFormId !== oldFormId) {
    initializeForm();
  }
});


const updateFormData = (fieldId, value) => {
  formData[fieldId] = value;
};

const handleSubmit = async () => {
  try {
    await formRef.value.validate();
    submitting.value = true;

    // 从 formData 中提取附件ID
    const allFields = flattenFields(formDefinition.value.schema.fields);
    const attachmentFields = allFields.filter(f => f.type === 'FileUpload');
    const attachmentIds = attachmentFields.flatMap(f => formData[f.id]?.map(file => file.id) || []);

    const payload = {
      dataJson: JSON.stringify(formData),
      attachmentIds: attachmentIds,
    };
    await submitForm(props.formId, payload);
    message.success('申请提交成功！');
    router.push({name: 'home'});
  } catch (errorInfo) {
    if (errorInfo && errorInfo.errorFields) {
      message.warn('请填写所有必填项');
    } else {
      message.error('提交失败');
    }
  } finally {
    submitting.value = false;
  }
};
</script>