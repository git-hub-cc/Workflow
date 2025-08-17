<template>
  <div class="page-container">
    <!-- 页面头部，包含标题和操作按钮 -->
    <a-page-header title="表单设计器" @back="() => $router.push('/')">
      <template #extra>
        <a-space>
          <a-button @click="showPreviewModal">
            <template #icon><EyeOutlined /></template>
            预览
          </a-button>

          <!-- 新增 Word 导入功能 -->
          <input type="file" ref="wordFileInputRef" @change="handleWordImport" style="display: none" accept=".docx" />
          <a-button @click="triggerWordImport" :loading="importingWord">
            <template #icon><FileWordOutlined /></template>
            从Word导入
          </a-button>

          <!-- 明确原有导入为 JSON 导入 -->
          <input type="file" ref="jsonFileInputRef" @change="handleJsonImport" style="display: none" accept=".json" />
          <a-button @click="triggerJsonImport">
            <template #icon><UploadOutlined /></template>
            导入 JSON
          </a-button>

          <a-button @click="handleExport">
            <template #icon><DownloadOutlined /></template>
            导出
          </a-button>

          <a-divider type="vertical" />

          <a-button @click="$router.push('/')">取消</a-button>
          <a-button type="primary" @click="saveForm" :loading="saving">保存表单</a-button>
        </a-space>
      </template>
    </a-page-header>

    <!-- 表单名称输入框 -->
    <a-row :gutter="16" style="padding: 24px;">
      <a-col :span="24">
        <a-form-item label="表单名称" :label-col="{ span: 2 }" :wrapper-col="{ span: 10 }">
          <a-input v-model:value="formDefinition.name" placeholder="例如：请假申请单" />
        </a-form-item>
      </a-col>
    </a-row>

    <!-- 设计器主容器 -->
    <div class="builder-container">
      <!-- 左侧: 组件面板 -->
      <div class="palette">
        <!-- 布局组件 -->
        <a-card title="布局组件" size="small">
          <div
              v-for="item in paletteItems.layout"
              :key="item.label"
              class="palette-item"
              draggable="true"
              @dragstart="handleDragStart(item)"
          >
            {{ item.label }}
          </div>
        </a-card>
        <!-- 基础组件 -->
        <a-card title="基础组件" size="small" style="margin-top: 16px;">
          <div
              v-for="item in paletteItems.basic"
              :key="item.label"
              class="palette-item"
              draggable="true"
              @dragstart="handleDragStart(item)"
          >
            {{ item.label }}
          </div>
        </a-card>
        <!-- 高级组件 -->
        <a-card title="高级组件" size="small" style="margin-top: 16px;">
          <div
              v-for="item in paletteItems.advanced"
              :key="item.label"
              class="palette-item"
              draggable="true"
              @dragstart="handleDragStart(item)"
          >
            {{ item.label }}
          </div>
        </a-card>
      </div>

      <!-- 中间: 画布，所有组件的拖放区域 -->
      <div class="canvas" @dragover.prevent @drop.prevent.stop="handleCanvasDrop($event)">
        <a-form layout="vertical">
          <div v-if="formDefinition.schema.fields.length === 0" class="canvas-placeholder">
            从左侧拖拽组件到这里，或从顶部导入Word/JSON文件
          </div>
          <draggable-item
              v-for="(field, index) in formDefinition.schema.fields"
              :key="field.id || index"
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

      <!-- 右侧: 属性配置面板 -->
      <properties-panel
          :selected-field="selectedField"
          :all-fields="formDefinition.schema.fields"
          @update:field="handleFieldUpdate"
      />
    </div>

    <!-- 预览模态框 -->
    <FormPreviewModal
        v-if="previewModalVisible"
        v-model:open="previewModalVisible"
        :form-definition="formDefinition"
    />

  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue';
import { useRouter } from 'vue-router';
import { message, Modal } from 'ant-design-vue';
import { createForm, importFromWord } from '@/api';
import { v4 as uuidv4 } from 'uuid';
import DraggableItem from './builder-components/DraggableItem.vue';
import PropertiesPanel from './builder-components/PropertiesPanel.vue';
import FormPreviewModal from '@/components/FormPreviewModal.vue';
import { EyeOutlined, UploadOutlined, DownloadOutlined, FileWordOutlined } from "@ant-design/icons-vue";

