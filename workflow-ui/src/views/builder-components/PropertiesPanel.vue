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
import { ref, watch, computed, defineAsyncComponent } from 'vue';

const props = defineProps(['selectedField', 'allFields']);
const emit = defineEmits(['update:field']);

const localField = ref(null);
let debounceTimer = null;

watch(() => props.selectedField, (newField) => {
  if (newField?.id !== localField.value?.id) {
    if (newField) {
      localField.value = JSON.parse(JSON.stringify(newField));
    } else {
      localField.value = null;
    }
  }
}, { deep: true, immediate: true });


watch(localField, (newVal, oldVal) => {
  if (newVal && oldVal && newVal.id === oldVal.id) {
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
      return defineAsyncComponent(() => import('./props/LayoutProps.vue'));
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
    case 'RadioGroup':
      return defineAsyncComponent(() => import('./props/GenericProps.vue'));
    case 'StaticText':
      return defineAsyncComponent(() => import('./props/StaticTextProps.vue'));
    case 'RichText':
      return defineAsyncComponent(() => import('./props/RichTextProps.vue'));
    case 'Divider':
      return defineAsyncComponent(() => import('./props/DividerProps.vue'));
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

/* 【核心新增】在移动端，属性面板可能在 Drawer 中，需要确保它能填满空间 */
@media (max-width: 768px) {
  .properties {
    width: 100%;
    height: 100%;
  }
}
</style>