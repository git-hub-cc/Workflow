<template>
  <!-- 将 IconPickerModal 和 UserDeptSelectorModal 移至顶层，避免 FormItem 收集错误 -->
  <IconPickerModal v-if="field.type === 'IconPicker'" v-model:open="showIconPickerModal" @select="icon => localValue = icon" />

  <!-- 条件渲染：只有在满足显隐条件时才渲染组件 -->
  <div v-if="isVisible">
    <StaticTextRenderer v-if="field.type === 'StaticText'" :field="field" />
    <!-- 【核心新增】分割线渲染 -->
    <a-divider
        v-else-if="field.type === 'Divider'"
        :dashed="field.props.dashed"
        :orientation="field.props.orientation"
    >{{ field.props.text }}</a-divider>

    <template v-else-if="['GridRow', 'Collapse'].includes(field.type)">
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

    <a-form-item v-else :label="field.label" :name="field.id" :rules="mode === 'edit' ? dynamicRules : []">
      <template v-if="mode === 'edit'">
        <!-- 【核心修改】将 DatePicker 的渲染逻辑独立出来 -->
        <a-range-picker
            v-if="field.type === 'DatePicker' && field.props.pickerMode === 'range'"
            v-model:value="localValue"
            :placeholder="field.props.placeholder"
            style="width: 100%;"
        />
        <a-date-picker
            v-else-if="field.type === 'DatePicker' && field.props.pickerMode === 'multiple'"
            v-model:value="localValue"
            :multiple="true"
            :placeholder="field.props.placeholder"
            style="width: 100%;"
        />
        <FileUploader
            v-else-if="field.type === 'FileUpload'"
            v-model:value="localValue"
            :field="field"
            list-type="picture-card"
        />

        <a-select
            v-else-if="field.type === 'UserPicker'"
            v-model:value="localValue"
            show-search
            :placeholder="field.props.placeholder"
            :options="userOptions"
            :filter-option="false"
            :not-found-content="loadingOptions ? undefined : null"
            @search="handleUserSearch"
        >
          <template v-if="loadingOptions" #notFoundContent>
            <a-spin size="small" />
          </template>
        </a-select>

        <QuillEditor
            v-else-if="field.type === 'RichText'"
            v-model:content="localValue"
            :placeholder="field.props.placeholder"
            :toolbar="richTextToolbarOptions"
            content-type="html"
            theme="snow"
        />
        <a-select v-else-if="field.type === 'Select'" v-model:value="localValue" :placeholder="field.props.placeholder" :options="dynamicOptions" :loading="loadingOptions" show-search :filter-option="filterOption" />
        <a-tree-select v-else-if="field.type === 'TreeSelect'" v-model:value="localValue" :placeholder="field.props.placeholder" :tree-data="dynamicOptions" :loading="loadingOptions" tree-default-expand-all show-search tree-node-filter-prop="title" />
        <DataPicker v-else-if="field.type === 'DataPicker'" v-model:value="localValue" :field="field" :form-data="formData" @update:form-data="(id, val) => emit('update:form-data', id, val)" />
        <EditableSubform v-else-if="field.type === 'Subform'" v-model:value="localValue" :field="field" />
        <KeyValueEditor v-else-if="field.type === 'KeyValue'" v-model:value="localValue" :field-id="field.id" :key-placeholder="field.props.keyPlaceholder" :value-placeholder="field.props.valuePlaceholder" />
        <a-input v-else-if="field.type === 'IconPicker'" :value="localValue" :placeholder="field.props.placeholder" readonly @click="showIconPickerModal = true">
          <template #addonAfter>
            <a-button @click="showIconPickerModal = true">
              <component :is="iconMap[localValue] || iconMap['SmileOutlined']" />
            </a-button>
          </template>
        </a-input>
        <!-- 【核心新增】渲染新组件 -->
        <a-switch
            v-else-if="field.type === 'Switch'"
            v-model:checked="localValue"
            :checked-children="field.props.checkedChildren"
            :un-checked-children="field.props.unCheckedChildren"
        />
        <a-radio-group
            v-else-if="field.type === 'RadioGroup'"
            v-model:value="localValue"
            :options="dynamicOptions"
        />
        <a-slider
            v-else-if="field.type === 'Slider'"
            v-model:value="localValue"
            :min="field.props.min"
            :max="field.props.max"
            :range="field.props.range"
        />
        <a-rate
            v-else-if="field.type === 'Rate'"
            v-model:value="localValue"
            :count="field.props.count"
            :allow-half="field.props.allowHalf"
        />
        <component v-else :is="getComponentByType(field.type)" v-model:value="localValue" v-model:checked="localValue" :placeholder="field.props.placeholder" style="width: 100%;" />
      </template>

      <template v-else>
        <!-- 只读模式的显示 -->
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
import { computed, ref, onMounted, defineAsyncComponent, watch } from 'vue';
import { useUserStore } from '@/stores/user';
import { downloadFile, fetchTableData, fetchTreeData, searchUsersForPicker, getUsersForPicker } from '@/api';
import { message } from 'ant-design-vue';
import { PaperClipOutlined, SmileOutlined } from '@ant-design/icons-vue';
import FileUploader from '@/components/FileUploader.vue';
import DataPicker from './DataPicker.vue';
import { QuillEditor } from '@vueup/vue-quill';
import '@vueup/vue-quill/dist/vue-quill.snow.css';
import { iconMap } from '@/utils/iconLibrary.js';