// --- Vue Router 和状态管理 ---
const router = useRouter();
const saving = ref(false);

// --- 核心数据结构: 表单定义 ---
const formDefinition = reactive({
  name: '',
  schema: {
    fields: [], // 存放所有顶级字段和布局
  },
});

// --- 设计器状态 ---
const selectedFieldId = ref(null); // 当前选中的字段或布局的ID
const previewModalVisible = ref(false);

// 区分不同类型的文件输入框
const jsonFileInputRef = ref(null);
const wordFileInputRef = ref(null);
const importingWord = ref(false); // Word导入加载状态

// --- 组件面板定义 ---
const paletteItems = {
  // 布局组件
  layout: [
    { type: 'GridRow', label: '两栏布局', options: { spans: [12, 12] } },
    { type: 'GridRow', label: '三栏布局', options: { spans: [8, 8, 8] } },
    { type: 'GridRow', label: '四栏布局', options: { spans: [6, 6, 6, 6] } },
    { type: 'Collapse', label: '折叠面板' },
    { type: 'DescriptionList', label: '描述列表' },
    { type: 'StaticText', label: '静态文本' }, // 新增静态文本组件
  ],
  // 基础输入组件
  basic: [
    { type: 'Input', label: '单行文本' }, { type: 'Textarea', label: '多行文本' },
    { type: 'Select', label: '下拉选择' }, { type: 'Checkbox', label: '复选框' },
    { type: 'DatePicker', label: '日期选择' }, { type: 'UserPicker', label: '人员选择器' },
    { type: 'TreeSelect', label: '树形选择器' },
  ],
  // 高级业务组件
  advanced: [
    { type: 'FileUpload', label: '文件上传' }, { type: 'RichText', label: '富文本编辑器' },
    { type: 'DataPicker', label: '数据选择器' }, { type: 'Subform', label: '子表单/明细' },
    { type: 'KeyValue', label: '键值对' },
    { type: 'IconPicker', label: '图标选择器' },
  ]
};

let draggedItem = null; // 存储当前正在被拖拽的组件信息

// --- 拖拽与放置 (Drag & Drop) 逻辑 ---

const handleDragStart = (item) => {
  draggedItem = item;
};

const handleCanvasDrop = (event) => {
  const targetList = formDefinition.schema.fields;
  const targetIndex = targetList.length;
  handleDrop(event, targetList, targetIndex);
};

const handleComponentDrop = ({ event, targetList, index }) => {
  handleDrop(event, targetList, index);
};

// 递归查找并移除字段 (用于画布内移动)
function findAndRemoveField(fieldId, list = formDefinition.schema.fields) {
  for (let i = 0; i < list.length; i++) {
    const field = list[i];
    if (field.id === fieldId || field === fieldId) {
      const [removedField] = list.splice(i, 1);
      return removedField;
    }
    if (field.type === 'GridRow' && field.columns) {
      for (const col of field.columns) {
        const found = findAndRemoveField(fieldId, col.fields);
        if (found) return found;
      }
    } else if (field.type === 'Collapse' && field.panels) {
      for (const panel of field.panels) {
        const found = findAndRemoveField(fieldId, panel.fields);
        if (found) return found;
      }
    }
  }
  return null;
}

// 放置操作的核心逻辑
const handleDrop = (event, targetList, index) => {
  // 检查是否为画布内移动操作
  const moveData = event.dataTransfer.getData('text/plain');
  if (moveData) {
    try {
      const { sourceFieldId } = JSON.parse(moveData);
      if (sourceFieldId) {
        const fieldToMove = findAndRemoveField(sourceFieldId);
        if (fieldToMove) {
          targetList.splice(index, 0, fieldToMove);
          selectField(fieldToMove);
        }
        draggedItem = null;
        return;
      }
    } catch (e) { /* 不是移动操作, 忽略 */ }
  }

  // 处理从组件面板拖入的新组件
  if (draggedItem) {
    const newField = createNewField(draggedItem);
    targetList.splice(index, 0, newField);
    selectField(newField);
    draggedItem = null;
  }
};

