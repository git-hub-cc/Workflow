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
              v-for="item in paletteItems"
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
      <div
          class="canvas"
          @dragover.prevent
          @drop="handleDrop"
      >
        <a-form layout="vertical">
          <div
              v-if="formDefinition.schema.fields.length === 0"
              class="canvas-placeholder"
          >
            从左侧拖拽组件到这里
          </div>
          <div
              v-for="(field, index) in formDefinition.schema.fields"
              :key="field.id"
              :class="['form-field', { selected: selectedFieldId === field.id }]"
              @click="selectField(field)"
          >
            <a-form-item
                :label="field.label"
                :required="field.rules[0]?.required"
            >
              <component
                  :is="getComponentByType(field.type)"
                  :placeholder="field.props.placeholder"
                  :options="getOptionsForField(field)"
                  disabled
              />
            </a-form-item>
            <div class="field-actions">
              <a-button type="text" size="small" @click.stop="deleteField(index)" danger>删除</a-button>
            </div>
          </div>
        </a-form>
      </div>

      <!-- 右侧: 属性配置 -->
      <div class="properties">
        <a-card title="属性配置" size="small">
          <div v-if="!selectedField" class="properties-placeholder">
            选中一个组件以编辑其属性
          </div>
          <a-form v-else layout="vertical">
            <a-form-item label="字段ID">
              <a-input :value="selectedField.id" disabled />
            </a-form-item>
            <a-form-item label="标题 (Label)">
              <a-input v-model:value="selectedField.label" />
            </a-form-item>
            <a-form-item label="占位提示 (Placeholder)">
              <a-input v-model:value="selectedField.props.placeholder" />
            </a-form-item>
            <a-form-item>
              <a-checkbox v-model:checked="selectedField.rules[0].required">
                是否必填
              </a-checkbox>
            </a-form-item>
            <!-- 特定于 Select 的属性 -->
            <a-form-item v-if="selectedField.type === 'Select'" label="选项 (每行一个)">
              <a-textarea v-model:value="optionsText" :rows="4" />
            </a-form-item>
            <!-- 特定于 UserPicker 的属性 -->
            <a-form-item v-if="selectedField.type === 'UserPicker'" label="提示">
              <a-typography-text type="secondary">
                此组件将从系统中所有用户列表进行选择。
              </a-typography-text>
            </a-form-item>
          </a-form>
        </a-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { createForm } from '@/api';
import { useUserStore } from '@/stores/user';

import {
  Input as AInput,
  Textarea as ATextarea,
  Select as ASelect,
  Checkbox as ACheckbox,
  DatePicker as ADatePicker,
} from 'ant-design-vue';


const router = useRouter();
const userStore = useUserStore();
const saving = ref(false);

const formDefinition = reactive({
  name: '',
  schema: {
    fields: [],
  },
});

let fieldCounter = 0;
const selectedFieldId = ref(null);

const paletteItems = [
  { type: 'Input', label: '单行文本' },
  { type: 'Textarea', label: '多行文本' },
  { type: 'Select', label: '下拉选择' },
  { type: 'Checkbox', label: '复选框' },
  { type: 'DatePicker', label: '日期选择' },
  { type: 'UserPicker', label: '人员选择器' },
];

let draggedItem = null;

const handleDragStart = (item) => {
  draggedItem = item;
};

const handleDrop = () => {
  if (draggedItem) {
    addNewField(draggedItem.type);
    draggedItem = null;
  }
};

const addNewField = (type) => {
  const baseId = type === 'UserPicker' ? 'nextAssignee' : `field_${fieldCounter++}`;
  const fieldId = formDefinition.schema.fields.some(f => f.id === baseId) ? `${baseId}_${fieldCounter++}` : baseId;

  const newField = {
    id: fieldId, // 如果是人员选择器，约定ID为 nextAssignee
    type: type,
    label: `${paletteItems.find(p => p.type === type).label}`,
    props: { placeholder: '' },
    rules: [{ required: false, message: `请选择${paletteItems.find(p => p.type === type).label}` }],
  };

  if (type === 'Select') {
    newField.props.options = ['选项1', '选项2', '选项3'];
  }
  if (type === 'UserPicker') {
    newField.label = '下一步审批人';
    newField.rules[0].required = true; // 人员选择器默认为必填
  }

  formDefinition.schema.fields.push(newField);
  selectField(newField);
};

const selectField = (field) => {
  selectedFieldId.value = field.id;
};

const deleteField = (index) => {
  if (selectedFieldId.value === formDefinition.schema.fields[index].id) {
    selectedFieldId.value = null;
  }
  formDefinition.schema.fields.splice(index, 1);
};

const selectedField = computed(() => {
  return formDefinition.schema.fields.find(f => f.id === selectedFieldId.value);
});

// 特殊处理 Select 选项的 computed 属性
const optionsText = computed({
  get() {
    if (selectedField.value?.type === 'Select') {
      return selectedField.value.props.options.join('\n');
    }
    return '';
  },
  set(value) {
    if (selectedField.value?.type === 'Select') {
      selectedField.value.props.options = value.split('\n').filter(opt => opt.trim());
    }
  }
});

const getComponentByType = (type) => {
  const map = {
    Input: AInput,
    Textarea: ATextarea,
    Select: ASelect,
    Checkbox: ACheckbox,
    DatePicker: ADatePicker,
    UserPicker: ASelect,
  };
  return map[type] || AInput;
};

const getOptionsForField = (field) => {
  if (field.type === 'Select') {
    return field.props.options.map(o => ({ label: o, value: o }));
  }
  if (field.type === 'UserPicker') {
    return userStore.allUsers.map(u => ({ label: `${u.name} (${u.id})`, value: u.id }));
  }
  return undefined;
};


const saveForm = async () => {
  if (!formDefinition.name.trim()) {
    message.error('请输入表单名称！');
    return;
  }
  if (formDefinition.schema.fields.length === 0) {
    message.error('表单不能为空，请至少添加一个组件！');
    return;
  }

  saving.value = true;
  try {
    const payload = {
      name: formDefinition.name,
      schemaJson: JSON.stringify(formDefinition.schema),
    };
    await createForm(payload);
    message.success('表单保存成功！');
    router.push('/');
  } catch (error) {
    message.error('保存失败，请重试。');
  } finally {
    saving.value = false;
  }
};
</script>

<style scoped>
.builder-container {
  display: flex;
  gap: 16px;
  padding: 0 24px 24px;
}
.palette { width: 220px; }
.canvas { flex-grow: 1; border: 1px dashed #d9d9d9; padding: 16px; background: #fafafa; min-height: 500px; }
.properties { width: 300px; }
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
.form-field {
  padding: 12px;
  border: 1px solid transparent;
  margin-bottom: 8px;
  cursor: pointer;
  position: relative;
}
.form-field:hover {
  border-color: #d9d9d9;
}
.form-field.selected {
  border: 1px solid #1890ff;
  background: #e6f7ff;
}
.properties-placeholder {
  color: #aaa;
  text-align: center;
  padding-top: 24px;
}
.field-actions {
  position: absolute;
  top: -8px;
  right: 0;
  display: none;
}
.form-field:hover .field-actions {
  display: block;
}
</style>