<template>
  <div class="properties">
    <a-card title="属性配置" size="small">
      <div v-if="!selectedField" class="properties-placeholder">
        选中一个组件以编辑其属性
      </div>
      <a-form v-else :model="localField" layout="vertical">
        <a-form-item label="字段ID">
          <a-input v-model:value="localField.id" disabled />
        </a-form-item>
        <a-form-item label="标题 (Label)">
          <a-input v-model:value="localField.label" />
        </a-form-item>

        <template v-if="!['Layout', 'Subform'].includes(localField.type)">
          <a-form-item label="占位提示 (Placeholder)">
            <a-input v-model:value="localField.props.placeholder" />
          </a-form-item>
          <a-form-item>
            <a-checkbox v-model:checked="localField.rules[0].required">
              是否必填
            </a-checkbox>
          </a-form-item>
        </template>

        <!-- 特定组件属性 -->
        <component :is="propertiesComponent" :field="localField" />

        <!-- 条件显隐配置 -->
        <a-divider>条件显隐</a-divider>
        <a-form-item>
          <a-switch v-model:checked="localField.visibility.enabled" checked-children="启用" un-checked-children="禁用" />
        </a-form-item>
        <template v-if="localField.visibility.enabled">
          <p>当满足以下条件时显示此组件:</p>
          <a-radio-group v-model:value="localField.visibility.condition" button-style="solid" size="small">
            <a-radio-button value="AND">所有 (AND)</a-radio-button>
            <a-radio-button value="OR">任意 (OR)</a-radio-button>
          </a-radio-group>

          <div v-for="(rule, index) in localField.visibility.rules" :key="index" class="condition-rule">
            <a-select v-model:value="rule.fieldId" placeholder="选择字段" style="width: 120px;">
              <a-select-option v-for="f in availableFieldsForCondition" :key="f.id" :value="f.id">{{ f.label }}</a-select-option>
            </a-select>
            <a-select v-model:value="rule.operator" placeholder="操作" style="width: 80px;">
              <a-select-option value="==">等于</a-select-option>
              <a-select-option value="!=">不等于</a-select-option>
              <a-select-option value=">">大于</a-select-option>
              <a-select-option value="<">小于</a-select-option>
            </a-select>
            <a-input v-model:value="rule.value" placeholder="值" style="flex-grow: 1;" />
            <a-button type="text" danger @click="removeConditionRule(index)"><DeleteOutlined /></a-button>
          </div>
          <a-button type="dashed" block @click="addConditionRule">
            <PlusOutlined /> 添加条件
          </a-button>
        </template>

      </a-form>
    </a-card>
  </div>
</template>

<script setup>
import { ref, watch, computed, defineAsyncComponent, nextTick } from 'vue';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue';
import { flattenFields } from '../viewer-components/formUtils';

const props = defineProps(['selectedField', 'allFields']);
const emit = defineEmits(['update:field']);

const localField = ref(null);

// --- 【核心修复区域】 ---

// 1. 添加一个标志位，用于判断更新是来自外部prop还是内部用户操作
let isInternalUpdate = false;

// 2. 监视外部 prop (selectedField) 的变化
watch(() => props.selectedField, (newField) => {
  // 当 prop 变化时，设置标志位，表示这是一个来自外部的、程序化的更新
  isInternalUpdate = true;
  if (newField) {
    // 深拷贝 prop 数据到本地 state
    localField.value = JSON.parse(JSON.stringify(newField));
  } else {
    localField.value = null;
  }
  // 使用 nextTick 确保在 Vue 完成 DOM 更新后重置标志位
  // 这样，后续的用户操作就能正常触发 emit
  nextTick(() => {
    isInternalUpdate = false;
  });
}, { deep: true });

// 3. 监视内部 state (localField) 的变化
watch(localField, (newVal) => {
  // 如果标志位为 true，说明这次变化是由上面的 watcher 引起的，
  // 我们就阻止它 emit 事件，从而打破循环。
  if (isInternalUpdate) {
    return;
  }

  // 如果标志位为 false，说明这次变化是用户在属性面板里输入/修改造成的，
  // 此时我们才需要将更新通知父组件。
  if (newVal) {
    emit('update:field', newVal);
  }
}, { deep: true });

// --- 【修复结束】 ---


const propertiesComponent = computed(() => {
  if (!localField.value) return null;
  const type = localField.value.type;
  switch(type) {
    case 'Select': return defineAsyncComponent(() => import('./props/SelectProps.vue'));
    case 'UserPicker': return defineAsyncComponent(() => import('./props/UserPickerProps.vue'));
    case 'FileUpload': return defineAsyncComponent(() => import('./props/FileUploadProps.vue'));
    case 'Subform': return defineAsyncComponent(() => import('./props/SubformProps.vue'));
    case 'Layout': return defineAsyncComponent(() => import('./props/LayoutProps.vue'));
    default: return null;
  }
});

const availableFieldsForCondition = computed(() => {
  if (!localField.value) return [];
  return flattenFields(props.allFields).filter(f => f.id !== localField.value.id && !['Layout', 'Subform', 'RichText', 'FileUpload'].includes(f.type));
});

const addConditionRule = () => {
  localField.value.visibility.rules.push({ fieldId: '', operator: '==', value: '' });
};

const removeConditionRule = (index) => {
  localField.value.visibility.rules.splice(index, 1);
};

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
.condition-rule {
  display: flex;
  gap: 8px;
  margin: 12px 0;
  align-items: center;
}
</style>