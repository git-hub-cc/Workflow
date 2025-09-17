<template>
  <a-modal
      :open="open"
      title="流程图"
      :width="'90%'"
      :footer="null"
      destroyOnClose
      @update:open="(val) => $emit('update:open', val)"
      wrapClassName="fullscreen-mobile-modal"
  >
    <div class="modal-diagram-container">
      <ProcessDiagramViewer
          v-if="bpmnXml"
          :bpmn-xml="bpmnXml"
          :history-activities="historyActivities"
      />
      <a-empty v-else description="无法加载流程图" />
    </div>
  </a-modal>
</template>

<script setup>
import ProcessDiagramViewer from '@/components/ProcessDiagramViewer.vue';

defineProps({
  open: {
    type: Boolean,
    required: true,
  },
  bpmnXml: {
    type: String,
    default: null,
  },
  historyActivities: {
    type: Array,
    default: () => [],
  },
});

defineEmits(['update:open']);
</script>

<style scoped>
/* 【核心修改】在非移动端，保持原有的75vh高度 */
.modal-diagram-container {
  height: 75vh;
}

.modal-diagram-container :deep(.diagram-container) {
  height: 100%;
}

/* 【核心新增】移动端弹窗全屏化样式 */
@media (max-width: 768px) {
  /* 使用 :global 选择器，因为 Modal 组件被挂载到 body 下，不受 scoped 限制 */
  :global(.fullscreen-mobile-modal) .ant-modal {
    max-width: 100%;
    top: 0;
    padding-bottom: 0;
    margin: 0;
  }
  :global(.fullscreen-mobile-modal) .ant-modal-content {
    display: flex;
    flex-direction: column;
    height: 100vh;
    border-radius: 0;
  }
  :global(.fullscreen-mobile-modal) .ant-modal-body {
    flex: 1;
    overflow: hidden; /* 让内部的 viewer 自己滚动 */
    padding: 0; /* 移除内边距，让画布填满 */
  }
  /* 在移动端全屏模式下，让容器和画布都占满100%高度 */
  .modal-diagram-container {
    height: 100%;
  }
}
</style>