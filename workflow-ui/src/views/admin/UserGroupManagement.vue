<template>
  <div class="page-container">
    <a-page-header title="用户组管理" sub-title="管理可用于流程审批的候选组">
      <template #extra>
        <a-button type="primary" @click="showModal(null)">
          <template #icon><PlusOutlined /></template>
          新增用户组
        </a-button>
      </template>
    </a-page-header>

    <div style="padding: 24px;">
      <a-card :bordered="false" style="margin-bottom: 24px;">
        <a-form :model="filterState" layout="inline">
          <a-form-item label="用户组名称">
            <a-input v-model:value="filterState.name" placeholder="输入名称模糊查询" allow-clear />
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" @click="handleSearch">
                <template #icon><SearchOutlined /></template>
                查询
              </a-button>
              <a-button @click="handleReset">
                <template #icon><ReloadOutlined /></template>
                重置
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </a-card>

      <a-table
          :columns="columns"
          :data-source="dataSource"
          :loading="loading"
          :pagination="pagination"
          row-key="id"
          @change="handleTableChange"
          :scroll="{ x: 'max-content' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a-tag color="processing">{{ record.name }}</a-tag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" @click="showModal(record)">编辑</a-button>
              <a-popconfirm
                  title="确定要删除这个用户组吗？"
                  ok-text="确认删除"
                  cancel-text="取消"
                  @confirm="handleDelete(record.id)"
              >
                <a-button type="link" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <a-modal
        :title="isEditing ? '编辑用户组' : '新增用户组'"
        v-model:open="modalVisible"
        :confirm-loading="modalConfirmLoading"
        @ok="handleOk"
        @cancel="handleCancel"
        destroyOnClose
    >
      <a-form :model="formState" :rules="rules" ref="formRef" layout="vertical">
        <a-form-item label="用户组名称 (英文)" name="name" help="例如: R_D_DEPARTMENT_APPROVERS">
          <a-input v-model:value="formState.name" />
        </a-form-item>
        <a-form-item label="用户组描述" name="description">
          <a-textarea v-model:value="formState.description" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { createGroup, updateGroup, deleteGroup, getGroups } from '@/api';
import { usePaginatedFetch } from '@/composables/usePaginatedFetch';
import { message } from 'ant-design-vue';
import { PlusOutlined, SearchOutlined, ReloadOutlined } from '@ant-design/icons-vue';

const {
  loading,
  dataSource,
  pagination,
  filterState,
  handleTableChange,
  handleSearch,
  handleReset,
  fetchData,
} = usePaginatedFetch(
    getGroups,
    { name: '' },
    { defaultSort: 'id,asc' }
);

onMounted(fetchData);

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '用户组名称', dataIndex: 'name', key: 'name', sorter: true },
  { title: '描述', dataIndex: 'description', key: 'description' },
  { title: '操作', key: 'actions', align: 'center', width: 150 },
];

const modalVisible = ref(false);
const modalConfirmLoading = ref(false);
const isEditing = ref(false);
const formRef = ref();
const formState = reactive({
  id: null,
  name: '',
  description: '',
});

const rules = {
  name: [
    { required: true, message: '请输入用户组名称' },
    { pattern: /^[A-Za-z0-9_]+$/, message: '只能包含字母、数字和下划线' }
  ],
  description: [{ required: true, message: '请输入用户组描述' }],
};


const showModal = (group) => {
  if (group) {
    isEditing.value = true;
    Object.assign(formState, group);
  } else {
    isEditing.value = false;
    resetForm();
  }
  modalVisible.value = true;
};

const handleOk = async () => {
  try {
    await formRef.value.validate();
    modalConfirmLoading.value = true;

    if (isEditing.value) {
      await updateGroup(formState.id, formState);
      message.success('用户组更新成功！');
    } else {
      await createGroup(formState);
      message.success('用户组创建成功！');
    }
    modalVisible.value = false;
    await fetchData();
  } catch (error) {
    console.error('Form validation/submission failed:', error);
  } finally {
    modalConfirmLoading.value = false;
  }
};

const handleCancel = () => {
  modalVisible.value = false;
};

const resetForm = () => {
  formState.id = null;
  formState.name = '';
  formState.description = '';
};

const handleDelete = async (groupId) => {
  try {
    await deleteGroup(groupId);
    message.success('用户组删除成功！');
    await fetchData();
  } catch (error) {}
};
</script>

<style scoped>
.page-container {
  background-color: #fff;
  border-radius: 4px;
}
@media (max-width: 768px) {
  :deep(.ant-form-inline .ant-form-item) {
    margin-bottom: 16px;
  }
}
</style>