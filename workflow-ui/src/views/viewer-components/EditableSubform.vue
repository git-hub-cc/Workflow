<template>
  <div class="editable-subform">
    <a-table
        :columns="tableColumns"
        :data-source="localValue"
        :pagination="false"
        bordered
        size="small"
        row-key="__id"
        :summary="field.props.summary?.enabled ? renderSummary : undefined"
    >
      <template #bodyCell="{ column, record, index }">
        <template v-if="isEditableColumn(column.dataIndex)">
          <a-form-item
              :name="[field.id, index, column.dataIndex]"
              :rules="[{ required: true, message: '此项必填' }]"
              class="editable-cell-form-item"
          >
            <component
                :is="getComponentByType(column.type)"
                v-model:value="record[column.dataIndex]"
                size="small"
                placeholder="请输入"
                style="width: 100%;"
                show-search
                :options="column.type === 'UserPicker' ? userOptions[index]?.[column.dataIndex] : undefined"
                :filter-option="false"
                :not-found-content="userSearchLoading ? undefined : null"
                @search="keyword => handleUserSearch(keyword, index, column.dataIndex)"
            />
          </a-form-item>
        </template>
        <!-- 【核心修改】计算列的只读显示 -->
        <template v-else-if="column.type === 'Formula'">
          <span>{{ record[column.dataIndex] }}</span>
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
// 【核心修改】从 vue 导入 h 函数
import { computed, ref, onMounted, watch, h } from 'vue';
import { v4 as uuidv4 } from 'uuid';
import { PlusOutlined } from '@ant-design/icons-vue';
import { searchUsersForPicker } from '@/api';
// 【核心修改】从 ant-design-vue 导入 Table 的子组件，以便 h 函数可以引用它们
import { Table } from 'ant-design-vue';

const props = defineProps({
  value: { type: Array, default: () => [] },
  field: { type: Object, required: true },
});
const emit = defineEmits(['update:value']);

const localValue = computed({
  get: () => props.value || [],
  set: (val) => emit('update:value', val),
});

watch(localValue, (newVal) => {
  if (!Array.isArray(newVal)) return;
  const formulaColumns = props.field.props.columns.filter(c => c.type === 'Formula');
  if (formulaColumns.length === 0) return;

  newVal.forEach(row => {
    formulaColumns.forEach(col => {
      const expr = col.props.expression;
      if (!expr) return;
      try {
        const variableNames = expr.match(/{[a-zA-Z0-9_]+}/g)?.map(v => v.slice(1, -1)) || [];
        const funcBody = `return ${expr.replace(/{/g, '').replace(/}/g, '')};`;
        const func = new Function(...variableNames, funcBody);
        const args = variableNames.map(name => Number(row[name]) || 0);
        const result = func(...args);
        row[col.id] = isNaN(result) ? 'N/A' : result.toFixed(2);
      } catch (e) {
        row[col.id] = '公式错误';
      }
    });
  });
}, { deep: true });

const tableColumns = computed(() => {
  const columns = props.field.props.columns.map(col => ({
    title: col.label,
    dataIndex: col.id,
    key: col.id,
    type: col.type,
  }));
  columns.push({
    title: '操作',
    key: 'actions',
    width: 80,
    align: 'center',
  });
  return columns;
});

const isEditableColumn = (key) => {
  const col = props.field.props.columns.find(c => c.id === key);
  return col && col.type !== 'Formula';
};

const getComponentByType = (type) => {
  const map = {
    Input: 'a-input',
    InputNumber: 'a-input-number',
    DatePicker: 'a-date-picker',
    UserPicker: 'a-select',
  };
  return map[type] || 'a-input';
};

const userOptions = ref([]);
const userSearchLoading = ref(false);
let searchTimeout;

onMounted(() => {
  if (localValue.value && localValue.value.length > 0) {
    localValue.value.forEach((row, index) => {
      props.field.props.columns.forEach(async col => {
        if (col.type === 'UserPicker' && row[col.id]) {
          const results = await searchUsersForPicker(row[col.id]);
          if (results && results.length > 0) {
            updateUserOptions(index, col.id, results.map(u => ({ label: `${u.name} (${u.id})`, value: u.id })));
          }
        }
      });
    });
  }
});

const updateUserOptions = (rowIndex, colId, options) => {
  if (!userOptions.value[rowIndex]) {
    userOptions.value[rowIndex] = {};
  }
  userOptions.value[rowIndex][colId] = options;
};

const handleUserSearch = (keyword, rowIndex, colId) => {
  clearTimeout(searchTimeout);
  if (!keyword) {
    updateUserOptions(rowIndex, colId, []);
    return;
  }
  userSearchLoading.value = true;
  searchTimeout = setTimeout(async () => {
    try {
      const results = await searchUsersForPicker(keyword);
      updateUserOptions(rowIndex, colId, results.map(u => ({ label: `${u.name} (${u.id})`, value: u.id })));
    } catch (error) {
      updateUserOptions(rowIndex, colId, []);
    } finally {
      userSearchLoading.value = false;
    }
  }, 300);
};

const handleAdd = () => {
  const newRow = { __id: uuidv4() };
  props.field.props.columns.forEach(col => {
    newRow[col.id] = undefined;
  });
  localValue.value = [...(localValue.value || []), newRow];
};

const handleDelete = (index) => {
  const newValue = [...localValue.value];
  newValue.splice(index, 1);
  localValue.value = newValue;
};

// 【核心修改】使用 h 函数重写汇总行渲染逻辑
const renderSummary = () => {
  const summaryItems = props.field.props.summary?.items;
  if (!summaryItems || summaryItems.length === 0) return null;

  const summaryData = {};
  summaryItems.forEach(item => {
    const values = localValue.value.map(row => Number(row[item.columnId]) || 0);
    if (item.type === 'sum') {
      summaryData[item.columnId] = values.reduce((sum, val) => sum + val, 0).toFixed(2);
    } else if (item.type === 'avg') {
      const sum = values.reduce((s, v) => s + v, 0);
      summaryData[item.columnId] = values.length > 0 ? (sum / values.length).toFixed(2) : '0.00';
    }
  });

  const cells = [
    h(Table.Summary.Cell, { index: 0 }, { default: () => '合计' })
  ];

  tableColumns.value.slice(1).forEach((col, index) => {
    cells.push(
        h(Table.Summary.Cell, { index: index + 1 }, { default: () => summaryData[col.key] || '' })
    );
  });

  return h(Table.Summary.Row, null, { default: () => cells });
};
</script>

<style scoped>
.editable-cell-form-item {
  margin-bottom: 0;
}
</style>