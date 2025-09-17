<template>
  <div class="page-container">
    <a-page-header :title="pageTitle" @back="() => $router.push({ name: 'admin-forms' })">
      <template #extra>
        <a-space>
          <a-button @click="showPreviewModal">
            <template #icon><EyeOutlined /></template>
            <!-- 【核心修改】移动端只显示图标 -->
            <span v-if="!isMobile">预览</span>
          </a-button>

          <!-- 【核心新增】移动端将导入/导出收到一个菜单里 -->
          <a-dropdown v-if="!isMobile">
            <template #overlay>
              <a-menu>
                <a-menu-item key="import-word" @click="triggerWordImport" :loading="importingWord">
                  <FileWordOutlined /> 从Word导入
                </a-menu-item>
                <a-menu-item key="import-json" @click="triggerJsonImport">
                  <UploadOutlined /> 导入 JSON
                </a-menu-item>
                <a-menu-item key="export" @click="handleExport">
                  <DownloadOutlined /> 导出
                </a-menu-item>
              </a-menu>
            </template>
            <a-button>
              导入/导出 <DownOutlined />
            </a-button>
          </a-dropdown>

          <a-divider type="vertical" />

          <a-button @click="$router.push({ name: 'admin-forms' })">取消</a-button>
          <a-button type="primary" @click="saveForm" :loading="saving || loading">保存</a-button>
        </a-space>
      </template>
    </a-page-header>

    <a-spin :spinning="loading" tip="正在加载表单定义...">
      <a-row :gutter="16" style="padding: 16px;">
        <a-col :span="24">
          <a-form-item label="表单名称" :label-col="{ span: isMobile ? 24 : 2 }" :wrapper-col="{ span: isMobile ? 24 : 10 }">
            <a-input v-model:value="formDefinition.name" placeholder="例如：请假申请单" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="builder-container">
        <!-- 左侧: 组件面板 (桌面端) -->
        <div v-if="!isMobile" class="palette">
          <a-card title="布局组件" size="small">
            <div v-for="item in paletteItems.layout" :key="item.label" class="palette-item" draggable="true" @dragstart="handleDragStart(item)">{{ item.label }}</div>
          </a-card>
          <a-card title="基础组件" size="small" style="margin-top: 16px;">
            <div v-for="item in paletteItems.basic" :key="item.label" class="palette-item" draggable="true" @dragstart="handleDragStart(item)">{{ item.label }}</div>
          </a-card>
          <a-card title="高级组件" size="small" style="margin-top: 16px;">
            <div v-for="item in paletteItems.advanced" :key="item.label" class="palette-item" draggable="true" @dragstart="handleDragStart(item)">{{ item.label }}</div>
          </a-card>
        </div>

        <!-- 中间: 画布 -->
        <div class="canvas" @dragover.prevent @drop.prevent.stop="handleCanvasDrop($event)">
          <a-form layout="vertical">
            <div v-if="formDefinition.schema.fields.length === 0" class="canvas-placeholder">
              {{ isMobile ? '点击右下角按钮添加组件' : '从左侧拖拽组件到这里' }}
            </div>
            <DraggableItem
                v-for="(field, index) in formDefinition.schema.fields"
                :key="field.id || index"
                :field="field"
                :index="index"
                :fields="formDefinition.schema.fields"
                :selected-field-id="selectedFieldId"
                :is-mobile="isMobile"
                @select="selectField"
                @delete="deleteField"
                @component-dropped="handleComponentDrop"
                @move="moveField"
            />
          </a-form>
        </div>

        <!-- 右侧: 属性配置面板 (桌面端) -->
        <properties-panel
            v-if="!isMobile"
            :selected-field="selectedField"
            :all-fields="formDefinition.schema.fields"
            @update:field="handleFieldUpdate"
        />
      </div>
    </a-spin>

    <!-- 预览模态框 -->
    <FormPreviewModal
        v-if="previewModalVisible"
        v-model:open="previewModalVisible"
        :form-definition="formDefinition"
    />

    <!-- 【核心新增】移动端悬浮操作按钮 -->
    <div v-if="isMobile" class="mobile-fab-group">
      <a-button type="primary" shape="circle" size="large" @click="paletteDrawerVisible = true">
        <PlusOutlined />
      </a-button>
    </div>

    <!-- 【核心新增】移动端组件面板抽屉 -->
    <a-drawer
        v-if="isMobile"
        v-model:open="paletteDrawerVisible"
        title="添加组件"
        placement="bottom"
        :height="'80%'"
    >
      <a-list item-layout="horizontal">
        <a-list-item>
          <a-tag color="blue">布局组件</a-tag>
          <a-space wrap>
            <a-button v-for="item in paletteItems.layout" :key="item.label" @click="addComponent(item)">{{ item.label }}</a-button>
          </a-space>
        </a-list-item>
        <a-list-item>
          <a-tag color="green">基础组件</a-tag>
          <a-space wrap>
            <a-button v-for="item in paletteItems.basic" :key="item.label" @click="addComponent(item)">{{ item.label }}</a-button>
          </a-space>
        </a-list-item>
        <a-list-item>
          <a-tag color="purple">高级组件</a-tag>
          <a-space wrap>
            <a-button v-for="item in paletteItems.advanced" :key="item.label" @click="addComponent(item)">{{ item.label }}</a-button>
          </a-space>
        </a-list-item>
      </a-list>
    </a-drawer>

    <!-- 【核心新增】移动端属性面板抽屉 -->
    <a-drawer
        v-if="isMobile"
        v-model:open="propertiesDrawerVisible"
        title="属性配置"
        placement="right"
        width="85%"
        :body-style="{ padding: '16px' }"
    >
      <properties-panel
          :selected-field="selectedField"
          :all-fields="formDefinition.schema.fields"
          @update:field="handleFieldUpdate"
      />
    </a-drawer>

    <!-- 隐藏的 input 用于文件导入 -->
    <input type="file" ref="wordFileInputRef" @change="handleWordImport" style="display: none" accept=".docx" />
    <input type="file" ref="jsonFileInputRef" @change="handleJsonImport" style="display: none" accept=".json" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, defineAsyncComponent, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { message, Modal } from 'ant-design-vue';
