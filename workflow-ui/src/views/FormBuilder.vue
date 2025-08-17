<template>
  <div class="page-container">
    <a-page-header title="表单设计器" @back="() => $router.push('/')">
      <template #extra>
        <a-button @click="$router.push('/')">取消</a-button>
        <a-button type="primary" @click="saveForm" :loading="saving">保存表单</a-button>
      </template>
    </a-page-header>

    <a-row :gutter="16" style="padding: 24px;">
      <a-col :span="24">
        <a-form-item label="表单名称" :label-col="{ span: 2 }" :wrapper-col="{ span: 10 }">
          <a-input v-model:value="formDefinition.name" placeholder="例如：请假申请单" />
        </a-form-item>
      </a-col>
    </a-row>

    <div class="builder-container">
      <!-- 左侧: 组件面板 -->
      <div class="palette">
        <a-card title="基础组件" size="small">
          <div
              v-for="item in paletteItems.basic"
              :key="item.type"
              class="palette-item"
              draggable="true"
              @dragstart="handleDragStart(item)"
          >
            {{ item.label }}
          </div>
        </a-card>
        <a-card title="高级组件" size="small" style="margin-top: 16px;">
          <div
              v-for="item in paletteItems.advanced"
              :key="item.type"
              class="palette-item"
              draggable="true"
              @dragstart="handleDragStart(item)"
          >
            {{ item.label }}
          </div>
        </a-card>
      </div>

      <!-- 中间: 画布 -->
      <!-- 【修复 1】: 明确 drop 事件处理，使用 .prevent.stop 并传递事件对象 -->
      <div class="canvas" @dragover.prevent @drop.prevent.stop="handleCanvasDrop($event)">
        <a-form layout="vertical">
          <div v-if="formDefinition.schema.fields.length === 0" class="canvas-placeholder">
            从左侧拖拽组件到这里
          </div>
          <!-- 【修复 2】: 监听子组件发出的 'component-dropped' 事件 -->
          <draggable-item
              v-for="(field, index) in formDefinition.schema.fields"
              :key="field.id"
              :field="field"
              :index="index"
              :fields="formDefinition.schema.fields"
              :selected-field-id="selectedFieldId"
              @select="selectField"
              @delete="deleteField"
              @component-dropped="handleComponentDrop"
          />
        </a-form>
      </div>

      <!-- 右侧: 属性配置 -->
      <properties-panel
          :selected-field="selectedField"
          :all-fields="formDefinition.schema.fields"
          @update:field="handleFieldUpdate"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { createForm } from '@/api';
import { v4 as uuidv4 } from 'uuid';
import DraggableItem from './builder-components/DraggableItem.vue';
import PropertiesPanel from './builder-components/PropertiesPanel.vue';

const router = useRouter();
const saving = ref(false);

const formDefinition = reactive({
  name: '',
  schema: {
    fields: [],
  },
});

const selectedFieldId = ref(null);

const paletteItems = {
  basic: [
    { type: 'Input', label: '单行文本' }, { type: 'Textarea', label: '多行文本' },
    { type: 'Select', label: '下拉选择' }, { type: 'Checkbox', label: '复选框' },
    { type: 'DatePicker', label: '日期选择' }, { type: 'UserPicker', label: '人员选择器' },
  ],
  advanced: [
    { type: 'FileUpload', label: '文件上传' }, { type: 'RichText', label: '富文本编辑器' },
    { type: 'Subform', label: '子表单/明细' }, { type: 'Layout', label: '布局容器' },
  ]
};

let draggedItem = null;

const handleDragStart = (item) => {
  draggedItem = item;
};

// 【修复 3】: 新增 handleCanvasDrop，专门处理在主画布空白区域的 drop
const handleCanvasDrop = (event) => {
  // 检查是否是从组件面板拖拽过来的新组件
  if (draggedItem) {
    // 直接在根列表末尾添加
    handleDrop(formDefinition.schema.fields);
  }
};

