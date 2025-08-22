<template>
  <div>
    <!-- 引入通用属性配置 -->
    <GenericProps :field="field" :all-fields="allFields" />

    <a-divider>子表单列配置</a-divider>
    <div v-for="(col, index) in field.props.columns" :key="col.id" class="subform-column">
      <a-input v-model:value="col.label" placeholder="列标题" style="flex: 1;" />
      <!-- 【核心修改】允许选择更多组件类型 -->
      <a-select v-model:value="col.type" style="width: 150px;">
        <a-select-option value="Input">单行文本</a-select-option>
        <a-select-option value="DatePicker">日期选择</a-select-option>
        <a-select-option value="UserPicker">人员选择器</a-select-option>
        <a-select-option value="DataPicker">数据选择器</a-select-option>
      </a-select>
      <a-button type="text" danger @click="removeColumn(index)"><DeleteOutlined /></a-button>
    </div>
    <a-button type="dashed" block @click="addColumn"><PlusOutlined /> 添加列</a-button>
  </div>
</template>

<script setup>
import { v4 as uuidv4 } from 'uuid';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue';
import GenericProps from './GenericProps.vue';

const props = defineProps({
  field: {
    type: Object,
    required: true,
  },
  allFields: {
    type: Array,
    required: true,
  },
});

const addColumn = () => {
  const newColumn = {
    id: `col_${uuidv4().substring(0, 4)}`,
    label: `新列${props.field.props.columns.length + 1}`,
    type: 'Input',
    // 【核心新增】为新列预置 props 对象，以便后续配置
    props: {}
  };

  // 如果是数据选择器，则提供默认配置
  if (newColumn.type === 'DataPicker') {
    newColumn.props = {
      modalTitle: '选择数据',
      dataUrl: '/workflows/users',
      columns: [{ title: 'ID', dataIndex: 'id' }, { title: '姓名', dataIndex: 'name' }],
      mappings: [{ sourceField: 'name', targetField: newColumn.id }]
    };
  }

  props.field.props.columns.push(newColumn);
};

const removeColumn = (index) => {
  props.field.props.columns.splice(index, 1);
};
</script>

<style scoped>
.subform-column {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}
</style>