<template>
  <div class="listeners-container">
    <a-divider>{{ title }}</a-divider>
    <div v-for="(listener, index) in localListeners" :key="index" class="listener-card">
      <div class="listener-header">
        <!-- 【修复】添加 @change 事件 -->
        <a-select v-model:value="listener.event" placeholder="事件类型" style="width: 120px;" @change="updateParent">
          <a-select-option v-for="event in eventTypes" :key="event" :value="event">{{ event }}</a-select-option>
        </a-select>
        <a-button type="text" danger @click="removeListener(index)"><DeleteOutlined /></a-button>
      </div>
      <div class="listener-body">
        <!-- 【修复】添加 @change 事件 -->
        <a-select v-model:value="listener.listenerType" placeholder="监听器类型" style="margin-bottom: 8px;" @change="updateParent">
          <a-select-option value="delegateExpression">代理表达式</a-select-option>
          <a-select-option value="class">Java 类</a-select-option>
          <a-select-option value="expression">表达式</a-select-option>
        </a-select>

        <!-- 【修复】添加 @change 事件 -->
        <a-select
            v-if="listener.listenerType === 'delegateExpression'"
            v-model:value="listener.value"
            show-search
            placeholder="选择或输入 Bean 名称"
            :options="availableBeans"
            @change="updateParent"
        />
        <!-- 【修复】添加 @change 事件 -->
        <a-input v-else v-model:value="listener.value" placeholder="输入类路径或表达式" @change="updateParent" />

        <!-- 字段注入 (仅对 Delegate Expression 和 Java Class 类型有意义) -->
        <!-- 【修复】修改 @update:fields 的处理器 -->
        <FieldInjection
            v-if="['delegateExpression', 'class'].includes(listener.listenerType)"
            :fields="listener.fields"
            @update:fields="newFields => { listener.fields = newFields; updateParent(); }"
        />
      </div>
    </div>
    <a-button type="dashed" block @click="addListener"><PlusOutlined /> 添加监听器</a-button>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue';
import { getAvailableBeans } from '@/api';
import FieldInjection from './FieldInjection.vue';

const props = defineProps({
  listeners: { type: Array, default: () => [] },
  title: { type: String, required: true },
  eventTypes: { type: Array, required: true },
});
const emit = defineEmits(['update:listeners']);

const localListeners = ref([]);
const availableBeans = ref([]);

// 解析从BPMN模型传入的监听器
const parseListeners = () => {
  if (props.listeners && props.listeners.length > 0) {
    localListeners.value = props.listeners.map(l => {
      let listenerType = '';
      let value = '';
      if (l.delegateExpression) {
        listenerType = 'delegateExpression';
        value = l.delegateExpression;
      } else if (l.class) {
        listenerType = 'class';
        value = l.class;
      } else if (l.expression) {
        listenerType = 'expression';
        value = l.expression;
      }
      return {
        event: l.event,
        listenerType,
        value,
        fields: l.fields ? [...l.fields] : [],
      };
    });
  } else {
    localListeners.value = [];
  }
};

watch(() => props.listeners, parseListeners, { immediate: true, deep: true });

onMounted(async () => {
  try {
    const beans = await getAvailableBeans({ type: 'listener' });
    availableBeans.value = beans.map(b => ({ label: b.name, value: `\${${b.name}}` }));
  } catch (e) {
    console.error("Failed to fetch available listener beans", e);
  }
});

const addListener = () => {
  localListeners.value.push({
    event: props.eventTypes[0],
    listenerType: 'delegateExpression',
    value: '',
    fields: [],
  });
  updateParent();
};

const removeListener = (index) => {
  localListeners.value.splice(index, 1);
  updateParent();
};

// 当本地监听器列表变化时，转换为BPMN格式并通知父组件
const updateParent = () => {
  const bpmnListeners = localListeners.value.map(l => {
    const bpmnListener = {
      event: l.event,
      fields: l.fields.length > 0 ? l.fields : undefined,
    };
    if (l.listenerType === 'delegateExpression') bpmnListener.delegateExpression = l.value;
    if (l.listenerType === 'class') bpmnListener.class = l.value;
    if (l.listenerType === 'expression') bpmnListener.expression = l.value;
    return bpmnListener;
  });
  emit('update:listeners', bpmnListeners);
};

// 【修复】移除这个导致无限循环的 watch
// watch(localListeners, updateParent, { deep: true });

</script>

<style scoped>
.listeners-container {
  margin-top: 16px;
}
.listener-card {
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 8px;
  margin-bottom: 12px;
}
.listener-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.listener-body {
  display: flex;
  flex-direction: column;
}
</style>