<template>
  <a-form-item label="选项 (每行一个)">
    <a-textarea v-model:value="optionsText" :rows="4" />
  </a-form-item>
</template>

<script setup>
import { computed } from 'vue';
const props = defineProps(['field']);

const optionsText = computed({
  get() {
    return props.field.props.options.join('\n');
  },
  set(value) {
    props.field.props.options = value.split('\n').filter(opt => opt.trim());
  }
});
</script>