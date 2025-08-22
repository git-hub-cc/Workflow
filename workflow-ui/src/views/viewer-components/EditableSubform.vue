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
            <!-- 【核心修改】动态渲染不同的组件类型 -->
            <component
                :is="getComponentByType(column.type)"
                v-model:value="record[column.key]"
                size="small"
                placeholder="请输入"
                style="width: 100%;"
                show-search
                :options="column.type === 'UserPicker' ? userOptions[index]?.[column.key] : undefined"
                :filter-option="false"
                :not-found-content="userSearchLoading ? undefined : null"
                @search="keyword => handleUserSearch(keyword, index, column.key)"
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
import { computed, ref, onMounted } from 'vue';
import { v4 as uuidv4 } from 'uuid';
import { PlusOutlined } from '@ant-design/icons-vue';
import { searchUsersForPicker } from '@/api';

const props = defineProps({
  value: { type: Array, default: () => [] },
  field: { type: Object, required: true },
});
const emit = defineEmits(['update:value']);

const localValue = computed({
  get: () => props.value,
  set: (val) => emit('update:value', val),
});

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

const isEditableColumn = (key) => key !== 'actions';

const getComponentByType = (type) => {
  const map = {
    Input: 'a-input',
    DatePicker: 'a-date-picker',
    UserPicker: 'a-select',
    DataPicker: 'a-input', // 在子表单中 DataPicker 简化为输入框
  };
  return map[type] || 'a-input';
};

// --- 【新增】人员选择器相关逻辑 ---
const userOptions = ref([]);
const userSearchLoading = ref(false);
let searchTimeout;

onMounted(() => {
  // 初始化时，为已有的 UserPicker 字段填充显示名称
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
</script>

<style scoped>
.editable-cell-form-item {
  margin-bottom: 0;
}
</style>