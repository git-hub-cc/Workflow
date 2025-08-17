<template>
  <div>
    <!-- Generic Properties -->
    <GenericProps :field="field" :all-fields="allFields" />

    <a-divider>数据选择器配置</a-divider>

    <!-- Modal Title -->
    <a-form-item label="弹窗标题">
      <a-input v-model:value="field.props.modalTitle" />
    </a-form-item>

    <!-- Data URL -->
    <a-form-item label="数据接口URL" help="接口需支持分页(page, size)和搜索(search)参数">
      <a-input v-model:value="field.props.dataUrl" />
    </a-form-item>

    <!-- Table Columns Configuration -->
    <a-form-item label="弹窗表格列">
      <div v-for="(col, index) in field.props.columns" :key="index" class="config-row">
        <a-input v-model:value="col.title" placeholder="列标题 (Title)" />
        <a-input v-model:value="col.dataIndex" placeholder="数据字段 (DataIndex)" />
        <a-button type="text" danger @click="removeColumn(index)">
          <DeleteOutlined />
        </a-button>
      </div>
      <a-button type="dashed" block @click="addColumn">
        <PlusOutlined /> 添加列
      </a-button>
    </a-form-item>

    <!-- Field Mappings Configuration -->
    <a-form-item label="回填字段映射" help="将选中行的源字段值，回填到当前表单的目标字段中">
      <div v-for="(mapping, index) in field.props.mappings" :key="index" class="config-row">
        <a-input v-model:value="mapping.sourceField" placeholder="源字段 (Source)" />
        <a-select v-model:value="mapping.targetField" placeholder="目标字段 (Target)">
          <a-select-option v-for="f in availableTargetFields" :key="f.id" :value="f.id">
            {{ f.label }} ({{ f.id }})
          </a-select-option>
        </a-select>
        <a-button type="text" danger @click="removeMapping(index)">
          <DeleteOutlined />
        </a-button>
      </div>
      <a-button type="dashed" block @click="addMapping">
        <PlusOutlined /> 添加映射
      </a-button>
    </a-form-item>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue';
import { flattenFields } from '@/views/viewer-components/formUtils';
import GenericProps from './GenericProps.vue'; // Re-use generic part

const props = defineProps(['field', 'allFields']);

const availableTargetFields = computed(() => {
  return flattenFields(props.allFields).filter(f => !['GridRow', 'GridCol', 'DataPicker', 'Subform'].includes(f.type));
});

// Columns management
const addColumn = () => {
  if (!props.field.props.columns) props.field.props.columns = [];
  props.field.props.columns.push({ title: '', dataIndex: '' });
};
const removeColumn = (index) => {
  props.field.props.columns.splice(index, 1);
};

// Mappings management
const addMapping = () => {
  if (!props.field.props.mappings) props.field.props.mappings = [];
  props.field.props.mappings.push({ sourceField: '', targetField: '' });
};
const removeMapping = (index) => {
  props.field.props.mappings.splice(index, 1);
};
</script>

<style scoped>
.config-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}
</style>