// --- 创建新字段的初始化逻辑 ---
const createNewField = (item) => {
  const { type, label, options = {} } = item;
  const fieldId = type.toLowerCase() + '_' + uuidv4().substring(0, 4);

  let baseField = {
    id: fieldId,
    type: type,
    label: label,
    props: { placeholder: '' },
    rules: [{ required: false, message: '此项为必填项' }],
    visibility: { enabled: false, condition: 'AND', rules: [] },
  };

  switch (type) {
    case 'GridRow':
      baseField.props.gutter = 16;
      baseField.columns = options.spans.map(span => ({ type: 'GridCol', props: { span }, fields: [] }));
      break;
    case 'Collapse':
      baseField.label = '折叠面板容器';
      baseField.props.accordion = false;
      baseField.panels = [{ id: `panel_${uuidv4().substring(0, 4)}`, type: 'CollapsePanel', props: { header: '面板1' }, fields: [] }];
      break;
    case 'StaticText':
      baseField.label = ''; // 静态文本自身没有 label
      baseField.props.content = '这是一段静态文本';
      baseField.props.tag = 'p'; // 默认为 p 标签
      baseField.rules = []; // 静态文本没有校验规则
      baseField.visibility = undefined; // 静态文本不支持条件显隐
      break;
    case 'Select':
    case 'TreeSelect': // TreeSelect 与 Select 共用数据源结构
      baseField.dataSource = { type: 'static', options: [{ label: '选项1', value: '1' }, { label: '选项2', value: '2' }] };
      if (type === 'TreeSelect') { // 树形选择器的静态数据需要是树形结构
        baseField.dataSource.options = [{ title: '父节点1', value: 'p1', children: [{ title: '子节点1-1', value: 'c1-1' }] }];
      }
      break;
    case 'UserPicker':
      baseField.id = 'nextAssignee';
      baseField.label = '下一步审批人';
      baseField.rules[0].required = true;
      baseField.dataSource = { type: 'system-users' };
      break;
    case 'DataPicker':
      baseField.props.modalTitle = '选择数据';
      baseField.props.dataUrl = '/api/workflows/users'; // 示例URL
      baseField.props.columns = [{ title: 'ID', dataIndex: 'id' }, { title: '姓名', dataIndex: 'name' }];
      baseField.props.mappings = [{ sourceField: 'id', targetField: '' }, { sourceField: 'name', targetField: '' }];
      break;
    case 'FileUpload':
      baseField.props.multiple = true;
      baseField.props.maxCount = 5;
      break;
    case 'Subform':
      baseField.props.columns = [{ id: `col_${uuidv4().substring(0, 4)}`, label: '列1', type: 'Input' }];
      break;
    case 'KeyValue':
      baseField.props.keyPlaceholder = 'Key';
      baseField.props.valuePlaceholder = 'Value';
      break;
    case 'DescriptionList':
      baseField.label = '详情信息';
      baseField.props.column = 3;
      baseField.props.bordered = true;
      baseField.props.size = 'default';
      baseField.props.items = [{ label: '标签一', fieldId: '' }, { label: '标签二', fieldId: '' }];
      baseField.rules = [];
      break;
    case 'IconPicker':
      baseField.label = '图标';
      baseField.props.placeholder = '请选择一个图标';
      baseField.rules = [];
      break;
  }
  return baseField;
};

// --- 字段选择、删除与更新 ---

const selectField = (field) => {
  selectedFieldId.value = field?.id || field || null;
};

const deleteField = (index, list) => {
  if (selectedFieldId.value === list[index].id) {
    selectedFieldId.value = null;
  }
  list.splice(index, 1);
};

const selectedField = computed(() => {
  function findField(fields, id) {
    for (const field of fields) {
      if (field.id === id || field === id) return field;
      if (field.type === 'GridRow' && field.columns) {
        for (const col of field.columns) {
          if (col === id) return col;
          const found = findField(col.fields, id);
          if (found) return found;
        }
      } else if (field.type === 'Collapse' && field.panels) {
        for (const panel of field.panels) {
          if (panel.id === id || panel === id) return panel;
          const found = findField(panel.fields, id);
          if (found) return found;
        }
      }
    }
    return null;
  }
  return findField(formDefinition.schema.fields, selectedFieldId.value);
});

