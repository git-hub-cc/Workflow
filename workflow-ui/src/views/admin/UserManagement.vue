<template>
  <div class="page-container">
    <a-page-header title="用户管理" sub-title="管理系统中的所有用户及其角色">
      <template #extra>
        <a-button type="primary" @click="showModal(null)">
          <template #icon><PlusOutlined /></template>
          新增用户
        </a-button>
      </template>
    </a-page-header>

    <div style="padding: 0 24px;">
      <a-table
          :columns="columns"
          :data-source="userStore.allUsers"
          :loading="userStore.loading"
          row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'role'">
            <a-tag :color="record.role === 'ADMIN' ? 'gold' : 'blue'">
              {{ record.role }}
            </a-tag>
          </template>
          <template v-if="column.key === 'managerName'">
            {{ getManagerName(record.managerId) }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" @click="showModal(record)">编辑</a-button>
              <a-popconfirm
                  title="确定要删除这个用户吗？"
                  ok-text="确认删除"
                  cancel-text="取消"
                  @confirm="handleDelete(record.id)"
                  :disabled="record.id === userStore.currentUser.id"
              >
                <a-button type="link" danger :disabled="record.id === userStore.currentUser.id">
                  删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 新增/编辑用户的弹窗 -->
    <a-modal
        :title="isEditing ? '编辑用户' : '新增用户'"
        v-model:open="modalVisible"
        :confirm-loading="modalConfirmLoading"
        @ok="handleOk"
        @cancel="handleCancel"
    >
      <a-form :model="formState" :rules="rules" ref="formRef" layout="vertical">
        <a-form-item label="用户ID (登录名)" name="id">
          <a-input v-model:value="formState.id" :disabled="isEditing" />
        </a-form-item>
        <a-form-item label="用户姓名" name="name">
          <a-input v-model:value="formState.name" />
        </a-form-item>
        <a-form-item label="角色" name="role">
          <a-select v-model:value="formState.role" placeholder="请选择角色">
            <a-select-option value="USER">普通用户 (USER)</a-select-option>
            <a-select-option value="ADMIN">管理员 (ADMIN)</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="直属上级" name="managerId">
          <a-select
              v-model:value="formState.managerId"
              placeholder="请选择直属上级 (可选)"
              allow-clear
              show-search
              :filter-option="filterOption"
          >
            <!-- 过滤掉用户自己，防止自己是自己的上级 -->
            <a-select-option v-for="user in availableManagers" :key="user.id" :value="user.id">
              {{ user.name }} ({{ user.id }})
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-alert v-if="!isEditing" message="新用户默认密码为 'password'，请提醒用户及时修改。" type="info" show-icon />
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { useUserStore } from '@/stores/user';
import { createUser, updateUser, deleteUser } from '@/api';
import { message } from 'ant-design-vue';
import { PlusOutlined } from '@ant-design/icons-vue';

const userStore = useUserStore();

onMounted(() => {
  if (userStore.allUsers.length === 0) {
    userStore.fetchAllUsers();
  }
});

const columns = [
  { title: '用户ID', dataIndex: 'id', key: 'id' },
  { title: '姓名', dataIndex: 'name', key: 'name' },
  { title: '角色', dataIndex: 'role', key: 'role', align: 'center' },
  { title: '直属上级', dataIndex: 'managerId', key: 'managerName', align: 'center' },
  { title: '操作', key: 'actions', align: 'center' },
];

// Modal state
const modalVisible = ref(false);
const modalConfirmLoading = ref(false);
const isEditing = ref(false);
const formRef = ref();
const formState = reactive({
  id: '',
  name: '',
  role: 'USER',
  managerId: null,
});

const rules = {
  id: [{ required: true, message: '请输入用户ID' }],
  name: [{ required: true, message: '请输入用户姓名' }],
  role: [{ required: true, message: '请选择角色' }],
};

const availableManagers = computed(() => {
  if (!isEditing.value) {
    return userStore.allUsers;
  }
  // 编辑时，从列表中排除自己
  return userStore.allUsers.filter(u => u.id !== formState.id);
});

const getManagerName = (managerId) => {
  if (!managerId) return '-';
  const manager = userStore.allUsers.find(u => u.id === managerId);
  return manager ? manager.name : managerId;
};

const filterOption = (input, option) => {
  // 允许通过姓名或ID搜索
  const label = option.children()[0].children.toLowerCase();
  return label.includes(input.toLowerCase());
};

const showModal = (user) => {
  if (user) {
    isEditing.value = true;
    Object.assign(formState, { ...user, managerId: user.managerId || null });
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

    // 准备提交的数据，如果 managerId 为空，确保发送 null 或 undefined 而不是空字符串
    const payload = { ...formState, managerId: formState.managerId || null };

    if (isEditing.value) {
      const updatedUser = await updateUser(payload.id, payload);
      userStore.updateUser(updatedUser);
      message.success('用户更新成功！');
    } else {
      const newUser = await createUser(payload);
      userStore.addUser(newUser);
      message.success('用户创建成功！');
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
  formState.id = '';
  formState.name = '';
  formState.role = 'USER';
  formState.managerId = null;
  formRef.value?.clearValidate();
};

const handleDelete = async (userId) => {
  try {
    await deleteUser(userId);
    userStore.removeUser(userId);
    message.success('用户删除成功！');
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