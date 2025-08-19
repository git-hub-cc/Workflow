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
import { ref, watch, computed, defineAsyncComponent, nextTick } from 'vue';

const props = defineProps(['selectedField', 'allFields']);
const emit = defineEmits(['update:field']);

const localField = ref(null);
let debounceTimer = null;

// 【核心修复】修改 watch 逻辑
watch(() => props.selectedField, (newField) => {
  // 只有当选择的字段ID发生变化时 (即用户点击了另一个组件)，才重置 localField
  if (newField?.id !== localField.value?.id) {
    if (newField) {
      // 使用深拷贝创建一个可编辑的本地副本
      localField.value = JSON.parse(JSON.stringify(newField));
    } else {
      localField.value = null;
    }
  }
}, { deep: true, immediate: true });


// 这个 watch 保持不变，它负责将本地的修改同步到父组件
watch(localField, (newVal, oldVal) => {
  // 仅当 newVal 存在且与 oldVal 的 ID 相同（表示是编辑而非切换组件）时才触发更新
  if (newVal && oldVal && newVal.id === oldVal.id) {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
      emit('update:field', newVal);
    }, 100); // 使用防抖减少事件触发频率
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
    case 'RichText':
      return defineAsyncComponent(() => import('./props/RichTextProps.vue'));
    default:
      return defineAsyncComponent(() => import('./props/GenericProps.vue'));
  }
});
</script>

<style scoped>
.properties {
  width: 320px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.properties :deep(.ant-card) {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
}

.properties :deep(.ant-card-body) {
  flex-grow: 1;
  overflow-y: auto;
  padding: 12px;
}

.properties-placeholder {
  color: #aaa;
  text-align: center;
  padding-top: 24px;
}
</style>