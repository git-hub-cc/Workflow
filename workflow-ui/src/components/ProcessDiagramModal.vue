<template>
  <a-modal
      :open="open"
      title="流程图"
      :width="'90%'"
      :footer="null"
      destroyOnClose
      @update:open="(val) => $emit('update:open', val)"
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
.modal-diagram-container {
  height: 75vh;
}

/* 确保 ProcessDiagramViewer 在模态框内能正确填充高度 */
.modal-diagram-container :deep(.diagram-container) {
  height: 100%;
}
</style>