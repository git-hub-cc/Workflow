<template>
  <div class="props-container">
    <a-form :model="formState" layout="vertical">
      <a-form-item label="ID">
        <a-input v-model:value="formState.id" disabled />
      </a-form-item>
      <a-form-item label="名称">
        <a-input v-model:value="formState.name" @change="updateProperties" />
      </a-form-item>
    </a-form>

    <!-- 【核心新增】执行监听器配置 -->
    <ListenersProps
        :listeners="executionListeners"
        title="执行监听器"
        :event-types="['start', 'end', 'take']"
        @update:listeners="updateExecutionListeners"
    />
  </div>
</template>

<script setup>
import { reactive, watch, ref } from 'vue';
import ListenersProps from './ListenersProps.vue'; // 引入新组件

const props = defineProps({
  selectedElement: { type: Object, required: true },
  // 【核心新增】
  modeler: { type: Object, required: true },
});
const emit = defineEmits(['update']);

const formState = reactive({
  id: '',
  name: '',
});

// 【核心新增】监听器状态
const executionListeners = ref([]);

// 监听选中节点的变化，并初始化表单和监听器
watch(() => props.selectedElement, (element) => {
  if (element) {
    formState.id = element.id;
    formState.name = element.businessObject.name || '';

    // 从 extensionElements 中提取监听器
    const extensionElements = element.businessObject.extensionElements?.values || [];
    executionListeners.value = extensionElements.filter(e => e.$type === 'camunda:ExecutionListener');
  }
}, { immediate: true });

// 当表单项变化时，通知父组件更新BPMN模型
const updateProperties = () => {
  emit('update', { name: formState.name });
};

// 【核心新增】更新监听器
const updateExecutionListeners = (newListeners) => {
  executionListeners.value = newListeners;
  updateExtensionElements();
};

const updateExtensionElements = () => {
  // 【核心修复】
  const moddle = props.modeler.get('moddle');

  // 获取其他类型的扩展元素（如果有的话）
  const otherExtensions = (props.selectedElement.businessObject.extensionElements?.values || [])
      .filter(e => e.$type !== 'camunda:ExecutionListener');

  const newListeners = executionListeners.value.map(l =>
      moddle.create('camunda:ExecutionListener', {
        event: l.event,
        delegateExpression: l.delegateExpression,
        class: l.class,
        expression: l.expression,
        fields: l.fields
      })
  );

  const newElements = [...otherExtensions, ...newListeners];

  let extensionElements;
  if (newElements.length > 0) {
    extensionElements = moddle.create('bpmn:ExtensionElements', { values: newElements });
  } else {
    extensionElements = undefined; // 如果没有扩展元素，则设置为 undefined 以便从模型中移除
  }

  emit('update', { extensionElements });
};
</script>

<style scoped>
.props-container {
  padding: 8px;
}
</style>