<template>
  <div>
    <!-- 引入通用属性配置 -->
    <GenericProps :field="field" :all-fields="allFields" />

    <a-divider>子表单列配置</a-divider>
    <div v-for="(col, index) in field.props.columns" :key="col.id" class="subform-column">
      <!-- 【核心修改】增加列类型选择 -->
      <a-select v-model:value="col.type" style="width: 120px;" @change="onColumnTypeChange(col)">
        <a-select-option value="Input">文本</a-select-option>
        <a-select-option value="InputNumber">数字</a-select-option>
        <a-select-option value="DatePicker">日期</a-select-option>
        <a-select-option value="UserPicker">人员</a-select-option>
        <!-- <a-select-option value="DataPicker">数据选择器</a-select-option> -->
        <a-select-option value="Formula">计算列</a-select-option>
      </a-select>

      <a-input v-model:value="col.label" placeholder="列标题" style="flex: 1;" />
      <!-- 【核心修改】为计算列提供表达式输入 -->
      <a-input
          v-if="col.type === 'Formula'"
          v-model:value="col.props.expression"
          placeholder="例如: {quantity} * {price}"
          style="flex: 1;"
      />
      <a-button type="text" danger @click="removeColumn(index)"><DeleteOutlined /></a-button>
    </div>
    <a-button type="dashed" block @click="addColumn"><PlusOutlined /> 添加列</a-button>

    <!-- 【核心新增】底部汇总行配置 -->
    <a-divider>汇总行配置</a-divider>
    <a-form-item>
      <a-switch v-model:checked="field.props.summary.enabled" checked-children="启用汇总行" un-checked-children="禁用汇总行" />
    </a-form-item>
    <template v-if="field.props.summary.enabled">
      <div v-for="(item, index) in field.props.summary.items" :key="index" class="summary-item">
        <a-select v-model:value="item.columnId" placeholder="选择汇总列" style="flex: 1;">
          <a-select-option v-for="col in summableColumns" :key="col.id" :value="col.id">
            {{ col.label }}
          </a-select-option>
        </a-select>
        <a-select v-model:value="item.type" style="width: 100px;">
          <a-select-option value="sum">求和</a-select-option>
          <a-select-option value="avg">平均值</a-select-option>
        </a-select>
        <a-button type="text" danger @click="removeSummaryItem(index)"><DeleteOutlined /></a-button>
      </div>
      <a-button type="dashed" block @click="addSummaryItem"><PlusOutlined /> 添加汇总项</a-button>
    </template>
  </div>
</template>

<script setup>
import { v4 as uuidv4 } from 'uuid';
import { computed } from 'vue';
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
    props: {}
  };
  props.field.props.columns.push(newColumn);
};

const removeColumn = (index) => {
  props.field.props.columns.splice(index, 1);
};

const onColumnTypeChange = (column) => {
  if (column.type === 'Formula') {
    column.props.expression = '';
  }
};

// --- 汇总行逻辑 ---
const summableColumns = computed(() => {
  return props.field.props.columns.filter(col => ['InputNumber', 'Formula'].includes(col.type));
});

const addSummaryItem = () => {
  if (!props.field.props.summary.items) {
    props.field.props.summary.items = [];
  }
  props.field.props.summary.items.push({ columnId: '', type: 'sum' });
};

const removeSummaryItem = (index) => {
  props.field.props.summary.items.splice(index, 1);
};
</script>

<style scoped>
.subform-column, .summary-item {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}
</style>