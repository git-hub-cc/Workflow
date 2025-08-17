<template>
  <div>
    <a-form-item label="组件类型">
      <a-tag color="purple">栅格行 (Row)</a-tag>
    </a-form-item>
    <a-form-item label="列间距 (Gutter)">
      <a-slider v-model:value="field.props.gutter" :min="0" :max="48" />
    </a-form-item>
  </div>
</template>

<script setup>
defineProps(['field']);
</script>