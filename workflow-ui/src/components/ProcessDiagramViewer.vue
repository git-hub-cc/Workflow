<template>
  <div class="diagram-container">
    <div ref="canvasRef" class="canvas"></div>
    <a-spin v-if="loading" class="diagram-loading" />
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue';
import BpmnViewer from 'bpmn-js/lib/Viewer';
import 'bpmn-js/dist/assets/diagram-js.css';
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css';
import { message } from 'ant-design-vue';

const props = defineProps({
  bpmnXml: {
    type: String,
    required: true,
  },
  historyActivities: {
    type: Array,
    default: () => [],
  },
});

const canvasRef = ref(null);
const loading = ref(true);
let viewer = null;

const initViewer = () => {
  if (viewer) {
    viewer.destroy();
  }
  viewer = new BpmnViewer({
    container: canvasRef.value,
  });
};

const renderDiagram = async () => {
  if (!props.bpmnXml || !viewer) return;
  loading.value = true;
  try {
    await viewer.importXML(props.bpmnXml);
    viewer.get('canvas').zoom('fit-viewport');
    highlightActivities(); // 在图表渲染成功后调用高亮
  } catch (error) {
    message.error('加载流程图失败');
    console.error('BPMN Import Error:', error);
  } finally {
    loading.value = false;
  }
};

const highlightActivities = () => {
  if (!viewer || !props.historyActivities || props.historyActivities.length === 0) return;

  const canvas = viewer.get('canvas');
  const elementRegistry = viewer.get('elementRegistry');

  // --- 【修复 3】健壮性改造：在应用新标记前，先清理所有旧的高亮标记 ---
  const allElements = elementRegistry.getAll();
  allElements.forEach(element => {
    if (element && element.id) {
      canvas.removeMarker(element.id, 'highlight-completed');
      canvas.removeMarker(element.id, 'highlight-current');
      canvas.removeMarker(element.id, 'highlight-taken');
    }
  });


  const completedActivities = new Set();
  const completedFlows = new Set();
  let currentActivities = new Set();

  props.historyActivities.forEach(activity => {
    if (!activity || !activity.activityId) return;

    // --- 【修复 1】清理 Activity ID，移除 Camunda 7 可能附加的实例部分 ---
    const cleanedId = activity.activityId.split(':')[0];

    // --- 【修复 2】健壮性改造：在操作前检查元素是否存在于图表中 ---
    const element = elementRegistry.get(cleanedId);
    if (!element) {
      // 在控制台静默忽略，因为某些历史活动（如子流程的容器）在图上没有对应元素是正常的
      // console.warn(`在流程图中未找到历史活动 ID: ${cleanedId}`);
      return;
    }

    if (activity.endTime) {
      if (element.type === 'bpmn:SequenceFlow') {
        completedFlows.add(cleanedId);
      } else {
        completedActivities.add(cleanedId);
      }
    } else {
      currentActivities.add(cleanedId);
    }
  });

  // 应用高亮
  completedActivities.forEach(id => canvas.addMarker(id, 'highlight-completed'));
  completedFlows.forEach(id => canvas.addMarker(id, 'highlight-taken'));
  currentActivities.forEach(id => canvas.addMarker(id, 'highlight-current'));
};


onMounted(() => {
  initViewer();
  if (props.bpmnXml) {
    renderDiagram();
  }
});

onBeforeUnmount(() => {
  if (viewer) {
    viewer.destroy();
    viewer = null;
  }
});

watch(() => props.bpmnXml, (newXml) => {
  if (newXml && canvasRef.value) {
    if (!viewer) initViewer();
    renderDiagram();
  }
}, { immediate: true });


watch(() => props.historyActivities, () => {
  if (viewer && viewer.getDefinitions()) { // 确保图已加载
    highlightActivities();
  }
}, { deep: true });

</script>

<style>
.diagram-container {
  position: relative;
  width: 100%;
  height: 600px;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
}
.canvas {
  width: 100%;
  height: 100%;
}
.diagram-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}
.bjs-powered-by {
  display: none;
}

/* --- Highlighting styles --- */
/* 已完成的节点 */
.highlight-completed .djs-visual > :nth-child(1) {
  fill: #f6ffed !important; /* very light green */
  stroke: #52c41a !important;
}
/* 当前活动的节点 */
.highlight-current .djs-visual > :nth-child(1) {
  fill: #e6f7ff !important; /* very light blue */
  stroke: #1890ff !important;
  stroke-dasharray: 4, 2;
  animation: stroke-dashoffset-animation 1s infinite linear;
}
/* 已走过的路径 */
.highlight-taken .djs-visual > :nth-child(1) {
  stroke: #52c41a !important;
  stroke-width: 2.5px !important;
}

@keyframes stroke-dashoffset-animation {
  from {
    stroke-dashoffset: 6;
  }
  to {
    stroke-dashoffset: 0;
  }
}
</style>