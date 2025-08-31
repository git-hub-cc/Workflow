<template>
  <a-modal
      :open="open"
      :title="`预览: ${formDefinition.name || '无标题表单'}`"
      @update:open="(val) => emit('update:open', val)"
      width="800px"
      :footer="null"
      destroyOnClose
  >
    <div style="max-height: 70vh; overflow-y: auto; padding: 24px;">
      <a-alert
          message="预览模式"
          description="这是一个预览界面，用于展示表单的最终渲染效果和交互。所有数据不会被提交。"
          type="info"
          show-icon
          style="margin-bottom: 24px;"
      />

      <a-form
          :model="formData"
          layout="vertical"
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
  </a-modal>
</template>

<script setup>
import { reactive, watchEffect, defineAsyncComponent } from 'vue';
import { initFormData } from '@/utils/formUtils.js';

const FormItemRenderer = defineAsyncComponent(() => import('@/views/viewer-components/FormItemRenderer.vue'));

const props = defineProps({
  open: Boolean,
  formDefinition: {
    type: Object,
    required: true,
  },
});
const emit = defineEmits(['update:open']);

const formData = reactive({});

// 当表单定义变化时 (即模态框打开或内容更新时)，重新初始化预览用的 formData
watchEffect(() => {
  if (props.formDefinition && props.formDefinition.schema) {
    initFormData(props.formDefinition.schema.fields, formData);
  }
});

const updateFormData = (fieldId, value) => {
  formData[fieldId] = value;
};
</script>