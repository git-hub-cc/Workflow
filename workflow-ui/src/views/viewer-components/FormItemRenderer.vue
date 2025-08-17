<template>
  <!-- 将 IconPickerModal 移至顶层，避免 FormItem 收集错误 -->
  <IconPickerModal v-if="field.type === 'IconPicker'" v-model:open="showIconPickerModal" @select="icon => localValue = icon" />

  <!-- 条件渲染：只有在满足显隐条件时才渲染组件 -->
  <div v-if="isVisible">
    <!-- 1. 布局组件的统一处理 -->
    <template v-if="['GridRow', 'Collapse'].includes(field.type)">
      <a-row v-if="field.type === 'GridRow'" :gutter="field.props.gutter">
        <a-col v-for="(col, colIndex) in field.columns" :key="colIndex" :span="col.props.span">
          <FormItemRenderer v-for="childField in col.fields" :key="childField.id" :field="childField" :form-data="formData" :mode="mode" @update:form-data="(...args) => emit('update:form-data', ...args)" />
        </a-col>
      </a-row>
      <a-collapse v-if="field.type === 'Collapse'" :accordion="field.props.accordion">
        <a-collapse-panel v-for="panel in field.panels" :key="panel.id" :header="panel.props.header">
          <FormItemRenderer v-for="childField in panel.fields" :key="childField.id" :field="childField" :form-data="formData" :mode="mode" @update:form-data="(...args) => emit('update:form-data', ...args)" />
        </a-collapse-panel>
      </a-collapse>
    </template>

    <!-- 2. 描述列表 (纯展示组件) -->
    <a-descriptions
        v-else-if="field.type === 'DescriptionList'"
        :title="field.label"
        :column="field.props.column"
        :size="field.props.size"
        :bordered="field.props.bordered"
    >
      <a-descriptions-item v-for="(item, index) in field.props.items" :key="index" :label="item.label">
        {{ getDescItemValue(item.fieldId) }}
      </a-descriptions-item>
    </a-descriptions>

    <!-- 3. 其他所有可交互组件的渲染 -->
    <a-form-item v-else :label="field.label" :name="field.id" :rules="mode === 'edit' ? field.rules : []">
      <!-- ==================== 编辑模式 (mode === 'edit') ==================== -->
      <template v-if="mode === 'edit'">
        <QuillEditor v-if="field.type === 'RichText'" v-model:content="localValue" :placeholder="field.props.placeholder" content-type="html" theme="snow" />
        <a-select v-else-if="field.type === 'Select'" v-model:value="localValue" :placeholder="field.props.placeholder" :options="dynamicOptions" :loading="loadingOptions" show-search :filter-option="filterOption" />
        <!-- 【修复】UserPicker 之前被误删，现在加回来 -->
        <template v-else-if="field.type === 'UserPicker'">
          <a-select :value="localValue" :placeholder="field.props.placeholder" :options="dynamicOptions" @click.prevent="showUserSelectorModal = true" :open="false">
            <template #suffixIcon><UserOutlined/></template>
          </a-select>
          <UserDeptSelectorModal v-model:open="showUserSelectorModal" @select="handleUserSelect" />
        </template>
        <a-tree-select v-else-if="field.type === 'TreeSelect'" v-model:value="localValue" :placeholder="field.props.placeholder" :tree-data="dynamicOptions" :loading="loadingOptions" tree-default-expand-all show-search tree-node-filter-prop="title" />
        <DataPicker v-else-if="field.type === 'DataPicker'" v-model:value="localValue" :field="field" @update:form-data="(mappings) => handleDataPickerUpdate(mappings)" />
        <EditableSubform v-else-if="field.type === 'Subform'" v-model:value="localValue" :field="field" />
        <KeyValueEditor v-else-if="field.type === 'KeyValue'" v-model:value="localValue" :field-id="field.id" :key-placeholder="field.props.keyPlaceholder" :value-placeholder="field.props.valuePlaceholder" />
        <a-input v-else-if="field.type === 'IconPicker'" :value="localValue" :placeholder="field.props.placeholder" readonly @click="showIconPickerModal = true">
          <template #addonAfter>
            <a-button @click="showIconPickerModal = true">
              <component :is="iconMap[localValue] || iconMap['SmileOutlined']" />
            </a-button>
          </template>
        </a-input>
        <component v-else :is="getComponentByType(field.type)" v-model:value="localValue" v-model:checked="localValue" :placeholder="field.props.placeholder" />
      </template>

      <!-- ==================== 只读模式 (mode === 'readonly') ==================== -->
      <template v-else>
        <span v-if="!['FileUpload', 'RichText', 'Subform', 'KeyValue', 'IconPicker'].includes(field.type)">{{ formattedValue }}</span>
        <div v-if="field.type === 'FileUpload' && localValue && localValue.length > 0">
          <p v-for="file in localValue" :key="file.id">
            <a @click.prevent="handleDownload(file.id, file.originalFilename)" href="#"><PaperClipOutlined /> {{ file.originalFilename }}</a>
          </p>
        </div>
        <div v-if="field.type === 'RichText'" v-html="localValue" class="readonly-richtext"></div>
        <a-table v-if="field.type === 'Subform'" :columns="getSubformColumns(field)" :data-source="localValue" :pagination="false" bordered size="small" />
        <a-descriptions v-if="field.type === 'KeyValue'" :column="1" size="small" bordered>
          <a-descriptions-item v-for="(item, idx) in localValue" :key="idx" :label="item.key">{{ item.value }}</a-descriptions-item>
        </a-descriptions>
        <component v-if="field.type === 'IconPicker' && localValue" :is="iconMap[localValue]" style="font-size: 20px;" />
      </template>
    </a-form-item>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, defineAsyncComponent } from 'vue';
