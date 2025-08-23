<template>
  <div class="props-container">
    <DefaultProps :selected-element="selectedElement" :modeler="modeler" @update="properties => $emit('update', properties)" />

    <!-- 只有当顺序流的源头是网关时，才显示条件配置 -->
    <div v-if="isConditionalFlow">
      <a-divider>条件配置</a-divider>
      <a-form layout="vertical">
        <a-form-item label="条件类型">
          <a-radio-group v-model:value="conditionType" button-style="solid" size="small" @change="onConditionTypeChange">
            <a-radio-button value="none">无条件 (默认流)</a-radio-button>
            <!-- 【核心新增】新增“审核”选项 -->
            <a-radio-button value="audit">审核</a-radio-button>
            <a-radio-button value="builder">条件构建器</a-radio-button>
            <a-radio-button value="expression">表达式</a-radio-button>
          </a-radio-group>
        </a-form-item>

        <!-- 【核心新增】当选择“审核”类型时，显示此选择框 -->
        <a-form-item v-if="conditionType === 'audit'" label="审核结果">
          <a-select
              v-model:value="auditOutcome"
              placeholder="请选择一个审核结果"
              :options="auditOptions"
              @change="applyAuditChanges"
          />
        </a-form-item>

        <!-- 条件构建器 -->
        <div v-if="conditionType === 'builder'" class="condition-builder">
          <a-select
              v-model:value="builderState.field"
              placeholder="选择表单字段"
              :options="formFieldsForSelect"
              show-search
              option-filter-prop="label"
              style="margin-bottom: 8px;"
              @change="applyBuilderChanges"
          />
          <a-select v-model:value="builderState.operator" style="width: 100px; margin-bottom: 8px;" @change="applyBuilderChanges">
            <a-select-option value="==">等于 (==)</a-select-option>
            <a-select-option value="!=">不等于 (!=)</a-select-option>
            <a-select-option value=">">大于 (&gt;)</a-select-option>
            <a-select-option value=">=">大于等于 (&gt;=)</a-select-option>
            <a-select-option value="<">小于 (&lt;)</a-select-option>
            <a-select-option value="<=">小于等于 (&lt;=)</a-select-option>
          </a-select>
          <a-input v-model:value="builderState.value" placeholder="输入比较值" @change="applyBuilderChanges" />
          <p class="help-text">提示: 文本值请用双引号包裹, 如 "beijing"</p>
        </div>

        <!-- 表达式输入框 -->
        <a-form-item v-if="conditionType === 'expression'" label="条件表达式 (EL)">
          <a-textarea v-model:value="expression" :rows="3" @change="updateExpression" />
          <p class="help-text">例如: ${approved}</p>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive, watch } from 'vue';
import DefaultProps from './DefaultProps.vue';
import { message } from "ant-design-vue";

const props = defineProps({
  selectedElement: { type: Object, required: true },
  modeler: { type: Object, required: true },
  formFields: { type: Array, default: () => [] },
});
const emit = defineEmits(['update']);

// --- 状态定义 ---
const conditionType = ref('none');
const expression = ref('');
// 构建器状态
const builderState = reactive({ field: null, operator: '==', value: '' });
// 【核心新增】审核结果状态和选项
const auditOutcome = ref(null);
const auditOptions = [
  { label: '同意', value: 'approved' },
  { label: '拒绝', value: 'rejected' },
  { label: '打回至发起人', value: 'returnToInitiator' },
  { label: '打回至上一节点', value: 'returnToPrevious' },
];

const isConditionalFlow = computed(() => {
  const source = props.selectedElement?.businessObject?.sourceRef;
  return source && source.$type === 'bpmn:ExclusiveGateway';
});

const formFieldsForSelect = computed(() =>
    props.formFields.map(f => ({ label: `${f.label} (${f.id})`, value: f.id }))
);

const resetAllBuilders = () => {
  builderState.field = null;
  builderState.operator = '==';
  builderState.value = '';
  auditOutcome.value = null;
};

