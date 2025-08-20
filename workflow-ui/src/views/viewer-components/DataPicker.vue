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

    <!-- 【核心修复】将 a-modal 包裹在 a-form-item-rest 中 -->
    <a-form-item-rest>
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
    </a-form-item-rest>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
// 【核心修复】从 ant-design-vue 导入 FormItemRest
import { message, FormItemRest as AFormItemRest } from 'ant-design-vue';
import { fetchTableData } from '@/api';

const props = defineProps(['value', 'field', 'form-data']);
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
  // 优先从 mappings 的第一个目标字段获取显示值
  const primaryTargetField = props.field.props.mappings?.[0]?.targetField;
  if (primaryTargetField && props.formData && props.formData[primaryTargetField]) {
    return props.formData[primaryTargetField];
  }
  // 否则回退到组件自身的 v-model 值
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

  // 【修复】改为直接 emit 'update:form-data' 事件，并传入一个包含所有映射的对象
  props.field.props.mappings.forEach(m => {
    if (m.sourceField && m.targetField) {
      // 分别触发每个映射字段的更新
      emit('update:form-data', m.targetField, selectedRow.value[m.sourceField]);
    }
  });

  // 更新组件自身绑定的主值
  const primarySourceField = props.field.props.mappings[0]?.sourceField || 'id';
  const primaryValue = selectedRow.value[primarySourceField];
  emit('update:value', primaryValue);

  modalVisible.value = false;
};
</script>