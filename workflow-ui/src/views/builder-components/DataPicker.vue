<template>
  <div>
    <a-input-search
        :value="displayValue"
        placeholder="点击选择"
        readonly
        @search="openModal"
    >
      <template #enterButton>
        <a-button>选择</a-button>
      </template>
    </a-input-search>

    <a-modal
        v-model:open="modalVisible"
        :title="field.props.modalTitle || '选择数据'"
        width="800px"
        @ok="handleOk"
        @cancel="modalVisible = false"
    >
      <a-input-search
          v-model:value="searchQuery"
          placeholder="输入关键词搜索"
          @search="fetchData(1)"
          style="margin-bottom: 16px;"
      />
      <!-- 【核心修改】将 loading 状态绑定到表格上 -->
      <a-table
          :columns="field.props.columns"
          :data-source="tableData"
          :loading="loading"
          :pagination="pagination"
          row-key="id"
          :row-selection="{ type: 'radio', selectedRowKeys: selectedKeys, onChange: onSelectChange }"
          @change="handleTableChange"
          size="small"
      />
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { message } from 'ant-design-vue';
import { fetchTableData } from '@/api';

const props = defineProps(['value', 'field']);
const emit = defineEmits(['update:value', 'update:form-data']);

const modalVisible = ref(false);
const loading = ref(false); // 表格的局部加载状态
const tableData = ref([]);
const searchQuery = ref('');
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
});
const selectedKeys = ref([]);
const selectedRow = ref(null);

const displayValue = computed(() => {
  return props.value || '';
});

const fetchData = async (page = 1) => {
  loading.value = true;
  try {
    const params = {
      page: page - 1, // 后端分页从0开始
      size: pagination.value.pageSize,
      search: searchQuery.value,
    };
    const response = await fetchTableData(props.field.props.dataUrl, params);

    // 兼容数组和分页对象两种返回格式
    if (Array.isArray(response)) {
      tableData.value = response;
      pagination.value.total = response.length;
    } else {
      tableData.value = response.content;
      pagination.value.total = response.totalElements;
    }
    pagination.value.current = page;

  } catch (error) {
    message.error('数据加载失败');
  } finally {
    loading.value = false;
  }
};

const openModal = () => {
  modalVisible.value = true;
  // 清空上次选择
  selectedKeys.value = [];
  selectedRow.value = null;
  fetchData();
};

const onSelectChange = (keys, rows) => {
  selectedKeys.value = keys;
  selectedRow.value = rows[0];
};

const handleTableChange = (pager) => {
  fetchData(pager.current);
};

const handleOk = () => {
  if (!selectedRow.value) {
    message.warn('请选择一条数据');
    return;
  }

  const mappings = {};
  props.field.props.mappings.forEach(m => {
    if (m.sourceField && m.targetField) {
      // 使用 emit 更新整个 formData，而不仅仅是单个字段
      emit('update:form-data', m.targetField, selectedRow.value[m.sourceField]);
    }
  });

  // 主字段的值通常是第一个映射的源字段值，或者是行的ID
  const primarySourceField = props.field.props.mappings[0]?.sourceField || 'id';
  const primaryValue = selectedRow.value[primarySourceField];

  // 使用 v-model 更新主字段的值
  emit('update:value', primaryValue);

  modalVisible.value = false;
};
</script>