// 【修复 4】: 统一的 drop 逻辑处理器
const handleDrop = (targetList, index = -1) => {
  if (draggedItem) {
    const newField = createNewField(draggedItem.type);
    if (index === -1) {
      targetList.push(newField);
    } else {
      targetList.splice(index, 0, newField);
    }
    selectField(newField);
    draggedItem = null;
  }
};

// 【修复 5】: 创建一个新的处理器来接收子组件的 drop 事件
const handleComponentDrop = ({ targetList, index }) => {
  handleDrop(targetList, index);
};


const createNewField = (type) => {
  const fieldId = type === 'UserPicker' ? 'nextAssignee' : `${type.toLowerCase()}_${uuidv4().substring(0, 4)}`;
  const baseField = {
    id: fieldId, type: type,
    label: `${paletteItems.basic.find(p => p.type === type)?.label || paletteItems.advanced.find(p => p.type === type)?.label}`,
    props: { placeholder: '' },
    rules: [{ required: false, message: '此项为必填项' }],
    visibility: { enabled: false, condition: 'AND', rules: [] },
  };
  switch (type) {
    case 'Select': baseField.props.options = ['选项1', '选项2', '选项3']; break;
    case 'UserPicker': baseField.label = '下一步审批人'; baseField.rules[0].required = true; break;
    case 'FileUpload': baseField.props.multiple = true; baseField.props.maxCount = 5; break;
    case 'Subform':
      baseField.props.columns = [
        { id: `col_${uuidv4().substring(0, 4)}`, label: '列1', type: 'Input' },
        { id: `col_${uuidv4().substring(0, 4)}`, label: '列2', type: 'Input' },
      ];
      break;
    case 'Layout': baseField.props.columns = [[], []]; baseField.label = '布局容器'; break;
  }
  return baseField;
};

const selectField = (field) => { selectedFieldId.value = field?.id || null; };
const deleteField = (index, list) => {
  if (selectedFieldId.value === list[index].id) { selectedFieldId.value = null; }
  list.splice(index, 1);
};

const selectedField = computed(() => {
  function findField(fields, id) {
    for (const field of fields) {
      if (field.id === id) return field;
      if (field.type === 'Layout') {
        for (const col of field.props.columns) {
          const found = findField(col, id);
          if (found) return found;
        }
      }
    }
    return null;
  }
  return findField(formDefinition.schema.fields, selectedFieldId.value);
});

function findAndReplaceField(fields, updatedField) {
  for (let i = 0; i < fields.length; i++) {
    const field = fields[i];
    if (field.id === updatedField.id) { fields[i] = updatedField; return true; }
    if (field.type === 'Layout') {
      for (const col of field.props.columns) {
        if (findAndReplaceField(col, updatedField)) { return true; }
      }
    }
  }
  return false;
}
const handleFieldUpdate = (updatedField) => {
  if (!selectedField.value || !updatedField) return;
  findAndReplaceField(formDefinition.schema.fields, updatedField);
};

const saveForm = async () => {
  if (!formDefinition.name.trim()) { message.error('请输入表单名称！'); return; }
  if (formDefinition.schema.fields.length === 0) { message.error('表单不能为空，请至少添加一个组件！'); return; }
  saving.value = true;
  try {
    const payload = { name: formDefinition.name, schemaJson: JSON.stringify(formDefinition.schema) };
    await createForm(payload);
    message.success('表单保存成功！');
    router.push('/');
  } catch (error) { message.error('保存失败，请重试。'); } finally { saving.value = false; }
};
</script>

<style scoped>
.builder-container { display: flex; gap: 16px; padding: 0 24px 24px; }
.palette { width: 220px; flex-shrink: 0; }
.canvas { flex-grow: 1; border: 1px dashed #d9d9d9; padding: 16px; background: #fafafa; min-height: 600px; overflow-y: auto; }
.palette-item { padding: 8px 12px; margin-bottom: 8px; background: #f7f7f7; border: 1px solid #d9d9d9; border-radius: 4px; cursor: grab; text-align: center; }
.canvas-placeholder { text-align: center; color: #aaa; padding: 64px 0; }
</style>