<template>
  <div class="properties">
    <a-card title="属性配置" size="small">
      <div v-if="!selectedField" class="properties-placeholder">
        选中一个组件以编辑其属性
      </div>
      <a-form v-else :key="selectedField.id" :model="localField" layout="vertical">
        <component :is="propertiesComponent" :field="localField" :all-fields="allFields" />
      </a-form>
    </a-card>
  </div>
</template>

<script setup>
// ... script部分保持不变 ...
import { ref, watch, computed, defineAsyncComponent, nextTick } from 'vue';

const props = defineProps(['selectedField', 'allFields']);
const emit = defineEmits(['update:field']);

const localField = ref(null);
let isInternalUpdate = false;
let debounceTimer = null;

watch(() => props.selectedField, (newField) => {
  clearTimeout(debounceTimer);
  isInternalUpdate = true;
  if (newField) {
    localField.value = JSON.parse(JSON.stringify(newField));
  } else {
    localField.value = null;
  }
  nextTick(() => {
    isInternalUpdate = false;
  });
}, { deep: true, immediate: true });

watch(localField, (newVal) => {
  if (isInternalUpdate) return;
  if (newVal) {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
      emit('update:field', newVal);
    }, 100);
  }
}, { deep: true });

const propertiesComponent = computed(() => {
  if (!localField.value || !localField.value.type) return null;

  const type = localField.value.type;
  switch (type) {
    case 'GridRow':
      return defineAsyncComponent(() => import('./props/GridRowProps.vue'));
    case 'GridCol':
      return defineAsyncComponent(() => import('./props/GridColProps.vue'));
    case 'Collapse':
      return defineAsyncComponent(() => import('./props/CollapseProps.vue'));
    case 'CollapsePanel':
      return defineAsyncComponent(() => import('./props/CollapsePanelProps.vue'));
    case 'DataPicker':
      return defineAsyncComponent(() => import('./props/DataPickerProps.vue'));
    case 'FileUpload':
      return defineAsyncComponent(() => import('./props/FileUploadProps.vue'));
    case 'Subform':
      return defineAsyncComponent(() => import('./props/SubformProps.vue'));
    case 'DescriptionList':
      return defineAsyncComponent(() => import('./props/DescriptionListProps.vue'));
    case 'KeyValue':
      return defineAsyncComponent(() => import('./props/KeyValueProps.vue'));
    case 'IconPicker':
      return defineAsyncComponent(() => import('./props/IconPickerProps.vue'));
    case 'TreeSelect':
      return defineAsyncComponent(() => import('./props/GenericProps.vue'));
    case 'StaticText':
      return defineAsyncComponent(() => import('./props/StaticTextProps.vue'));
      // --- 【核心修改】新增 RichText 的属性面板 ---
    case 'RichText':
      return defineAsyncComponent(() => import('./props/RichTextProps.vue'));
    default:
      return defineAsyncComponent(() => import('./props/GenericProps.vue'));
  }
});
</script>

<!-- 【核心修改】重写整个样式部分 -->
<style scoped>
.properties {
  width: 320px;
  flex-shrink: 0;
  display: flex; /* 1. 使自身成为 flex 容器 */
  flex-direction: column; /* 垂直排列 */
}

/* 2. 使用 :deep() 选择器让 antd 卡片组件充满 .properties 容器 */
.properties :deep(.ant-card) {
  flex-grow: 1; /* 关键：让卡片填满所有可用垂直空间 */
  display: flex;
  flex-direction: column;
}

/* 3. 让卡片的内容区域（body）成为可滚动区域 */
.properties :deep(.ant-card-body) {
  flex-grow: 1; /* 关键：让内容区填满卡片内剩余空间 */
  overflow-y: auto; /* 当内容溢出时，显示滚动条 */
  padding: 12px; /* 可以适当调整内边距 */
}

.properties-placeholder {
  color: #aaa;
  text-align: center;
  padding-top: 24px;
}
</style>