<template>
  <div class="props-container">
    <DefaultProps :selected-element="selectedElement" :modeler="modeler" @update="properties => $emit('update', properties)" />

    <a-divider>实现</a-divider>

    <a-form layout="vertical">
      <a-form-item label="实现方式">
        <a-select v-model:value="implementationType">
          <a-select-option value="delegateExpression">代理表达式</a-select-option>
          <a-select-option value="class">Java 类</a-select-option>
          <a-select-option value="expression">表达式</a-select-option>
          <a-select-option value="external">外部任务</a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item
          v-if="implementationType === 'delegateExpression'"
          label="代理表达式"
          help="选择或输入一个 Spring Bean 名称"
      >
        <a-select v-model:value="implementationValue" show-search :options="availableDelegates" />
      </a-form-item>

      <a-form-item v-if="implementationType === 'class'" label="Java 类">
        <a-input v-model:value="implementationValue" placeholder="e.g., com.example.MyDelegate" />
      </a-form-item>

      <a-form-item v-if="implementationType === 'expression'" label="表达式">
        <a-input v-model:value="implementationValue" placeholder="e.g., ${myBean.myMethod()}" />
      </a-form-item>

      <a-form-item v-if="implementationType === 'external'" label="Topic">
        <a-input v-model:value="implementationValue" placeholder="e.g., payment-processing" />
      </a-form-item>
    </a-form>

    <!-- 字段注入 -->
    <FieldInjection
        v-if="['delegateExpression', 'class'].includes(implementationType)"
        :fields="fields"
        @update:fields="updateFields"
    />

    <!-- 执行监听器 -->
    <ListenersProps
        :listeners="executionListeners"
        title="执行监听器"
        :event-types="['start', 'end']"
        @update:listeners="updateExecutionListeners"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import { getAvailableBeans } from '@/api';
import DefaultProps from './DefaultProps.vue';
import FieldInjection from './FieldInjection.vue';
import ListenersProps from './ListenersProps.vue';

const props = defineProps({
  selectedElement: { type: Object, required: true },
  // 【核心新增】
  modeler: { type: Object, required: true },
});
const emit = defineEmits(['update']);

const implementationType = ref('delegateExpression');
const implementationValue = ref('');
const availableDelegates = ref([]);
const fields = ref([]);
const executionListeners = ref([]);

const getExtensionElements = () => {
  return props.selectedElement.businessObject.extensionElements?.values || [];
};

watch(() => props.selectedElement, (element) => {
  if (!element || element.type !== 'bpmn:ServiceTask') return;
  const bo = element.businessObject;

  // Determine implementation type and value
  if (bo.delegateExpression) {
    implementationType.value = 'delegateExpression';
    implementationValue.value = bo.delegateExpression;
  } else if (bo.class) {
    implementationType.value = 'class';
    implementationValue.value = bo.class;
  } else if (bo.expression) {
    implementationType.value = 'expression';
    implementationValue.value = bo.expression;
  } else if (bo.type === 'external') {
    implementationType.value = 'external';
    implementationValue.value = bo.topic;
  } else {
    implementationType.value = 'delegateExpression';
    implementationValue.value = '';
  }

  // Extract fields and listeners from extensionElements
  const extensionElements = getExtensionElements();
  fields.value = extensionElements.filter(e => e.$type === 'camunda:Field');
  executionListeners.value = extensionElements.filter(e => e.$type === 'camunda:ExecutionListener');

}, { immediate: true });

onMounted(async () => {
  try {
    const beans = await getAvailableBeans({ type: 'delegate' });
    availableDelegates.value = beans.map(b => ({ label: b.name, value: `\${${b.name}}` }));
  } catch (e) { console.error("Failed to fetch beans", e); }
});

const updateProperties = () => {
  const properties = {
    delegateExpression: undefined,
    class: undefined,
    expression: undefined,
    type: undefined,
    topic: undefined,
  };
  properties[implementationType.value] = implementationValue.value;

  if (implementationType.value === 'external') {
    properties.type = 'external';
    properties.topic = implementationValue.value;
  }

  emit('update', properties);
};

const updateFields = (newFields) => {
  fields.value = newFields;
  updateExtensionElements();
};

const updateExecutionListeners = (newListeners) => {
  executionListeners.value = newListeners;
  updateExtensionElements();
};

const updateExtensionElements = () => {
  // 【核心修复】
  const moddle = props.modeler.get('moddle');
  const newElements = [
    ...fields.value.map(f => moddle.create('camunda:Field', { name: f.name, string: f.string, expression: f.expression })),
    ...executionListeners.value.map(l => moddle.create('camunda:ExecutionListener', { event: l.event, delegateExpression: l.delegateExpression, class: l.class, expression: l.expression, fields: l.fields }))
  ];
  const extensionElements = moddle.create('bpmn:ExtensionElements', { values: newElements });
  emit('update', { extensionElements });
};

watch([implementationType, implementationValue], updateProperties);
</script>

<style scoped>
.props-container { padding: 8px; }
</style>