// 递归查找并替换字段 (用于属性面板更新)
function findAndReplaceField(fields, updatedField) {
  for (let i = 0; i < fields.length; i++) {
    let field = fields[i];
    if (field.id === updatedField.id || field === updatedField) {
      fields[i] = updatedField;
      return true;
    }
    if (field.type === 'GridRow') {
      for (let j = 0; j < field.columns.length; j++) {
        if (field.columns[j] === updatedField) {
          field.columns[j] = updatedField;
          return true;
        }
        if (findAndReplaceField(field.columns[j].fields, updatedField)) {
          return true;
        }
      }
    } else if (field.type === 'Collapse') {
      for (let j = 0; j < field.panels.length; j++) {
        if (field.panels[j].id === updatedField.id || field.panels[j] === updatedField) {
          field.panels[j] = updatedField;
          return true;
        }
        if (findAndReplaceField(field.panels[j].fields, updatedField)) {
          return true;
        }
      }
    }
  }
  return false;
}

const handleFieldUpdate = (updatedField) => {
  if (!selectedField.value || !updatedField) return;
  findAndReplaceField(formDefinition.schema.fields, updatedField);
};

// --- 表单保存、预览、导入/导出 ---

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

const showPreviewModal = () => {
  previewModalVisible.value = true;
};

const handleExport = () => {
  if (!formDefinition.name && formDefinition.schema.fields.length === 0) { message.warn('表单为空，无法导出。'); return; }
  try {
    const jsonString = JSON.stringify(formDefinition, null, 2);
    const blob = new Blob([jsonString], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    const filename = (formDefinition.name || 'untitled-form').replace(/[^a-z0-9]/gi, '_').toLowerCase();
    a.download = `${filename}.json`;
    a.href = url;
    document.body.appendChild(a); a.click(); document.body.removeChild(a);
    URL.revokeObjectURL(url);
    message.success('表单定义已导出！');
  } catch (error) { message.error('导出失败！'); console.error('Export failed:', error); }
};

// 重命名原有 JSON 导入逻辑
const triggerJsonImport = () => { jsonFileInputRef.value?.click(); };

const handleJsonImport = (event) => {
  const file = event.target.files[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = (e) => {
    try {
      const importedData = JSON.parse(e.target.result);
      if (typeof importedData.name === 'string' && importedData.schema && Array.isArray(importedData.schema.fields)) {
        formDefinition.name = importedData.name;
        formDefinition.schema = importedData.schema;
        selectedFieldId.value = null;
        message.success(`表单 "${importedData.name}" 导入成功！`);
      } else { throw new Error('无效的表单定义文件格式。'); }
    } catch (error) { message.error(`导入失败：${error.message}`); }
  };
  reader.onerror = () => { message.error('读取文件失败！'); };
  reader.readAsText(file);
  event.target.value = '';
};


// --- 新增 Word 导入逻辑 ---
const triggerWordImport = () => { wordFileInputRef.value?.click(); };

const handleWordImport = async (event) => {
  const file = event.target.files[0];
  if (!file) return;

  Modal.confirm({
    title: '确认导入',
    content: '从Word导入将会覆盖当前画布上的所有内容，确定要继续吗？',
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      importingWord.value = true;
      try {
        const response = await importFromWord(file);
        // 后端返回的 schemaJson 是字符串，需要解析
        const schema = JSON.parse(response.schemaJson);

        formDefinition.name = response.name;
        formDefinition.schema.fields = schema.fields;
        selectedFieldId.value = null;

        message.success(`从 "${file.name}" 导入成功！请检查并调整自动生成的表单。`);
      } catch (error) {
        // 全局错误处理器已显示消息
        console.error("Word import failed:", error);
      } finally {
        importingWord.value = false;
        // 重置 input，以便可以再次选择同一个文件
        event.target.value = '';
      }
    },
    onCancel: () => {
      event.target.value = '';
    }
  });
};

</script>

<style scoped>
.builder-container { display: flex; gap: 16px; padding: 0 24px 24px; }
.palette { width: 220px; flex-shrink: 0; }
.canvas { flex-grow: 1; border: 1px dashed #d9d9d9; padding: 16px; background: #fafafa; min-height: 600px; overflow-y: auto; }
.palette-item { padding: 8px 12px; margin-bottom: 8px; background: #f7f7f7; border: 1px solid #d9d9d9; border-radius: 4px; cursor: grab; text-align: center; }
.canvas-placeholder { text-align: center; color: #aaa; padding: 64px 0; }
</style>