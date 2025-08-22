<template>
  <div>
    <a-divider>校验规则</a-divider>

    <div v-for="(rule, index) in localRules" :key="index" class="validation-rule-card">
      <div class="rule-header">
        <strong>规则 {{ index + 1 }}</strong>
        <a-button v-if="index > 0" type="text" danger size="small" @click="removeRule(index)">
          <DeleteOutlined />
        </a-button>
      </div>
      <div class="rule-body">
        <a-checkbox v-if="'required' in rule" v-model:checked="rule.required">必填</a-checkbox>
        <template v-else>
          <a-select v-model:value="rule.type" placeholder="规则类型" style="width: 120px;">
            <a-select-option value="string">文本</a-select-option>
            <a-select-option value="number">数字</a-select-option>
            <a-select-option value="email">邮箱</a-select-option>
            <a-select-option value="url">网址</a-select-option>
            <a-select-option value="pattern">正则表达式</a-select-option>
            <a-select-option value="compare">字段比较</a-select-option>
          </a-select>

          <template v-if="rule.type === 'string'">
            <a-input-number v-model:value="rule.min" placeholder="最小长度" style="width: 100px;" />
            <a-input-number v-model:value="rule.max" placeholder="最大长度" style="width: 100px;" />
          </template>

          <a-input v-if="rule.type === 'pattern'" v-model:value="rule.pattern" placeholder="输入正则表达式" style="flex-grow: 1;" />

          <template v-if="rule.type === 'compare'">
            <a-select v-model:value="rule.compareOperator" style="width: 90px;">
              <a-select-option value="==">等于</a-select-option>
              <a-select-option value="!=">不等于</a-select-option>
              <a-select-option value=">">大于</a-select-option>
              <a-select-option value="<">小于</a-select-option>
            </a-select>
            <a-select v-model:value="rule.compareField" placeholder="选择字段" style="flex-grow: 1;">
              <a-select-option v-for="f in availableFieldsForCondition" :key="f.id" :value="f.id">
                {{ f.label }}
              </a-select-option>
            </a-select>
          </template>

        </template>
        <a-input v-model:value="rule.message" placeholder="自定义错误提示" style="margin-top: 8px; width: 100%;" />
      </div>
    </div>

    <a-dropdown>
      <a-button type="dashed" block>
        <PlusOutlined /> 添加校验规则
      </a-button>
      <template #overlay>
        <a-menu @click="handleAddRule">
          <a-menu-item key="string">长度校验</a-menu-item>
          <a-menu-item key="number">数字校验</a-menu-item>
          <a-menu-item key="email">邮箱校验</a-menu-item>
          <a-menu-item key="url">网址校验</a-menu-item>
          <a-menu-item key="pattern">正则校验</a-menu-item>
          <a-menu-item key="compare">字段比较校验</a-menu-item>
          <!-- 【新增】合计校验选项 -->
          <a-menu-item key="sum" v-if="isSubform">子表单合计校验</a-menu-item>
        </a-menu>
      </template>
    </a-dropdown>

    <!-- 【新增】合计校验的详细配置 -->
    <template v-for="(rule, index) in sumRules" :key="'sum-' + index">
      <div class="validation-rule-card">
        <div class="rule-header">
          <strong>子表单合计校验</strong>
          <a-button type="text" danger size="small" @click="removeSumRule(index)">
            <DeleteOutlined />
          </a-button>
        </div>
        <div class="rule-body">
          <span>子表单中</span>
          <a-select v-model:value="rule.subformColumn" placeholder="选择列" style="width: 120px;">
            <a-select-option v-for="col in subformNumericColumns" :key="col.id" :value="col.id">
              {{ col.label }}
            </a-select-option>
          </a-select>
          <span>的总和</span>
          <a-select v-model:value="rule.compareOperator" style="width: 90px;">
            <a-select-option value="==">等于</a-select-option>
            <a-select-option value=">=">大于等于</a-select-option>
            <a-select-option value="<=">小于等于</a-select-option>
          </a-select>
          <span>主表中字段</span>
          <a-select v-model:value="rule.mainFormField" placeholder="选择字段" style="flex-grow: 1;">
            <a-select-option v-for="f in mainFormNumericFields" :key="f.id" :value="f.id">
              {{ f.label }}
            </a-select-option>
          </a-select>
          <a-input v-model:value="rule.message" placeholder="自定义错误提示" style="margin-top: 8px; width: 100%;" />
        </div>
      </div>
    </template>

  </div>
</template>

<script setup>
import { computed } from 'vue';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue';
import { flattenFields } from '@/utils/formUtils.js';

const props = defineProps({
  rules: { type: Array, required: true },
  field: { type: Object, required: true },
  allFields: { type: Array, required: true },
});
const emit = defineEmits(['update:rules']);

const localRules = computed({
  get: () => props.rules.filter(r => r.type !== 'sum'),
  set: (newRules) => {
    emit('update:rules', [...newRules, ...sumRules.value]);
  }
});

const sumRules = computed(() => props.rules.filter(r => r.type === 'sum'));

const isSubform = computed(() => props.field.type === 'Subform');

const availableFieldsForCondition = computed(() => {
  return flattenFields(props.allFields).filter(f =>
      f.id !== props.field.id && !['GridRow', 'GridCol', 'Subform', 'RichText', 'FileUpload', 'DescriptionList'].includes(f.type)
  );
});

// --- 【新增】为合计校验提供数据源 ---
const subformNumericColumns = computed(() => {
  if (!isSubform.value) return [];
  // 假设只有 Input 类型的列可以用于求和
  return props.field.props.columns.filter(col => col.type === 'Input');
});

const mainFormNumericFields = computed(() => {
  return flattenFields(props.allFields).filter(f =>
      f.type === 'Input' || f.type === 'InputNumber' // 假设主表中也有数字输入框
  );
});


const handleAddRule = ({ key }) => {
  let newRule = { type: key, message: '' };
  switch (key) {
    case 'string': newRule.message = '文本格式不正确'; break;
    case 'number': newRule.message = '请输入有效的数字'; break;
    case 'email': newRule.message = '请输入有效的邮箱地址'; break;
    case 'url': newRule.message = '请输入有效的网址'; break;
    case 'pattern': newRule.pattern = ''; newRule.message = '不符合正则表达式规则'; break;
    case 'compare': newRule.compareField = ''; newRule.compareOperator = '=='; newRule.message = '字段值不匹配'; break;
    case 'sum':
      newRule = {
        type: 'sum',
        subformColumn: '',
        compareOperator: '==',
        mainFormField: '',
        message: '子表单合计值不正确',
      };
      // 合计规则是独立的
      emit('update:rules', [...props.rules, newRule]);
      return;
  }
  localRules.value = [...localRules.value, newRule];
};

const removeRule = (index) => {
  if (index >= 0) {
    const newRules = [...localRules.value];
    newRules.splice(index + 1, 1); // +1 to account for the required rule which is not in localRules
    localRules.value = newRules;
  }
};

const removeSumRule = (index) => {
  const newSumRules = [...sumRules.value];
  newSumRules.splice(index, 1);
  emit('update:rules', [...localRules.value, ...newSumRules]);
};
</script>

<style scoped>
.validation-rule-card {
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 8px;
  margin-bottom: 12px;
}
.rule-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.rule-body {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
</style>