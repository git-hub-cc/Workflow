<template>
  <div class="rich-text-container" v-html="resolvedContent"></div>
</template>

<script setup>
import { computed } from 'vue';
import { resolveBinding } from '@/utils/bindingResolver';

const props = defineProps({
  content: String,
  businessData: Object,
});

const resolvedContent = computed(() => resolveBinding(props.content, props.businessData));
</script>

<style scoped>
.rich-text-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
  line-height: 1.8;
}

/* 确保v-html渲染的内容样式表现良好 */
.rich-text-container :deep(h1),
.rich-text-container :deep(h2),
.rich-text-container :deep(h3) {
  margin-top: 1.5em;
  margin-bottom: 0.8em;
  font-weight: 600;
}

.rich-text-container :deep(p) {
  margin-bottom: 1em;
}

.rich-text-container :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
}

.rich-text-container :deep(blockquote) {
  border-left: 4px solid #e8e8e8;
  padding-left: 1em;
  margin-left: 0;
  color: #595959;
}

.rich-text-container :deep(pre) {
  background-color: #f5f5f5;
  padding: 1em;
  border-radius: 4px;
  overflow-x: auto;
}
</style>