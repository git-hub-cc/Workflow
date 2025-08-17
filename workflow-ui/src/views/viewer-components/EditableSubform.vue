<template>
  <div class="editable-subform">
    <a-table
        :columns="tableColumns"
        :data-source="localValue"
        :pagination="false"
        bordered
        size="small"
        row-key="__id"
    >
      <template #bodyCell="{ column, record, index }">
        <template v-if="isEditableColumn(column.key)">
          <a-form-item
              :name="[field.id, index, column.key]"
              :rules="[{ required: true, message: '此项必填' }]"
              class="editable-cell-form-item"
          >
            <component
                :is="getComponentByType(column.type)"
                v-model:value="record[column.key]"
                size="small"
                placeholder="请输入"
                style="width: 100%;"
            />
          </a-form-item>
        </template>
        <template v-else-if="column.key === 'actions'">
          <a-popconfirm title="确定删除此行吗?" @confirm="handleDelete(index)">
            <a-button type="link" danger size="small">删除</a-button>
          </a-popconfirm>
        </template>
      </template>
    </a-table>
    <a-button type="dashed" block @click="handleAdd" style="margin-top: 8px;">
      <PlusOutlined /> 新增一行
    </a-button>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { v4 as uuidv4 } from 'uuid';
import { PlusOutlined } from '@ant-design/icons-vue';

const props = defineProps({
  value: { type: Array, default: () => [] },
  field: { type: Object, required: true },
});
const emit = defineEmits(['update:value']);

// 使用 v-model 语法糖
const localValue = computed({
  get: () => props.value,
  set: (val) => emit('update:value', val),
});

// 动态生成表格列定义
const tableColumns = computed(() => {
  const columns = props.field.props.columns.map(col => ({
    title: col.label,
    dataIndex: col.id,
    key: col.id,
    type: col.type, // 传递组件类型
  }));
  columns.push({
    title: '操作',
    key: 'actions',
    width: 80,
    align: 'center',
  });
  return columns;
});

const isEditableColumn = (key) => key !== 'actions';

// 根据列配置获取对应的 Ant Design 组件
const getComponentByType = (type) => {
  const map = {
    Input: 'a-input',
    DatePicker: 'a-date-picker',
    Select: 'a-select',
  };
  return map[type] || 'a-input';
};

const handleAdd = () => {
  const newRow = { __id: uuidv4() }; // 使用唯一ID作为row-key
  props.field.props.columns.forEach(col => {
    newRow[col.id] = undefined; // 初始化所有字段
  });
  localValue.value = [...(localValue.value || []), newRow];
};

const handleDelete = (index) => {
  const newValue = [...localValue.value];
  newValue.splice(index, 1);
  localValue.value = newValue;
};
</script>

<style scoped>
.editable-cell-form-item {
  margin-bottom: 0; /* 关键样式，移除表格单元格内表单项的下边距 */
}
</style>