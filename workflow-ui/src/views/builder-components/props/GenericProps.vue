<template>
  <div>
    <!-- 通用属性 -->
    <a-form-item label="字段ID">
      <a-input v-model:value="field.id" disabled />
    </a-form-item>
    <a-form-item label="标题 (Label)">
      <a-input v-model:value="field.label" />
    </a-form-item>
    <a-form-item v-if="field.props && 'placeholder' in field.props" label="占位提示 (Placeholder)">
      <a-input v-model:value="field.props.placeholder" />
    </a-form-item>

    <!-- 数据源配置 -->
    <DataSourceConfig
        v-if="field.dataSource"
        :field="field"
        @update:field="$emit('update:field', $event)"
    />

    <!-- 校验规则 -->
    <ValidationRulesConfig
        v-if="field.rules !== undefined"
        :rules="field.rules"
        @update:rules="newRules => field.rules = newRules"
    />

    <!-- 条件显隐 -->
    <template v-if="field.visibility">
      <a-divider>条件显隐</a-divider>
      <a-form-item>
        <a-switch v-model:checked="field.visibility.enabled" checked-children="启用" un-checked-children="禁用" />
      </a-form-item>
      <template v-if="field.visibility.enabled">
        <p>当满足以下条件时显示此组件:</p>
        <a-radio-group v-model:value="field.visibility.condition" button-style="solid" size="small">
          <a-radio-button value="AND">所有 (AND)</a-radio-button>
          <a-radio-button value="OR">任意 (OR)</a-radio-button>
        </a-radio-group>

        <div v-for="(rule, index) in field.visibility.rules" :key="index" class="condition-rule">
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
    </template>
  </div>
</template>

<script setup>
import { computed, defineEmits } from 'vue';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue';
// --- 【路径已修改】 ---
import { flattenFields } from '@/utils/formUtils.js';
import DataSourceConfig from './DataSourceConfig.vue';
import ValidationRulesConfig from './ValidationRulesConfig.vue';

const props = defineProps(['field', 'allFields']);
const emit = defineEmits(['update:field']);

const availableFieldsForCondition = computed(() => {
  if (!props.field) return [];
  return flattenFields(props.allFields).filter(f => f.id !== props.field.id && !['GridRow', 'GridCol', 'Subform', 'RichText', 'FileUpload', 'DescriptionList'].includes(f.type));
});

const addConditionRule = () => {
  if (!props.field.visibility.rules) {
    props.field.visibility.rules = [];
  }
  props.field.visibility.rules.push({ fieldId: '', operator: '==', value: '' });
};

const removeConditionRule = (index) => {
  props.field.visibility.rules.splice(index, 1);
};
</script>

<style scoped>
.condition-rule {
  display: flex;
  gap: 8px;
  margin: 12px 0;
  align-items: center;
}
</style>