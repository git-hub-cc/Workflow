<template>
  <div class="properties">
    <a-card title="属性配置" size="small">
      <div v-if="!selectedField" class="properties-placeholder">
        选中一个组件以编辑其属性
      </div>
      <a-form v-else :model="localField" layout="vertical">
        <!-- 【核心修改】: 动态渲染属性表单 -->
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
let isInternalUpdate = false;

watch(() => props.selectedField, (newField) => {
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
    emit('update:field', newVal);
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
      // --- 【新增】 ---
    case 'StaticText':
      return defineAsyncComponent(() => import('./props/StaticTextProps.vue'));
      // --- 【新增结束】 ---
    default:
      return defineAsyncComponent(() => import('./props/GenericProps.vue'));
  }
});
</script>

<style scoped>
.properties {
  width: 320px;
  flex-shrink: 0;
  overflow-y: auto;
  max-height: calc(100vh - 200px);
}
.properties-placeholder {
  color: #aaa;
  text-align: center;
  padding-top: 24px;
}
</style>