import EditableSubform from './EditableSubform.vue';
import KeyValueEditor from './KeyValueEditor.vue';
import IconPickerModal from '@/components/IconPickerModal.vue';
import StaticTextRenderer from './StaticTextRenderer.vue';
const FormItemRenderer = defineAsyncComponent(() => import('./FormItemRenderer.vue'));


const props = defineProps(['field', 'formData', 'mode']);
const emit = defineEmits(['update:form-data']);

const userStore = useUserStore();
const dynamicOptions = ref([]);
const loadingOptions = ref(false);
const showIconPickerModal = ref(false);
const userOptions = ref([]);

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

const dynamicRules = computed(() => {
  if (!props.field.rules) return [];

  return props.field.rules.map(ruleConfig => {
    if (ruleConfig.required) {
      return { required: true, message: ruleConfig.message || '此项为必填项' };
    }
    if (ruleConfig.pattern) {
      return { pattern: new RegExp(ruleConfig.pattern), message: ruleConfig.message };
    }
    if (ruleConfig.compareField) {
      const validator = (rule, value) => {
        const targetValue = props.formData[ruleConfig.compareField];
        let isValid = true;
        switch (ruleConfig.compareOperator) {
          case '==': isValid = (value == targetValue); break;
          case '!=': isValid = (value != targetValue); break;
          case '>': isValid = (Number(value) > Number(targetValue)); break;
          case '<': isValid = (Number(value) < Number(targetValue)); break;
        }
        return isValid ? Promise.resolve() : Promise.reject(new Error(ruleConfig.message));
      };
      return { validator, trigger: 'blur' };
    }
    if (ruleConfig.type === 'sum') {
      const validator = (rule, value) => {
        if (!Array.isArray(value) || value.length === 0) {
          return Promise.resolve();
        }
        const sum = value.reduce((acc, row) => acc + (Number(row[ruleConfig.subformColumn]) || 0), 0);
        const mainValue = Number(props.formData[ruleConfig.mainFormField]) || 0;

        let isValid = true;
        switch (ruleConfig.compareOperator) {
          case '==': isValid = (sum === mainValue); break;
          case '>=': isValid = (sum >= mainValue); break;
          case '<=': isValid = (sum <= mainValue); break;
        }
        return isValid ? Promise.resolve() : Promise.reject(new Error(ruleConfig.message));
      };
      return { validator, trigger: 'change' };
    }

    return { ...ruleConfig };
  });
});

const richTextToolbarOptions = computed(() => {
  const config = props.field.props?.toolbarOptions;
  if (!config) return 'minimal';
  const toolbar = [];
  const allOptions = [...(config.basic || []), ...(config.header || []), ...(config.list || []), ...(config.extra || [])];
  if (allOptions.length === 0) return false;
  const mapping = {
    'bold': 'bold', 'italic': 'italic', 'underline': 'underline', 'strike': 'strike',
    'header': { 'header': [1, 2, 3, false] },
    'blockquote': 'blockquote', 'code-block': 'code-block',
    'list-ordered': { 'list': 'ordered' }, 'list-bullet': { 'list': 'bullet' },
    'link': 'link', 'image': 'image', 'clean': 'clean'
  };
  const group = [];
  allOptions.forEach(opt => {
    if (mapping[opt]) group.push(mapping[opt]);
  });
  if (group.length > 0) toolbar.push(group);
  return toolbar;
});


