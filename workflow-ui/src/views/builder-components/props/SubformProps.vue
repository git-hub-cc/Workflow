<template>
  <div>
    <a-divider>子表单列配置</a-divider>
    <div v-for="(col, index) in field.props.columns" :key="col.id" class="subform-column">
      <a-input v-model:value="col.label" placeholder="列标题" style="flex: 1;" />
      <a-select v-model:value="col.type" style="width: 120px;">
        <a-select-option value="Input">单行文本</a-select-option>
        <a-select-option value="DatePicker">日期</a-select-option>
        <a-select-option value="Select">下拉框</a-select-option>
      </a-select>
      <a-button type="text" danger @click="removeColumn(index)"><DeleteOutlined /></a-button>
    </div>
    <a-button type="dashed" block @click="addColumn"><PlusOutlined /> 添加列</a-button>
  </div>
</template>

<script setup>
import { v4 as uuidv4 } from 'uuid';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue';
const props = defineProps(['field']);

const addColumn = () => {
  props.field.props.columns.push({
    id: `col_${uuidv4().substring(0, 4)}`,
    label: `新列${props.field.props.columns.length + 1}`,
    type: 'Input'
  });
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