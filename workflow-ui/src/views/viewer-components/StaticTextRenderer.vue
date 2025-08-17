<template>
  <div class="static-text-container" :style="containerStyle">
    <component :is="tag" v-html="content" />
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  field: {
    type: Object,
    required: true,
  },
});

const content = computed(() => props.field.props?.content || '');
const tag = computed(() => props.field.props?.tag || 'div');
const containerStyle = computed(() => {
  // 增加一点下边距，使其与 antd 的 a-form-item 视觉上更协调
  return {
    marginBottom: '24px'
  };
});
</script>

<style scoped>
/*
  确保从 Word 转换过来的 HTML 元素样式被正确应用。
  使用 :deep() 选择器来穿透到 v-html 渲染的内容。
*/
.static-text-container :deep(h1),
.static-text-container :deep(h2),
.static-text-container :deep(h3) {
  margin-top: 0;
  margin-bottom: 0.5em;
  font-weight: 600;
  line-height: 1.25;
}

.static-text-container :deep(h1) {
  font-size: 2em;
}

.static-text-container :deep(h2) {
  font-size: 1.5em;
}

.static-text-container :deep(p) {
  margin-bottom: 1em;
}

.static-text-container :deep(strong) {
  font-weight: bold;
}
</style>