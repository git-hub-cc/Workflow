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
        <a-form-item
            v-for="field in formDefinition.schema.fields"
            :key="field.id"
            :label="field.label"
            :name="field.id"
            :rules="field.rules"
        >
          <component
              :is="getComponentByType(field.type)"
              v-model:value="formData[field.id]"
              :placeholder="field.props.placeholder"
              :options="field.type === 'Select' ? field.props.options.map(o => ({label: o, value: o})) : undefined"
          />
        </a-form-item>
      </a-form>
    </a-spin>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getFormById, submitForm } from '@/api';
import { message } from 'ant-design-vue';
import {
  Input as AInput,
  Textarea as ATextarea,
  Select as ASelect,
  Checkbox as ACheckbox,
  DatePicker as ADatePicker,
} from 'ant-design-vue';

const props = defineProps({ formId: String });
const router = useRouter();

const loading = ref(true);
const submitting = ref(false);
const formDefinition = ref({ schema: { fields: [] } });
const formData = reactive({});
const formRef = ref();

const getComponentByType = (type) => ({
  Input: AInput, Textarea: ATextarea, Select: ASelect, Checkbox: ACheckbox, DatePicker: ADatePicker,
}[type] || AInput);

onMounted(async () => {
  try {
    const res = await getFormById(props.formId);
    res.schema = JSON.parse(res.schemaJson);
    // 初始化 formData
    res.schema.fields.forEach(field => {
      formData[field.id] = undefined;
    });
    formDefinition.value = res;
  } catch (error) {
    message.error('加载表单失败');
  } finally {
    loading.value = false;
  }
});

const handleSubmit = async () => {
  try {
    await formRef.value.validate();
    submitting.value = true;
    const payload = { dataJson: JSON.stringify(formData) };
    await submitForm(props.formId, payload);
    message.success('申请提交成功！');
    router.push('/');
  } catch (errorInfo) {
    if (errorInfo.errorFields) {
      message.warn('请填写所有必填项');
    } else {
      message.error('提交失败');
    }
  } finally {
    submitting.value = false;
  }
};
</script>