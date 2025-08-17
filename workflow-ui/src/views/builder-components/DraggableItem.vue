<template>
  <div
      :class="['form-field', { selected: selectedFieldId === field.id }]"
      @click.stop="selectField(field)"
      draggable="true"
      @dragstart.stop="handleDragStart($event)"
      @dragover.prevent.stop="handleDragOver"
      @dragleave.stop="handleDragLeave"
      @drop.prevent.stop="handleDropOnItem($event)"
  >
    <!-- 渲染普通组件 -->
    <template v-if="field.type !== 'Layout'">
      <a-form-item :label="field.label" :required="field.rules[0]?.required">
        <component
            :is="getComponentByType(field.type)"
            :placeholder="field.props.placeholder"
            :options="getOptionsForField(field)"
            disabled
            style="pointer-events: none;"
        />
      </a-form-item>
    </template>

    <!-- 渲染布局容器 -->
    <template v-else>
      <div class="layout-container">
        <a-row :gutter="16">
          <a-col v-for="(col, colIndex) in field.props.columns" :key="colIndex" :span="24 / field.props.columns.length">
            <!-- 【修复】: 明确传递 $event 和目标列 col -->
            <div
                class="layout-column-dropzone"
                @dragover.prevent.stop
                @drop.prevent.stop="handleDropOnColumn($event, col)"
            >
              <draggable-item
                  v-for="(childField, childIndex) in col"
                  :key="childField.id"
                  :field="childField"
                  :index="childIndex"
                  :fields="col"
                  :selected-field-id="selectedFieldId"
                  @select="selectField"
                  @delete="(...args) => emit('delete', ...args)"
                  @component-dropped="(payload) => emit('component-dropped', payload)"
              />
              <div v-if="col.length === 0" class="column-placeholder">拖拽组件到此列</div>
            </div>
          </a-col>
        </a-row>
      </div>
    </template>

    <div class="field-actions">
      <a-button type="text" size="small" @click.stop="deleteField(index, fields)" danger>删除</a-button>
    </div>
  </div>
</template>

<script setup>
import { defineAsyncComponent } from 'vue';
import { useUserStore } from '@/stores/user';
const userStore = useUserStore();

const props = defineProps(['field', 'index', 'fields', 'selectedFieldId']);
// 【修复】: 更改 emit 的事件名称，使其更具描述性
const emit = defineEmits(['select', 'delete', 'component-dropped']);

const DraggableItem = defineAsyncComponent(() => import('./DraggableItem.vue'));

const getComponentByType = (type) => {
  const map = {
    Input: 'a-input', Textarea: 'a-textarea', Select: 'a-select', Checkbox: 'a-checkbox',
    DatePicker: 'a-date-picker', UserPicker: 'a-select', FileUpload: 'a-upload',
    RichText: 'div', Subform: 'a-table',
  };
  return map[type] || 'a-input';
};

const getOptionsForField = (field) => {
  if (field.type === 'Select') return field.props.options.map(o => ({ label: o, value: o }));
  if (field.type === 'UserPicker') return userStore.allUsers.map(u => ({ label: `${u.name} (${u.id})`, value: u.id }));
  return undefined;
};

const selectField = (field) => emit('select', field);
const deleteField = (index, list) => emit('delete', index, list);


// 【修复】: 拖拽开始时，将当前字段ID存入 dataTransfer，用于“移动”操作
const handleDragStart = (e) => {
  e.dataTransfer.setData('text/plain', JSON.stringify({ sourceFieldId: props.field.id }));
  e.dataTransfer.effectAllowed = 'move';
};

const handleDragOver = (e) => { e.currentTarget.classList.add('drag-over'); };
const handleDragLeave = (e) => { e.currentTarget.classList.remove('drag-over'); };


// 【修复】: 当有组件在此组件上释放时，触发事件，表示要插入到此组件“之前”
const handleDropOnItem = (event) => {
  event.currentTarget.classList.remove('drag-over');
  // 向上级（FormBuilder）发出事件，告知需要在当前组件的位置插入新组件
  emit('component-dropped', { event, targetList: props.fields, index: props.index });
};

// 【修复】: 当有组件在布局列中释放时，触发事件，表示要插入到该列的“末尾”
const handleDropOnColumn = (event, targetColumn) => {
  emit('component-dropped', { event, targetList: targetColumn, index: targetColumn.length });
};

</script>

<style scoped>
.form-field { padding: 12px; border: 1px solid transparent; margin-bottom: 8px; cursor: pointer; position: relative; background: white; }
.form-field:hover { border-color: #d9d9d9; }
.form-field.selected { border: 1px solid #1890ff; background: #e6f7ff; }
.field-actions { position: absolute; top: -8px; right: 0; display: none; }
.form-field:hover .field-actions { display: block; }
.layout-container { padding: 16px; border: 1px dashed #b4b4b4; background-color: #f6f7f9; }
.layout-column-dropzone { min-height: 100px; border: 1px dashed #cccccc; padding: 8px; background-color: white; height: 100%; }
.column-placeholder { text-align: center; color: #aaa; padding: 32px 0; }
.drag-over { border-top: 2px solid #1890ff; }
</style>