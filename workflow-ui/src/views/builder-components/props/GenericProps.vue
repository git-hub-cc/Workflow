<template>
  <div>
    <!-- 通用属性 -->
    <a-form-item label="字段ID">
      <a-input v-model:value="field.id" disabled />
    </a-form-item>
    <a-form-item label="标题 (Label)">
      <a-input v-model:value="field.label" />
    </a-form-item>

    <!-- 【核心新增】日期选择器模式配置 -->
    <template v-if="field.type === 'DatePicker'">
      <a-form-item label="选择模式">
        <a-radio-group v-model:value="field.props.pickerMode" button-style="solid" size="small" @change="handlePickerModeChange">
          <a-radio-button value="single">单日</a-radio-button>
          <a-radio-button value="range">范围</a-radio-button>
          <a-radio-button value="multiple">多日</a-radio-button>
        </a-radio-group>
      </a-form-item>
    </template>

    <a-form-item v-if="field.props && 'placeholder' in field.props" label="占位提示 (Placeholder)">
      <!-- 【核心修改】为范围选择器提供专门的占位符输入 -->
      <a-space v-if="field.type === 'DatePicker' && field.props.pickerMode === 'range'">
        <a-input v-model:value="field.props.placeholder[0]" placeholder="开始日期提示" />
        <a-input v-model:value="field.props.placeholder[1]" placeholder="结束日期提示" />
      </a-space>
      <a-input v-else v-model:value="field.props.placeholder" />
    </a-form-item>

    <!-- 【核心新增】新组件的特有属性 -->
    <template v-if="field.type === 'InputNumber'">
      <a-row :gutter="16">
        <a-col :span="12"><a-form-item label="最小值"><a-input-number v-model:value="field.props.min" style="width: 100%;" /></a-form-item></a-col>
        <a-col :span="12"><a-form-item label="最大值"><a-input-number v-model:value="field.props.max" style="width: 100%;" /></a-form-item></a-col>
      </a-row>
    </template>
    <template v-if="field.type === 'Switch'">
      <a-row :gutter="16">
        <a-col :span="12"><a-form-item label="开启时文字"><a-input v-model:value="field.props.checkedChildren" /></a-form-item></a-col>
        <a-col :span="12"><a-form-item label="关闭时文字"><a-input v-model:value="field.props.unCheckedChildren" /></a-form-item></a-col>
      </a-row>
    </template>
    <template v-if="field.type === 'Slider'">
      <a-row :gutter="16">
        <a-col :span="12"><a-form-item label="最小值"><a-input-number v-model:value="field.props.min" style="width: 100%;" /></a-form-item></a-col>
        <a-col :span="12"><a-form-item label="最大值"><a-input-number v-model:value="field.props.max" style="width: 100%;" /></a-form-item></a-col>
      </a-row>
      <a-form-item label="范围选择"><a-switch v-model:checked="field.props.range" /></a-form-item>
    </template>
    <template v-if="field.type === 'Rate'">
      <a-row :gutter="16">
        <a-col :span="12"><a-form-item label="总星数"><a-input-number v-model:value="field.props.count" :min="1" style="width: 100%;" /></a-form-item></a-col>
        <a-col :span="12"><a-form-item label="允许半选"><a-switch v-model:checked="field.props.allowHalf" /></a-form-item></a-col>
      </a-row>
    </template>

    <!-- 列表页配置 -->
    <a-form-item label="用作列表筛选条件" help="在数据列表页面，此字段将作为一个筛选查询条件。">
      <a-switch v-model:checked="field.isFilterable" />
    </a-form-item>
    <a-form-item label="在列表中作为列显示" help="在数据列表页面，此字段的值将作为一列进行展示。">
      <a-switch v-model:checked="field.showInList" />
    </a-form-item>


    <!-- 数据源配置 -->
    <DataSourceConfig
        v-if="field.dataSource"
        :field="field"
        :all-fields="allFields"
        @update:field="$emit('update:field', $event)"
    />

    <!-- 校验规则 -->
    <ValidationRulesConfig
        v-if="field.rules !== undefined"
        :rules="field.rules"
        :field="field"
        :all-fields="allFields"
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
import { computed, watchEffect } from 'vue';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue';
import { flattenFields } from '@/utils/formUtils.js';
import DataSourceConfig from './DataSourceConfig.vue';
import ValidationRulesConfig from './ValidationRulesConfig.vue';

const props = defineProps(['field', 'allFields']);
const emit = defineEmits(['update:field']);

watchEffect(() => {
  if (props.field.type === 'DatePicker' && !props.field.props.pickerMode) {
    props.field.props.pickerMode = 'single';
  }
});

const handlePickerModeChange = (e) => {
  const mode = e.target.value;
  const placeholder = props.field.props.placeholder;
  if (mode === 'range') {
    if (!Array.isArray(placeholder)) {
      props.field.props.placeholder = [placeholder || '开始日期', '结束日期'];
    }
  } else {
    if (Array.isArray(placeholder)) {
      props.field.props.placeholder = placeholder[0] || '';
    }
  }
};

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