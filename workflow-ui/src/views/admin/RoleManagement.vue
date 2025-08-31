<template>
  <div class="page-container">
    <a-page-header title="角色管理" sub-title="定义系统中的用户角色及其权限">
      <template #extra>
        <a-button type="primary" @click="showModal(null)">
          <template #icon><PlusOutlined /></template>
          新增角色
        </a-button>
      </template>
    </a-page-header>

    <div style="padding: 24px;">
      <!-- 【阶段二新增】筛选区域 -->
      <a-card :bordered="false" style="margin-bottom: 24px;">
        <a-form :model="filterState" layout="inline">
          <a-form-item label="角色名称">
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
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a-tag color="purple">{{ record.name }}</a-tag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" @click="showModal(record)">编辑</a-button>
              <a-popconfirm
                  title="确定要删除这个角色吗？"
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

    <!-- 新增/编辑角色的弹窗 -->
    <a-modal
        :title="isEditing ? '编辑角色' : '新增角色'"
        v-model:open="modalVisible"
        :confirm-loading="modalConfirmLoading"
        @ok="handleOk"
        @cancel="handleCancel"
        destroyOnClose
    >
      <a-form :model="formState" :rules="rules" ref="formRef" layout="vertical">
        <a-form-item label="角色名称 (英文大写)" name="name" help="例如: FINANCE_APPROVER, 创建后不可修改">
          <a-input v-model:value="formState.name" :disabled="isEditing" />
        </a-form-item>
        <a-form-item label="角色描述" name="description">
          <a-textarea v-model:value="formState.description" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { createRole, updateRole, deleteRole, getRoles } from '@/api';
import { usePaginatedFetch } from '@/composables/usePaginatedFetch';
import { message } from 'ant-design-vue';
import { PlusOutlined, SearchOutlined, ReloadOutlined } from '@ant-design/icons-vue';

// --- 【阶段二修改】使用 usePaginatedFetch Hook ---
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
    getRoles,
    { name: '' },
    { defaultSort: 'id,asc' }
);

onMounted(fetchData);

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '角色名称', dataIndex: 'name', key: 'name', sorter: true },
  { title: '描述', dataIndex: 'description', key: 'description' },
  { title: '操作', key: 'actions', align: 'center', width: 150 },
];

// Modal state
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
    { required: true, message: '请输入角色名称' },
    { pattern: /^[A-Z_]+$/, message: '只能包含大写字母和下划线' }
  ],
  description: [{ required: true, message: '请输入角色描述' }],
};


const showModal = (role) => {
  if (role) {
    isEditing.value = true;
    Object.assign(formState, role);
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
      await updateRole(formState.id, formState);
      message.success('角色更新成功！');
    } else {
      await createRole(formState);
      message.success('角色创建成功！');
    }
    modalVisible.value = false;
    await fetchData(); // 【阶段二修改】刷新数据
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

const handleDelete = async (roleId) => {
  try {
    await deleteRole(roleId);
    message.success('角色删除成功！');
    await fetchData(); // 【阶段二修改】刷新数据
  } catch (error) {
    // API 错误已全局处理
  }
};
</script>

<style scoped>
.page-container {
  background-color: #fff;
  border-radius: 4px;
}
</style>