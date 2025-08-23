<template>
  <div class="props-container">
    <!-- 引入默认的ID和名称编辑 -->
    <DefaultProps :selected-element="selectedElement" :modeler="modeler" @update="properties => $emit('update', properties)" />

    <a-divider>处理人配置</a-divider>

    <a-form layout="vertical">
      <a-form-item label="分配类型">
        <a-radio-group v-model:value="assignmentType" button-style="solid" size="small">
          <a-radio-button value="assignee">指定处理人</a-radio-button>
          <a-radio-button value="candidateGroups">指定候选组</a-radio-button>
        </a-radio-group>
      </a-form-item>

      <!-- 1. 指定处理人 (Assignee) 的配置 -->
      <div v-if="assignmentType === 'assignee'">
        <a-form-item label="处理人来源">
          <a-select v-model:value="assigneeSource">
            <a-select-opt-group label="系统变量">
              <a-select-option value="initiator">流程发起人</a-select-option>
              <a-select-option value="manager">发起人的上级</a-select-option>
            </a-select-opt-group>
            <a-select-opt-group label="表单变量">
              <a-select-option v-for="field in formUserPickerFields" :key="field.id" :value="field.id">
                {{ field.label }}
              </a-select-option>
            </a-select-opt-group>
          </a-select>
        </a-form-item>
      </div>

      <!-- 2. 指定候选组 (Candidate Groups) 的配置 -->
      <div v-if="assignmentType === 'candidateGroups'">
        <a-form-item label="选择用户组">
          <a-select
              v-model:value="selectedGroups"
              mode="multiple"
              placeholder="请选择一个或多个用户组"
              :options="userGroupsForSelect"
          />
        </a-form-item>
      </div>
    </a-form>

    <!-- 【核心新增】任务监听器配置 -->
    <ListenersProps
        :listeners="taskListeners"
        title="任务监听器"
        :event-types="['create', 'assignment', 'complete', 'delete', 'update', 'timeout']"
        @update:listeners="updateTaskListeners"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import DefaultProps from './DefaultProps.vue';
import ListenersProps from './ListenersProps.vue'; // 引入新组件

const props = defineProps({
  selectedElement: { type: Object, required: true },
  // 【核心新增】接收 modeler 实例
  modeler: { type: Object, required: true },
  formFields: { type: Array, default: () => [] },
  userGroups: { type: Array, default: () => [] },
});
const emit = defineEmits(['update']);

// --- 内部状态 ---
const assignmentType = ref('assignee');
const assigneeSource = ref('initiator'); // 'initiator', 'manager', or a form field ID
const selectedGroups = ref([]);
const taskListeners = ref([]); // 新增状态

// --- 计算属性 ---
const formUserPickerFields = computed(() =>
    props.formFields.filter(f => f.type === 'UserPicker')
);
const userGroupsForSelect = computed(() =>
    props.userGroups.map(g => ({ label: `${g.name} (${g.description})`, value: g.name }))
);

// --- 监听与逻辑 ---
watch(
    () => props.selectedElement,
    (element) => {
      // --- 非用户任务直接跳过 ---
      if (!element || element.type !== 'bpmn:UserTask') return;

      const bo = element.businessObject;

      // =============================
      // 处理人配置初始化
      // =============================
      const assignee = bo['camunda:assignee'] || bo.assignee;
      const candidateGroups = bo['camunda:candidateGroups'] || bo.candidateGroups;

      if (assignee) {
        assignmentType.value = 'assignee';

        switch (assignee) {
          case '${initiator}':
            assigneeSource.value = 'initiator';
            break;
          case '${managerId}':
            assigneeSource.value = 'manager';
            break;
          default: {
            // 匹配 ${fieldId} 形式
            const match = assignee.match(/\$\{(.+?)}/);
            if (match && props.formFields.some((f) => f.id === match[1])) {
              assigneeSource.value = match[1];
            } else {
              assigneeSource.value = 'initiator';
            }
          }
        }
      } else if (candidateGroups) {
        assignmentType.value = 'candidateGroups';
        selectedGroups.value = candidateGroups
            .split(',')
            .map((g) => g.trim())
            .filter(Boolean);
      } else {
        // 默认初始化
        assignmentType.value = 'assignee';
        assigneeSource.value = 'initiator';
        selectedGroups.value = [];
      }

      // =============================
      // 监听器初始化
      // =============================
      const extensionElements = bo.extensionElements?.values ?? [];
      taskListeners.value = extensionElements.filter(
          (e) => e.$type === 'camunda:TaskListener'
      );
    },
    { immediate: true }
);

// 【核心修改】将所有属性更新合并到一个 watch 中
watch([assignmentType, assigneeSource, selectedGroups], () => {
  updateProperties();
}, { deep: true });

// --- 【核心新增】更新监听器的方法 ---
const updateTaskListeners = (newListeners) => {
  taskListeners.value = newListeners;
  updateProperties(); // 属性变化时，统一调用更新方法
};

// 【核心修改】统一的属性更新方法
const updateProperties = () => {
  const propertiesToUpdate = {};

  // 1. 处理处理人/候选组
  if (assignmentType.value === 'assignee') {
    propertiesToUpdate['camunda:candidateGroups'] = undefined;
    if (assigneeSource.value === 'initiator') propertiesToUpdate['camunda:assignee'] = '${initiator}';
    else if (assigneeSource.value === 'manager') propertiesToUpdate['camunda:assignee'] = '${managerId}';
    else propertiesToUpdate['camunda:assignee'] = `\${${assigneeSource.value}}`;
  } else if (assignmentType.value === 'candidateGroups') {
    propertiesToUpdate['camunda:assignee'] = undefined;
    if (selectedGroups.value && selectedGroups.value.length > 0) {
      propertiesToUpdate['camunda:candidateGroups'] = selectedGroups.value.join(',');
    } else {
      propertiesToUpdate['camunda:candidateGroups'] = undefined;
    }
  }

  // 2. 处理扩展元素（监听器等）
  // 【核心修复】使用 props.modeler 获取 moddle 实例
  const moddle = props.modeler.get('moddle');
  const otherExtensions = (props.selectedElement.businessObject.extensionElements?.values || [])
      .filter(e => e.$type !== 'camunda:TaskListener');

  const newListeners = taskListeners.value.map(l =>
      moddle.create('camunda:TaskListener', {
        event: l.event,
        delegateExpression: l.delegateExpression,
        class: l.class,
        expression: l.expression,
        fields: l.fields
      })
  );

  const allExtensions = [...otherExtensions, ...newListeners];

  if (allExtensions.length > 0) {
    propertiesToUpdate.extensionElements = moddle.create('bpmn:ExtensionElements', { values: allExtensions });
  } else {
    propertiesToUpdate.extensionElements = undefined;
  }

  emit('update', propertiesToUpdate);
};
</script>

<style scoped>
.props-container {
  padding: 8px;
}
</style>