// 【核心新增】一个辅助函数，用于解析审核表达式
const parseAuditExpression = (exprBody) => {
  const auditMatch = exprBody.match(/\$\{taskOutcome\s*==\s*'(.+?)'\}/);
  if (auditMatch) {
    const outcome = auditMatch[1];
    if (auditOptions.some(opt => opt.value === outcome)) {
      return outcome;
    }
  }
  return null;
};

// 监听选中节点的变化，并解析现有条件
watch(() => props.selectedElement, (element) => {
  if (!element) return;

  resetAllBuilders();
  expression.value = '';

  const conditionExpression = element.businessObject.conditionExpression;
  if (conditionExpression && conditionExpression.body) {
    const exprBody = conditionExpression.body;
    expression.value = exprBody;

    // 【核心修改】优先尝试解析为“审核”类型
    const parsedAuditOutcome = parseAuditExpression(exprBody);
    if (parsedAuditOutcome) {
      conditionType.value = 'audit';
      auditOutcome.value = parsedAuditOutcome;
      return;
    }

    // 其次尝试解析为“条件构建器”类型
    const match = exprBody.match(/\$\{\s*([a-zA-Z0-9_]+)\s*([<>=!]+)\s*(.+)\s*}/);
    if (match) {
      const [, field, operator, value] = match;
      if (props.formFields.some(f => f.id === field)) {
        conditionType.value = 'builder';
        builderState.field = field;
        builderState.operator = operator;
        builderState.value = value.trim();
        return;
      }
    }

    // 如果都无法解析，则认为是手动输入的“表达式”类型
    conditionType.value = 'expression';
  } else {
    // 没有条件表达式，则为“无条件”
    conditionType.value = 'none';
  }
}, { immediate: true });


// 【核心新增】当审核结果下拉框变化时，生成表达式并更新
const applyAuditChanges = () => {
  if (auditOutcome.value) {
    expression.value = `\${taskOutcome == '${auditOutcome.value}'}`;
  } else {
    expression.value = '';
  }
  updateExpression();
};

const applyBuilderChanges = () => {
  if (builderState.field && builderState.operator && builderState.value.trim() !== '') {
    expression.value = `\${${builderState.field} ${builderState.operator} ${builderState.value}}`;
  } else {
    expression.value = '';
  }
  updateExpression();
};

const onConditionTypeChange = () => {
  // 切换到“无条件”时，清空所有内容
  if (conditionType.value === 'none') {
    resetAllBuilders();
    expression.value = '';
    updateExpression();
    return;
  }
  // 【核心新增】切换到“审核”时，尝试解析当前表达式
  if (conditionType.value === 'audit') {
    const parsedOutcome = parseAuditExpression(expression.value);
    auditOutcome.value = parsedOutcome; // 如果解析失败，值为null，下拉框会显示placeholder
  }
};

const updateExpression = () => {
  const moddle = props.modeler.get('moddle');
  let conditionExpression;

  if (!expression.value.trim()) {
    conditionExpression = undefined;
  } else {
    conditionExpression = moddle.create('bpmn:FormalExpression', {
      body: expression.value,
    });
  }

  const sourceGateway = props.selectedElement.businessObject.sourceRef;
  const isDefaultFlow = conditionType.value === 'none';

  if (sourceGateway && sourceGateway.default?.id === props.selectedElement.id && !isDefaultFlow) {
    emit('update', { conditionExpression, default: undefined });
    message.warn('已为默认流设置了条件，其“默认”属性已被移除。');
  } else {
    emit('update', { conditionExpression });
  }
};
</script>

<style scoped>
.props-container { padding: 8px; }
.condition-builder {
  border: 1px solid #f0f0f0;
  padding: 12px;
  border-radius: 4px;
}
.help-text {
  font-size: 12px;
  color: #888;
  margin-top: 4px;
}
</style>