<template>
  <!-- 1. 普通组件 -->
  <div v-if="!isLayoutComponent(field.type)"
       :class="['form-field', { selected: selectedFieldId === field.id }]"
       @click.stop="selectField(field)"
       draggable="true"
       @dragstart.stop="handleDragStart($event)"
       @dragover.prevent.stop="handleDragOver"
       @dragleave.stop="handleDragLeave"
       @drop.prevent.stop="handleDropOnItem($event)"
  >
    <a-form-item :label="field.label" :required="isRequired">
      <component
          :is="getComponentByType(field.type)"
          :placeholder="field.props.placeholder"
      :options="getOptionsForField(field)"
      :tree-data="getOptionsForField(field)"
      disabled
      style="pointer-events: none;"
      >
      <!-- 特殊组件的静态预览 -->
      <template v-if="field.type === 'DataPicker'">
        <a-input-search placeholder="点击选择" disabled>
          <template #enterButton><a-button disabled>选择</a-button></template>
        </a-input-search>
      </template>
      <template v-if="field.type === 'IconPicker'">
        <a-input placeholder="点击选择图标" disabled>
          <template #addonAfter><a-button disabled><SmileOutlined /></a-button></template>
        </a-input>
      </template>
      <template v-if="field.type === 'KeyValue'">
        <div class="kv-preview">键值对编辑器</div>
      </template>
      </component>
    </a-form-item>

    <div class="field-actions">
      <a-button type="text" size="small" @click.stop="deleteField(index, fields)" danger>删除</a-button>
    </div>
  </div>

  <!-- 2. 布局组件渲染逻辑 -->
  <div v-else
       :class="['layout-container', getLayoutComponentClass(field.type), { selected: selectedFieldId === field.id }]"
       @click.stop="selectField(field)"
       draggable="true"
       @dragstart.stop="handleDragStart($event)"
       @dragover.prevent.stop="handleDragOver"
       @dragleave.stop="handleDragLeave"
       @drop.prevent.stop="handleDropOnItem($event)"
  >
    <!-- 2.1 栅格行 (GridRow) -->
    <a-row v-if="field.type === 'GridRow'" :gutter="field.props.gutter">
      <a-col v-for="(col, colIndex) in field.columns" :key="colIndex" :span="col.props.span">
        <div :class="['layout-dropzone', { selected: selectedFieldId === col }]" @click.stop="selectField(col)" @dragover.prevent.stop @drop.prevent.stop="handleDropInContainer($event, col.fields)">
          <DraggableItem v-for="(childField, childIndex) in col.fields" :key="childField.id" :field="childField" :index="childIndex" :fields="col.fields" :selected-field-id="selectedFieldId" @select="selectField" @delete="(...args) => emit('delete', ...args)" @component-dropped="(payload) => emit('component-dropped', payload)" />
          <div v-if="!col.fields || col.fields.length === 0" class="dropzone-placeholder">拖拽组件到此列</div>
        </div>
      </a-col>
    </a-row>

    <!-- 2.2 折叠面板 (Collapse) -->
    <a-collapse v-if="field.type === 'Collapse'" :accordion="field.props.accordion" :activeKey="alwaysOpenKey">
      <a-collapse-panel v-for="panel in field.panels" :key="panel.id" :header="panel.props.header">
        <div :class="['layout-dropzone', { selected: selectedFieldId === panel.id }]" @click.stop="selectField(panel)" @dragover.prevent.stop @drop.prevent.stop="handleDropInContainer($event, panel.fields)">
          <DraggableItem v-for="(childField, childIndex) in panel.fields" :key="childField.id" :field="childField" :index="childIndex" :fields="panel.fields" :selected-field-id="selectedFieldId" @select="selectField" @delete="(...args) => emit('delete', ...args)" @component-dropped="(payload) => emit('component-dropped', payload)" />
          <div v-if="!panel.fields || panel.fields.length === 0" class="dropzone-placeholder">拖拽组件到此面板</div>
        </div>
      </a-collapse-panel>
    </a-collapse>

    <!-- 2.3 描述列表 (DescriptionList) -->
    <a-descriptions v-if="field.type === 'DescriptionList'" :title="field.label" :column="field.props.column" :size="field.props.size" :bordered="field.props.bordered">
      <a-descriptions-item v-for="(item, itemIndex) in field.props.items" :key="itemIndex" :label="item.label">
        [关联字段: {{ item.fieldId || '未设置' }}]
      </a-descriptions-item>
    </a-descriptions>

    <div class="field-actions">
      <a-button type="text" size="small" @click.stop="deleteField(index, fields)" danger>删除</a-button>
    </div>
  </div>

