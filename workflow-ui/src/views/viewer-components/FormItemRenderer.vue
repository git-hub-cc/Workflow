<template>
  <div v-if="isVisible">
    <!-- 布局容器 -->
    <template v-if="field.type === 'Layout'">
      <a-row :gutter="16">
        <a-col v-for="(col, colIndex) in field.props.columns" :key="colIndex" :span="24 / field.props.columns.length">
          <FormItemRenderer
              v-for="childField in col"
              :key="childField.id"
              :field="childField"
              :form-data="formData"
              :mode="mode"
              @update:form-data="(...args) => emit('update:form-data', ...args)"
          />
        </a-col>
      </a-row>
    </template>

    <!-- 其他组件 -->
    <a-form-item
        v-else
        :label="field.label"
        :name="field.id"
        :rules="mode === 'edit' ? field.rules : []"
    >
      <!-- 编辑模式 -->
      <template v-if="mode === 'edit'">
        <!-- 【新增】为 QuillEditor 创建特例 -->
        <QuillEditor
            v-if="field.type === 'RichText'"
            v-model:content="localValue"
            :placeholder="field.props.placeholder"
            content-type="html"
            theme="snow"
        />
        <!-- 其他通用组件 -->
        <component
            v-else
            :is="getComponentByType(field.type)"
            v-model:value="localValue"
            :placeholder="field.props.placeholder"
            :options="getOptionsForField(field)"
            :columns="field.type === 'Subform' ? getSubformColumns(field) : undefined"
            :data-source="field.type === 'Subform' ? formData[field.id] : undefined"
            @change="field.type === 'Subform' ? handleSubformChange : null"
        />
      </template>

      <!-- 只读模式 -->
      <template v-else>
        <span v-if="!['FileUpload', 'RichText', 'Subform'].includes(field.type)">{{ formattedValue }}</span>
        <!-- 只读文件列表 -->
        <div v-if="field.type === 'FileUpload' && localValue && localValue.length > 0">
          <p v-for="file in localValue" :key="file.id">
            <a @click.prevent="handleDownload(file.id, file.originalFilename)" href="#">
              <PaperClipOutlined /> {{ file.originalFilename }}
            </a>
          </p>
        </div>
        <!-- 只读富文本 -->
        <div v-if="field.type === 'RichText'" v-html="localValue" class="readonly-richtext"></div>
        <!-- 只读子表单 -->
        <a-table
            v-if="field.type === 'Subform'"
            :columns="getSubformColumns(field)"
            :data-source="localValue"
            :pagination="false"
            bordered
            size="small"
        />
      </template>
    </a-form-item>
  </div>
</template>

<script setup>
import { computed, defineAsyncComponent, ref } from 'vue';
import { useUserStore } from '@/stores/user';
import { downloadFile } from '@/api';
import { message } from 'ant-design-vue';
import { PaperClipOutlined } from '@ant-design/icons-vue';
import FileUploader from '@/components/FileUploader.vue';
// 【修改】引入 QuillEditor 及其样式，移除 tinymce
import { QuillEditor } from '@vueup/vue-quill';
import '@vueup/vue-quill/dist/vue-quill.snow.css';

const props = defineProps(['field', 'formData', 'mode']);
const emit = defineEmits(['update:form-data']);
const userStore = useUserStore();

const FormItemRenderer = defineAsyncComponent(() => import('./FormItemRenderer.vue'));

const localValue = computed({
  get: () => props.formData[props.field.id],
  set: (value) => emit('update:form-data', props.field.id, value),
});

const isVisible = computed(() => {
  const visibility = props.field.visibility;
  if (!visibility || !visibility.enabled) return true;

  const checkRule = (rule) => {
    const targetValue = props.formData[rule.fieldId];
    if (targetValue === undefined) return false;
    switch (rule.operator) {
      case '==': return targetValue == rule.value;
      case '!=': return targetValue != rule.value;
      case '>': return targetValue > rule.value;
      case '<': return targetValue < rule.value;
      default: return false;
    }
  };

  if (visibility.condition === 'AND') {
    return visibility.rules.every(checkRule);
  } else {
    return visibility.rules.some(checkRule);
  }
});


const getComponentByType = (type) => {
  const map = {
    Input: 'a-input',
    Textarea: 'a-textarea',
    Select: 'a-select',
    Checkbox: 'a-checkbox',
    DatePicker: 'a-date-picker',
    UserPicker: 'a-select',
    FileUpload: FileUploader,
    // 【修改】RichText 不再由通用 component 处理
    Subform: 'a-table',
  };
  return map[type] || 'a-input';
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

const formattedValue = computed(() => {
  const value = localValue.value;
  if (value === null || value === undefined) return '(未填写)';

  if (props.field.type === 'DatePicker' && value) {
    return new Date(value).toLocaleString();
  }
  if (props.field.type === 'UserPicker' && value) {
    const user = userStore.allUsers.find(u => u.id === value);
    return user ? `${user.name} (${user.id})` : value;
  }
  if (typeof value === 'boolean') return value ? '是' : '否';
  return value;
});

const getSubformColumns = (field) => {
  return field.props.columns.map(col => ({
    title: col.label,
    dataIndex: col.id,
    key: col.id,
  }));
};

const handleDownload = async (fileId, filename) => {
  try {
    const response = await downloadFile(fileId);
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', filename);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  } catch (error) {
    message.error('文件下载失败');
  }
};
</script>

<style>
.readonly-richtext img {
  max-width: 100%;
  height: auto;
}

/* 【新增】Quill 编辑器高度样式 */
.ql-container {
  min-height: 150px;
}
</style>