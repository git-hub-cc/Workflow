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
let isInternalUpdate = false;
// 【新增】用于防抖的计时器变量
let debounceTimer = null;

watch(() => props.selectedField, (newField) => {
  // 当选中的字段改变时，清除可能存在的待处理的更新，防止旧的更新覆盖新的状态
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

// 【修改】对 localField 的侦听器进行防抖处理
watch(localField, (newVal) => {
  if (isInternalUpdate) return;
  if (newVal) {
    // 清除上一个计时器，以确保只有在用户停止输入后才触发
    clearTimeout(debounceTimer);
    // 设置一个新的计时器，延迟 100 毫秒后 emit 更新
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