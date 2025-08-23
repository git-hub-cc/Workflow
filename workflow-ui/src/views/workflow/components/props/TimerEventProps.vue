<template>
  <div class="props-container">
    <DefaultProps :selected-element="selectedElement" :modeler="modeler" @update="properties => $emit('update', properties)" />

    <a-divider>定时器配置</a-divider>

    <a-form layout="vertical">
      <a-form-item label="定时器类型">
        <a-radio-group v-model:value="timerType" button-style="solid" size="small">
          <a-radio-button value="timeDuration">持续时间</a-radio-button>
          <a-radio-button value="timeDate">固定日期</a-radio-button>
          <a-radio-button value="timeCycle">周期</a-radio-button>
        </a-radio-group>
      </a-form-item>

      <a-form-item
          :label="timerLabel"
          :help="timerHelp"
      >
        <a-input v-model:value="timerValue" @change="updateTimerDefinition" />
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import DefaultProps from './DefaultProps.vue';

const props = defineProps({
  selectedElement: { type: Object, required: true },
  // 【核心新增】
  modeler: { type: Object, required: true },
});
const emit = defineEmits(['update']);

const timerType = ref('timeDuration');
const timerValue = ref('');

const timerLabel = computed(() => {
  const labels = {
    timeDuration: '持续时间 (ISO 8601)',
    timeDate: '固定日期 (ISO 8601)',
    timeCycle: '周期 (ISO 8601)',
  };
  return labels[timerType.value];
});

const timerHelp = computed(() => {
  const helps = {
    timeDuration: '例如: PT5M (5分钟), P2D (2天)',
    timeDate: '例如: 2025-12-31T23:59:59Z',
    timeCycle: '例如: R5/PT10S (每10秒重复, 共5次)',
  };
  return helps[timerType.value];
});

watch(() => props.selectedElement, (element) => {
  if (!element) return;
  const timerDefinition = element.businessObject.eventDefinitions?.[0];

  if (timerDefinition) {
    if (timerDefinition.timeDuration) {
      timerType.value = 'timeDuration';
      timerValue.value = timerDefinition.timeDuration.body;
    } else if (timerDefinition.timeDate) {
      timerType.value = 'timeDate';
      timerValue.value = timerDefinition.timeDate.body;
    } else if (timerDefinition.timeCycle) {
      timerType.value = 'timeCycle';
      timerValue.value = timerDefinition.timeCycle.body;
    } else {
      timerType.value = 'timeDuration';
      timerValue.value = '';
    }
  } else {
    timerType.value = 'timeDuration';
    timerValue.value = '';
  }
}, { immediate: true });

const updateTimerDefinition = () => {
  // 【核心修复】
  const moddle = props.modeler.get('moddle');
  const bpmnFactory = props.modeler.get('bpmnFactory');

  const newProps = {
    timeDuration: undefined,
    timeDate: undefined,
    timeCycle: undefined,
  };

  if (timerValue.value) {
    newProps[timerType.value] = moddle.create('bpmn:FormalExpression', { body: timerValue.value });
  }

  const newTimerEventDefinition = bpmnFactory.create('bpmn:TimerEventDefinition', newProps);

  emit('update', { eventDefinitions: [newTimerEventDefinition] });
};

watch(timerType, () => {
  // 当类型切换时，清空值并更新模型
  timerValue.value = '';
  updateTimerDefinition();
});
</script>

<style scoped>
.props-container { padding: 8px; }
.help-text { font-size: 12px; color: #888; margin-top: 4px; }
</style>