const formattedValue = computed(() => {
  const value = localValue.value;
  if (value === null || value === undefined || value === '') return '(未填写)';

  if (['Select', 'TreeSelect', 'RadioGroup'].includes(props.field.type)) {
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
    const selectedUser = userOptions.value.find(u => u.value === value);
    return selectedUser ? selectedUser.label : value;
  }
  if (props.field.type === 'DatePicker' && value) {
    if (Array.isArray(value)) {
      if (value.length === 2 && props.field.props.pickerMode === 'range') {
        try {
          const start = new Date(value[0]).toLocaleDateString();
          const end = new Date(value[1]).toLocaleDateString();
          return `${start} 至 ${end}`;
        } catch (e) { return value.join(' 至 '); }
      }
      try {
        return value.map(d => new Date(d).toLocaleDateString()).join(', ');
      } catch (e) { return value.join(', '); }
    }
    try { return new Date(value).toLocaleString(); } catch(e) { return value; }
  }
  if (props.field.type === 'DataPicker') {
    if (props.field.props.mappings && props.field.props.mappings.length > 0) {
      const displayFieldId = props.field.props.mappings[0].targetField;
      if (displayFieldId && props.formData[displayFieldId]) {
        return props.formData[displayFieldId];
      }
    }
    return value;
  }
  if (props.field.type === 'Slider' && props.field.props.range) {
    return Array.isArray(value) ? value.join(' - ') : value;
  }
  if (typeof value === 'boolean') return value ? '是' : '否';
  return value;
});

const loadOptions = async (params = {}) => {
  const ds = props.field.dataSource;
  if (!ds) return;

  loadingOptions.value = true;
  try {
    if (ds.type === 'static') {
      dynamicOptions.value = ds.options || [];
    } else if (ds.type === 'api' && ds.url) {
      const data = await fetchTableData(ds.url, params);
      dynamicOptions.value = data.content.map(item => ({ label: item[ds.labelKey || 'label'], value: item[ds.valueKey || 'value'] }));
    } else if (ds.type === 'api-tree' && ds.source) {
      dynamicOptions.value = await fetchTreeData(ds.source);
    } else if (ds.type.startsWith('system-users-')) {
      const userParams = {};
      if (ds.type === 'system-users-dept' && ds.departmentId) userParams.departmentId = ds.departmentId;
      if (ds.type === 'system-users-role' && ds.roleName) userParams.roleName = ds.roleName;
      const data = await getUsersForPicker(userParams);
      userOptions.value = data.content.map(u => ({ label: `${u.name} (${u.id})`, value: u.id }));
    }
  } catch (error) {
    message.error(`加载字段 "${props.field.label}" 的选项失败`);
  } finally {
    loadingOptions.value = false;
  }
};

onMounted(() => {
  const ds = props.field.dataSource;
  if (ds && ds.listensTo) return;
  loadOptions();
});

// 【核心新增】级联逻辑
const parentFieldId = computed(() => props.field.dataSource?.listensTo);
if (parentFieldId.value) {
  watch(() => props.formData[parentFieldId.value], (newParentValue) => {
    // 清空当前字段值和选项
    localValue.value = undefined;
    dynamicOptions.value = [];
    if (newParentValue) {
      const paramName = props.field.dataSource.paramName || 'parentId';
      loadOptions({ [paramName]: newParentValue });
    }
  });
}

let searchTimeout;
const handleUserSearch = (keyword) => {
  const dsType = props.field.dataSource?.type;
  if (dsType !== 'system-users-global') return; // 只对全局搜索模式生效
  clearTimeout(searchTimeout);
  if (!keyword) {
    userOptions.value = [];
    return;
  }
  loadingOptions.value = true;
  searchTimeout = setTimeout(async () => {
    try {
      const results = await searchUsersForPicker(keyword);
      userOptions.value = results.map(u => ({ label: `${u.name} (${u.id})`, value: u.id }));
    } catch (error) {
      userOptions.value = [];
    } finally {
      loadingOptions.value = false;
    }
  }, 300);
};

const getComponentByType = (type) => {
  const map = { Input: 'a-input', Textarea: 'a-textarea', Checkbox: 'a-checkbox', DatePicker: 'a-date-picker', InputNumber: 'a-input-number' };
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
    URL.revokeObjectURL(url);
  } catch (error) { message.error('文件下载失败'); }
};
</script>

<style>
.readonly-richtext img { max-width: 100%; height: auto; }
.ql-container { min-height: 150px; }
</style>