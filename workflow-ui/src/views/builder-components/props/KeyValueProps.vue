<template>
  <div>
    <!-- 引入通用属性配置 -->
    <GenericProps :field="field" :all-fields="allFields" />

    <a-divider>键值对配置</a-divider>
    <a-form-item label="Key 输入框占位提示">
      <a-input v-model:value="field.props.keyPlaceholder" />
    </a-form-item>
    <a-form-item label="Value 输入框占位提示">
      <a-input v-model:value="field.props.valuePlaceholder" />
    </a-form-item>
  </div>
</template>

<script setup>
import GenericProps from './GenericProps.vue';

defineProps({
  field: {
    type: Object,
    required: true,
  },
  allFields: {
    type: Array,
    required: true,
  },
});
</script>