import { createForm, updateForm, getFormById, importFromWord } from '@/api';
import { v4 as uuidv4 } from 'uuid';
import PropertiesPanel from './builder-components/PropertiesPanel.vue';
const FormPreviewModal = defineAsyncComponent(() => import('@/components/FormPreviewModal.vue'));
import { EyeOutlined, UploadOutlined, DownloadOutlined, FileWordOutlined, DownOutlined, PlusOutlined } from "@ant-design/icons-vue";

const DraggableItem = defineAsyncComponent(() => import('./builder-components/DraggableItem.vue'));

const props = defineProps({
  formId: { type: [String, Number], required: false }
});

const router = useRouter();
const loading = ref(false);
const saving = ref(false);

// --- 【核心新增】响应式断点 ---
const isMobile = ref(window.innerWidth < 768);
const handleResize = () => { isMobile.value = window.innerWidth < 768; };
onMounted(() => window.addEventListener('resize', handleResize));
onBeforeUnmount(() => window.removeEventListener('resize', handleResize));

const isEditMode = computed(() => !!props.formId);
const pageTitle = computed(() => isEditMode.value ? `编辑表单` : '新建表单');

const formDefinition = reactive({
  name: '',
  schema: { fields: [] },
});

onMounted(() => {
  if (isEditMode.value) {
    loading.value = true;
    getFormById(props.formId)
        .then(res => {
          formDefinition.name = res.name;
          formDefinition.schema = JSON.parse(res.schemaJson);
        })
        .catch(err => message.error("加载表单数据失败"))
        .finally(() => { loading.value = false; });
  }
});

const selectedFieldId = ref(null);
const previewModalVisible = ref(false);
const jsonFileInputRef = ref(null);
const wordFileInputRef = ref(null);
const importingWord = ref(false);

// --- 【核心新增】移动端抽屉状态 ---
const paletteDrawerVisible = ref(false);
const propertiesDrawerVisible = ref(false);

const paletteItems = {
  layout: [
    { type: 'GridRow', label: '栅格', options: { spans: [12, 12] } },
    { type: 'Collapse', label: '折叠面板' },
    { type: 'DescriptionList', label: '描述列表' },
    { type: 'StaticText', label: '静态文本' },
    { type: 'Divider', label: '分割线' },
  ],
  basic: [
    { type: 'Input', label: '单行文本' }, { type: 'Textarea', label: '多行文本' },
    { type: 'InputNumber', label: '数字' }, { type: 'DatePicker', label: '日期' },
    { type: 'Select', label: '下拉选择' }, { type: 'RadioGroup', label: '单选框' },
    { type: 'Checkbox', label: '复选框' }, { type: 'Switch', label: '开关' },
    { type: 'UserPicker', label: '人员选择' }, { type: 'TreeSelect', label: '树形选择' },
  ],
  advanced: [
    { type: 'FileUpload', label: '文件上传' }, { type: 'RichText', label: '富文本' },
    { type: 'DataPicker', label: '数据选择' }, { type: 'Subform', label: '子表单' },
    { type: 'KeyValue', label: '键值对' }, { type: 'IconPicker', label: '图标选择' },
    { type: 'Slider', label: '滑块' }, { type: 'Rate', label: '评分' },
  ]
};

