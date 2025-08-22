<template>
  <div>
    <!-- 引入通用属性配置 -->
    <GenericProps :field="field" :all-fields="allFields" />

    <a-divider>文件上传配置</a-divider>
    <a-form-item label="是否允许多选">
      <a-switch v-model:checked="field.props.multiple" />
    </a-form-item>
    <a-form-item label="最大上传数量">
      <a-input-number v-model:value="field.props.maxCount" :min="1" />
    </a-form-item>

    <!-- 【核心新增】文件校验配置 -->
    <a-divider>文件校验</a-divider>
    <a-form-item label="文件大小限制 (MB)">
      <a-input-number v-model:value="field.props.maxSize" :min="1" addon-after="MB" />
    </a-form-item>
    <a-form-item label="允许的文件类型" help="用逗号分隔，如 .jpg,.png,application/pdf">
      <a-input v-model:value="field.props.allowedTypes" placeholder=".jpg,.png" />
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