<template>
  <div>
    <a-form-item label="组件类型">
      <a-tag color="blue">栅格列 (Column)</a-tag>
    </a-form-item>
    <a-form-item label="栅格宽度 (Span)">
      <a-slider v-model:value="field.props.span" :min="1" :max="24" />
    </a-form-item>
  </div>
</template>

<script setup>
defineProps(['field']);
</script>