let draggedItem = null;

const handleDragStart = (item) => { draggedItem = item; };
const handleCanvasDrop = (event) => {
  const targetList = formDefinition.schema.fields;
  const targetIndex = targetList.length;
  handleDrop(event, targetList, targetIndex);
};
const handleComponentDrop = ({ event, targetList, index }) => { handleDrop(event, targetList, index); };

function findAndRemoveField(fieldId, list = formDefinition.schema.fields) {
  for (let i = 0; i < list.length; i++) {
    const field = list[i];
    if (field.id === fieldId || field === fieldId) {
      return list.splice(i, 1)[0];
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

const handleDrop = (event, targetList, index) => {
  if (isMobile.value) return; // 移动端禁用拖拽
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
    } catch (e) { /* not a move op, ignore */ }
  }
  if (draggedItem) {
    const newField = createNewField(draggedItem);
    targetList.splice(index, 0, newField);
    selectField(newField);
    draggedItem = null;
  }
};

// 【核心新增】移动端点击添加组件
const addComponent = (item) => {
  const newField = createNewField(item);
  let targetList = formDefinition.schema.fields;
  if (selectedField.value) {
    const container = findContainerList(selectedField.value.id);
    if (container) targetList = container;
  }
  targetList.push(newField);
  selectField(newField);
  paletteDrawerVisible.value = false;
};

const createNewField = (item) => {
  const { type, label, options = {} } = item;
  const fieldId = type.toLowerCase() + '_' + uuidv4().substring(0, 4);

  let baseField = {
    id: fieldId, type: type, label: label, props: { placeholder: '' },
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
      baseField.label = ''; baseField.props.content = '这是一段静态文本'; baseField.props.tag = 'p'; baseField.rules = []; baseField.visibility = undefined;
      break;
    case 'Divider':
      baseField.label = ''; baseField.props.dashed = false; baseField.props.orientation = 'center'; baseField.props.text = '分割线'; baseField.rules = []; baseField.visibility = undefined;
      break;
    case 'Select': case 'TreeSelect': case 'RadioGroup':
      baseField.dataSource = { type: 'static', options: [{ label: '选项1', value: '1' }, { label: '选项2', value: '2' }] };
      if (type === 'TreeSelect') { baseField.dataSource.options = [{ title: '父节点1', value: 'p1', children: [{ title: '子节点1-1', value: 'c1-1' }] }]; }
      break;
    case 'UserPicker':
      baseField.id = 'nextAssignee'; baseField.label = '人员选择'; baseField.rules[0].required = true; baseField.dataSource = { type: 'system-users' };
      break;
    case 'InputNumber':
      baseField.props.min = 0; baseField.props.max = 100;
      break;
    case 'DatePicker':
      baseField.props.pickerMode = 'single';
      break;
    case 'Switch':
      baseField.props.checkedChildren = '是'; baseField.props.unCheckedChildren = '否';
      break;
    case 'Slider':
      baseField.props.min = 0; baseField.props.max = 100; baseField.props.range = false;
      break;
    case 'Rate':
      baseField.props.count = 5; baseField.props.allowHalf = false;
      break;
    case 'DataPicker':
      baseField.props.modalTitle = '选择数据'; baseField.props.dataUrl = '/workflows/users';
      baseField.props.columns = [{ title: 'ID', dataIndex: 'id' }, { title: '姓名', dataIndex: 'name' }];
      baseField.props.mappings = [{ sourceField: 'id', targetField: '' }, { sourceField: 'name', targetField: '' }];
      break;
    case 'FileUpload':
      baseField.props.multiple = true; baseField.props.maxCount = 5; baseField.props.maxSize = 10; baseField.props.allowedTypes = '';
      break;
    case 'Subform':
      baseField.props.columns = [{ id: `col_${uuidv4().substring(0, 4)}`, label: '列1', type: 'Input' }];
      baseField.props.summary = { enabled: false, items: [] };
      break;
    case 'KeyValue':
      baseField.props.keyPlaceholder = 'Key'; baseField.props.valuePlaceholder = 'Value';
      break;
    case 'DescriptionList':
      baseField.label = '详情信息'; baseField.props.column = 3; baseField.props.bordered = true; baseField.props.size = 'default';
      baseField.props.items = [{ label: '标签一', fieldId: '' }, { label: '标签二', fieldId: '' }]; baseField.rules = [];
      break;
    case 'IconPicker':
      baseField.label = '图标'; baseField.props.placeholder = '请选择一个图标'; baseField.rules = [];
      break;
  }
  return baseField;
};

// --- 【核心新增】移动端上下移动 ---
const moveField = (direction, index, list) => {
  const targetIndex = direction === 'up' ? index - 1 : index + 1;
  if (targetIndex < 0 || targetIndex >= list.length) return;
  const item = list.splice(index, 1)[0];
  list.splice(targetIndex, 0, item);
};

const selectField = (field) => {
  selectedFieldId.value = field?.id || field || null;
  if (isMobile.value && field) {
    propertiesDrawerVisible.value = true;
  }
};
const deleteField = (index, list) => {
  if (selectedFieldId.value === list[index].id) {
    selectedFieldId.value = null;
    if (isMobile.value) propertiesDrawerVisible.value = false;
  }
  list.splice(index, 1);
};
const findContainerList = (fieldId) => {
  function find(fields) {
    for (const field of fields) {
      if (field.id === fieldId && (field.type === 'GridCol' || field.type === 'CollapsePanel')) {
        return field.fields;
      }
      if (field.type === 'GridRow') {
        for (const col of field.columns) {
          const found = find(col.fields);
          if (found) return found;
          if (col.id === fieldId) return col.fields;
        }
      }
      if (field.type === 'Collapse') {
        for (const panel of field.panels) {
          const found = find(panel.fields);
          if (found) return found;
          if (panel.id === fieldId) return panel.fields;
        }
      }
    }
    return null;
  }
  return find(formDefinition.schema.fields);
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

const saveForm = async () => {
  if (!formDefinition.name.trim()) { message.error('请输入表单名称！'); return; }
  if (formDefinition.schema.fields.length === 0) { message.error('表单不能为空！'); return; }
  saving.value = true;
  try {
    const payload = { name: formDefinition.name, schemaJson: JSON.stringify(formDefinition.schema) };
    if (isEditMode.value) {
      await updateForm(props.formId, payload);
      message.success('表单更新成功！');
    } else {
      await createForm(payload);
      message.success('表单保存成功！');
    }
    router.push({ name: 'admin-forms' });
  } catch (error) {} finally {
    saving.value = false;
  }
};

const showPreviewModal = () => { previewModalVisible.value = true; };

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
const triggerWordImport = () => { wordFileInputRef.value?.click(); };
const handleWordImport = async (event) => {
  const file = event.target.files[0];
  if (!file) return;
  Modal.confirm({
    title: '确认导入',
    content: '从Word导入将会覆盖当前画布上的所有内容，确定要继续吗？',
    onOk: async () => {
      importingWord.value = true;
      try {
        const response = await importFromWord(file);
        const schema = JSON.parse(response.schemaJson);
        formDefinition.name = response.name;
        formDefinition.schema.fields = schema.fields;
        selectedFieldId.value = null;
        message.success(`从 "${file.name}" 导入成功！请检查并调整自动生成的表单。`);
      } catch (error) {
      } finally {
        importingWord.value = false;
        event.target.value = '';
      }
    },
    onCancel: () => { event.target.value = ''; }
  });
};
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.builder-container {
  display: flex;
  gap: 16px;
  padding: 0 16px 16px;
  flex-grow: 1;
  min-height: 0;
}
.palette {
  width: 220px;
  flex-shrink: 0;
}
.canvas {
  flex-grow: 1;
  border: 1px dashed #d9d9d9;
  padding: 16px;
  background: #fafafa;
  overflow-y: auto;
}
.palette-item {
  padding: 8px 12px;
  margin-bottom: 8px;
  background: #f7f7f7;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  cursor: grab;
  text-align: center;
}
.canvas-placeholder {
  text-align: center;
  color: #aaa;
  padding: 64px 0;
}
.mobile-fab-group {
  position: fixed;
  bottom: 80px;
  right: 24px;
  z-index: 100;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>