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

    <div style="padding: 0 24px;">
      <a-table
          :columns="columns"
          :data-source="userStore.allRoles"
          :loading="loading"
          row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a-tag color="purple">{{ record.name }}</a-tag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <!--
              <a-button type="link" @click="showModal(record)">编辑</a-button>
              <a-popconfirm
                  title="确定要删除这个角色吗？"
                  @confirm="handleDelete(record.id)"
              >
                <a-button type="link" danger>删除</a-button>
              </a-popconfirm>
              -->
              <a-tooltip title="编辑和删除功能待后续权限模块完善">
                <a-button type="link" disabled>编辑</a-button>
              </a-tooltip>
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
    >
      <a-form :model="formState" :rules="rules" ref="formRef" layout="vertical">
        <a-form-item label="角色名称 (英文大写)" name="name" help="例如: FINANCE_APPROVER">
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
import { useUserStore } from '@/stores/user';
import { createRole } from '@/api';
import { message } from 'ant-design-vue';
import { PlusOutlined } from '@ant-design/icons-vue';

const userStore = useUserStore();
const loading = ref(false);

onMounted(() => {
  if (userStore.allRoles.length === 0) {
    loading.value = true;
    userStore.fetchAllRoles().finally(() => {
      loading.value = false;
    });
  }
});

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '角色名称', dataIndex: 'name', key: 'name' },
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
      // const updatedRole = await updateRole(formState.id, formState);
      // store.updateRole(updatedRole);
      message.warn('编辑功能暂未实现');
    } else {
      const newRole = await createRole(formState);
      userStore.addRole(newRole);
      message.success('角色创建成功！');
    }
    modalVisible.value = false;
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
  formRef.value?.clearValidate();
};

// const handleDelete = async (roleId) => { ... }
</script>

<style scoped>
.page-container {
  background-color: #fff;
  border-radius: 4px;
}
</style>