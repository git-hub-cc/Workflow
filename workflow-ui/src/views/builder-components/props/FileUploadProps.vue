<template>
  <div>
    <a-form-item label="是否允许多选">
      <a-switch v-model:checked="field.props.multiple" />
    </a-form-item>
    <a-form-item label="最大上传数量">
      <a-input-number v-model:value="field.props.maxCount" :min="1" />
    </a-form-item>
  </div>
</template>

<script setup>
defineProps(['field']);
</script>