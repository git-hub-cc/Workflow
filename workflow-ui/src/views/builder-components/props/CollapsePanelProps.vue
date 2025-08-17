<template>
  <div>
    <a-form-item label="组件类型">
      <a-tag color="cyan">折叠面板 (Panel)</a-tag>
    </a-form-item>
    <a-form-item label="面板标题 (Header)">
      <a-input v-model:value="field.props.header" />
    </a-form-item>
  </div>
</template>

<script setup>
defineProps(['field']);
</script>