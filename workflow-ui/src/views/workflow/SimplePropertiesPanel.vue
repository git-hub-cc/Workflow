<template>
  <div class="simple-properties-panel">
    <a-card :title="panelTitle" size="small" :bordered="false">
      <!-- 动态加载对应的属性配置组件 -->
      <component
          :is="propertiesComponent"
          :key="selectedElement ? selectedElement.id : 'empty'"
          :selected-element="selectedElement"
          :modeler="modeler"
          :form-fields="formFields"
          :user-groups="userGroups"
          @update="properties => $emit('update', properties)"
      />
    </a-card>
  </div>
</template>

<script setup>
import { computed, defineAsyncComponent } from 'vue';

const props = defineProps({
  selectedElement: {
    type: Object,
    default: null,
  },
  // 【核心新增】接收 modeler 实例
  modeler: {
    type: Object,
    default: null,
  },
  formFields: {
    type: Array,
    default: () => [],
  },
  userGroups: {
    type: Array,
    default: () => [],
  },
});

defineEmits(['update']);

// 根据选中节点的类型，计算应显示的标题
const panelTitle = computed(() => {
  if (!props.selectedElement) return '属性面板';
  const typeMap = {
    'bpmn:Process': '流程属性',
    'bpmn:UserTask': '用户任务',
    'bpmn:ServiceTask': '服务任务',
    'bpmn:SequenceFlow': '顺序流',
    'bpmn:StartEvent': '开始事件',
    'bpmn:EndEvent': '结束事件',
    'bpmn:ExclusiveGateway': '排他网关',
    'bpmn:BoundaryEvent': '边界事件',
  };
  return typeMap[props.selectedElement.type] || '属性配置';
});

// 根据选中节点的类型，动态加载对应的属性配置组件
const propertiesComponent = computed(() => {
  if (!props.selectedElement) {
    return defineAsyncComponent(() => import('./components/props/EmptyProps.vue'));
  }

  const type = props.selectedElement.type;

  // 【核心修改】增加对新节点类型的支持
  switch (type) {
    case 'bpmn:UserTask':
      return defineAsyncComponent(() => import('./components/props/UserTaskProps.vue'));
    case 'bpmn:ServiceTask':
      return defineAsyncComponent(() => import('./components/props/ServiceTaskProps.vue'));
    case 'bpmn:SequenceFlow':
      return defineAsyncComponent(() => import('./components/props/SequenceFlowProps.vue'));
    case 'bpmn:BoundaryEvent': {
      const eventDefinition = props.selectedElement.businessObject.eventDefinitions?.[0];
      if (eventDefinition && eventDefinition.$type === 'bpmn:TimerEventDefinition') {
        return defineAsyncComponent(() => import('./components/props/TimerEventProps.vue'));
      }
      return defineAsyncComponent(() => import('./components/props/DefaultProps.vue'));
    }
    case 'bpmn:Process':
    case 'bpmn:StartEvent':
    case 'bpmn:EndEvent':
    case 'bpmn:ExclusiveGateway':
      return defineAsyncComponent(() => import('./components/props/DefaultProps.vue'));
    default:
      return defineAsyncComponent(() => import('./components/props/EmptyProps.vue'));
  }
});
</script>

<style scoped>
.simple-properties-panel {
  height: 100%;
}
.simple-properties-panel :deep(.ant-card) {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.simple-properties-panel :deep(.ant-card-head) {
  flex-shrink: 0;
}
.simple-properties-panel :deep(.ant-card-body) {
  flex-grow: 1;
  overflow-y: auto;
  padding: 12px;
}
</style>