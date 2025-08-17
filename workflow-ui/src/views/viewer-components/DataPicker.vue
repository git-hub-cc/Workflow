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
const loading = ref(false);
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
  // Display value is typically the value of the main field this component is bound to
  return props.value || '';
});

const fetchData = async (page = 1) => {
  loading.value = true;
  try {
    const params = {
      page: page,
      size: pagination.value.pageSize,
      search: searchQuery.value,
    };
    // Note: Assuming backend returns a paginated structure like { content: [], totalElements: 0 }
    // This needs to be adjusted based on your actual API response.
    const response = await fetchTableData(props.field.props.dataUrl, params);

    // Let's assume a simple array response for now, and handle pagination on client side for demo
    // In a real app, pagination should be driven by backend's `totalElements`.
    tableData.value = response; // Adjust if paginated: response.content;
    pagination.value.total = response.length; // Adjust if paginated: response.totalElements;
    pagination.value.current = page;

  } catch (error) {
    message.error('数据加载失败');
  } finally {
    loading.value = false;
  }
};

const openModal = () => {
  modalVisible.value = true;
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
      mappings[m.targetField] = selectedRow.value[m.sourceField];
    }
  });

  // The primary field value is typically the first mapping's source value, or the row's ID
  const primarySourceField = props.field.props.mappings[0]?.sourceField || 'id';
  const primaryValue = selectedRow.value[primarySourceField];

  // Emit an event to update the parent form data with all mappings
  emit('update:form-data', mappings);
  // Emit v-model update for the primary field
  emit('update:value', primaryValue);

  modalVisible.value = false;
  selectedKeys.value = [];
  selectedRow.value = null;
};
</script>