import { useUserStore } from '@/stores/user';
import { downloadFile, fetchTableData, fetchTreeData } from '@/api';
import { message } from 'ant-design-vue';
import { PaperClipOutlined, UserOutlined } from '@ant-design/icons-vue';
import FileUploader from '@/components/FileUploader.vue';
import DataPicker from './DataPicker.vue';
import { QuillEditor } from '@vueup/vue-quill';
import '@vueup/vue-quill/dist/vue-quill.snow.css';
import { iconMap } from '@/utils/iconLibrary.js';

import EditableSubform from './EditableSubform.vue';
import UserDeptSelectorModal from '@/components/UserDeptSelectorModal.vue';
import KeyValueEditor from './KeyValueEditor.vue';
import IconPickerModal from '@/components/IconPickerModal.vue';
const FormItemRenderer = defineAsyncComponent(() => import('./FormItemRenderer.vue'));


const props = defineProps(['field', 'formData', 'mode']);
const emit = defineEmits(['update:form-data']);

const userStore = useUserStore();
const dynamicOptions = ref([]);
const loadingOptions = ref(false);
const showUserSelectorModal = ref(false);
const showIconPickerModal = ref(false);

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
      case '>': return Number(targetValue) > Number(rule.value);
      case '<': return Number(targetValue) < Number(rule.value);
      default: return false;
    }
  };
  return visibility.condition === 'AND' ? visibility.rules.every(checkRule) : visibility.rules.some(checkRule);
});

const formattedValue = computed(() => {
  const value = localValue.value;
  if (value === null || value === undefined || value === '') return '(未填写)';

  if (['Select', 'TreeSelect'].includes(props.field.type)) {
    const findOptionLabel = (options, val) => {
      for (const opt of options) {
        if (opt.value === val) return opt.label || opt.title;
        if (opt.children) {
          const found = findOptionLabel(opt.children, val);
          if (found) return found;
        }
      }
      return null;
    };
    return findOptionLabel(dynamicOptions.value, value) || value;
  }
  if (props.field.type === 'UserPicker') {
    let user = dynamicOptions.value.find(u => u.value === value);
    if (!user) user = userStore.allUsers.find(u => u.id === value);
    return user ? (user.label || `${user.name} (${user.id})`) : value;
  }
  if (props.field.type === 'DatePicker' && value) { try { return new Date(value).toLocaleString(); } catch(e) { return value; } }
  if (props.field.type === 'DataPicker') {
    if (props.field.props.mappings && props.field.props.mappings.length > 0) {
      const displayFieldId = props.field.props.mappings[0].targetField;
      if (displayFieldId && props.formData[displayFieldId]) {
        return props.formData[displayFieldId];
      }
    }
    return value;
  }
  if (typeof value === 'boolean') return value ? '是' : '否';
  return value;
});

onMounted(async () => {
  const ds = props.field.dataSource;
  if (!ds || !['Select', 'UserPicker', 'TreeSelect'].includes(props.field.type)) return;

  loadingOptions.value = true;
  try {
    if (ds.type === 'static') {
      dynamicOptions.value = ds.options || [];
    } else if (ds.type === 'system-users') {
      if (userStore.allUsers.length === 0) await userStore.fetchAllUsers();
      dynamicOptions.value = userStore.allUsers.map(u => ({ label: `${u.name} (${u.id})`, value: u.id }));
    } else if (ds.type === 'api' && ds.url) {
      const data = await fetchTableData(ds.url);
      dynamicOptions.value = data.map(item => ({ label: item[ds.labelKey || 'label'], value: item[ds.valueKey || 'value'] }));
    } else if (ds.type === 'api-tree' && ds.source) {
      dynamicOptions.value = await fetchTreeData(ds.source);
    }
  } catch (error) {
    message.error(`加载字段 "${props.field.label}" 的选项失败`);
  } finally {
    loadingOptions.value = false;
  }
});

const getComponentByType = (type) => {
  const map = { Input: 'a-input', Textarea: 'a-textarea', Checkbox: 'a-checkbox', DatePicker: 'a-date-picker', FileUpload: FileUploader };
  return map[type] || 'a-input';
};
const filterOption = (input, option) => option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0;
const getSubformColumns = (field) => field.props.columns.map(col => ({ title: col.label, dataIndex: col.id, key: col.id }));
const getDescItemValue = (fieldId) => {
  if (!fieldId) return '(未关联)';
  const value = props.formData[fieldId];
  if (value === null || value === undefined || value === '') return '(空)';
  if (typeof value === 'object') return JSON.stringify(value);
  return value;
};
const handleDownload = async (fileId, filename) => {
  try {
    const response = await downloadFile(fileId);
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url; link.setAttribute('download', filename);
    document.body.appendChild(link); link.click(); document.body.removeChild(link);
  } catch (error) { message.error('文件下载失败'); }
};
const handleDataPickerUpdate = (mappings) => {
  for (const key in mappings) { emit('update:form-data', key, mappings[key]); }
};
const handleUserSelect = (user) => {
  localValue.value = user.id;
  const exists = dynamicOptions.value.some(opt => opt.value === user.id);
  if (!exists) { dynamicOptions.value.push({ label: `${user.name} (${user.id})`, value: user.id }); }
};
</script>

<style>
.readonly-richtext img { max-width: 100%; height: auto; }
.ql-container { min-height: 150px; }
</style>