</template>

<script setup>
import { computed, defineAsyncComponent } from 'vue';
import { useUserStore } from '@/stores/user';
import { SmileOutlined } from "@ant-design/icons-vue";

const userStore = useUserStore();

const props = defineProps(['field', 'index', 'fields', 'selectedFieldId']);
const emit = defineEmits(['select', 'delete', 'component-dropped']);

const DraggableItem = defineAsyncComponent(() => import('./DraggableItem.vue'));

const alwaysOpenKey = computed(() => (props.field.type === 'Collapse') ? props.field.panels.map(p => p.id) : []);
const isRequired = computed(() => props.field.rules?.some(rule => rule.required));

const isLayoutComponent = (type) => ['GridRow', 'Collapse', 'DescriptionList'].includes(type);
const getLayoutComponentClass = (type) => {
  const map = { GridRow: 'grid-row-container', Collapse: 'collapse-container', DescriptionList: 'desc-list-container' };
  return map[type] || '';
};

const getComponentByType = (type) => {
  const map = {
    Input: 'a-input', Textarea: 'a-textarea', Select: 'a-select', Checkbox: 'a-checkbox',
    DatePicker: 'a-date-picker', UserPicker: 'a-select', FileUpload: 'a-upload',
    RichText: 'div', Subform: 'a-table', TreeSelect: 'a-tree-select',
  };
  return map[type] || 'a-input';
};

const getOptionsForField = (field) => {
  if (field.dataSource?.type === 'static') return field.dataSource.options;
  if (field.dataSource?.type === 'system-users') return userStore.allUsers.map(u => ({ label: `${u.name} (${u.id})`, value: u.id }));
  return undefined;
};

const selectField = (field) => emit('select', field);
const deleteField = (index, list) => emit('delete', index, list);

const handleDragStart = (e) => {
  e.dataTransfer.setData('text/plain', JSON.stringify({ sourceFieldId: props.field.id || props.field }));
  e.dataTransfer.effectAllowed = 'move';
};

const handleDragOver = (e) => { e.currentTarget.classList.add('drag-over'); };
const handleDragLeave = (e) => { e.currentTarget.classList.remove('drag-over'); };

const handleDropOnItem = (event) => {
  event.currentTarget.classList.remove('drag-over');
  emit('component-dropped', { event, targetList: props.fields, index: props.index });
};

const handleDropInContainer = (event, targetContainerFields) => {
  emit('component-dropped', { event, targetList: targetContainerFields, index: targetContainerFields.length });
};
</script>

<style scoped>
.form-field { padding: 12px; border: 1px solid transparent; margin-bottom: 8px; cursor: pointer; position: relative; background: white; }
.form-field:hover { border-color: #d9d9d9; }
.form-field.selected { border: 1px solid #1890ff; background: #e6f7ff; }

.field-actions { position: absolute; top: -8px; right: 0; display: none; z-index: 10; }
.form-field:hover .field-actions, .layout-container:hover .field-actions { display: block; }

.layout-container { border: 1px dashed #b4b4b4; position: relative; margin-bottom: 8px; cursor: pointer; padding: 16px; }
.layout-container.selected { border-color: #1890ff; box-shadow: 0 0 5px rgba(24, 144, 255, 0.5); }
.grid-row-container { background-color: #f6f7f9; padding: 16px 8px; }
.collapse-container { background-color: #f9f9f9; padding: 8px; }
.desc-list-container { background-color: #fafafa; }

.layout-dropzone { min-height: 100px; border: 1px dashed #cccccc; padding: 8px; background-color: white; height: 100%; }
.layout-dropzone.selected { border-color: #1890ff; background-color: #e6f7ff; }
.dropzone-placeholder { text-align: center; color: #aaa; padding: 32px 0; }

.drag-over::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 2px; background-color: #1890ff; z-index: 1; }

.collapse-container :deep(.ant-collapse-content-box) { padding: 8px !important; }
.collapse-container :deep(.ant-collapse) { background-color: #fff; }

.kv-preview { padding: 8px; border: 1px dashed #d9d9d9; text-align: center; color: #888; background: #fafafa; }
</style>