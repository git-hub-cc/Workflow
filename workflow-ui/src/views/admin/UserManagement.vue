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
          :loading="loading"
          row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'roleNames'">
            <a-tag v-for="role in record.roleNames" :key="role" :color="getRoleColor(role)">
              {{ role }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'managerName'">
            {{ getManagerName(record.managerId) }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-dropdown>
              <a-button type="link">操作 <DownOutlined /></a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="showModal(record)">
                    <EditOutlined /> 编辑
                  </a-menu-item>
                  <a-menu-item @click="handleResetPassword(record.id)">
                    <ReloadOutlined /> 重置密码
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item
                      @click="handleDelete(record.id)"
                      danger
                      :disabled="record.id === userStore.currentUser.id || record.status === 'INACTIVE'"
                  >
                    <UserDeleteOutlined /> 禁用
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
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
        width="600px"
    >
      <a-form :model="formState" :rules="rules" ref="formRef" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="用户ID (登录名)" name="id">
              <a-input v-model:value="formState.id" :disabled="isEditing" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="用户姓名" name="name">
              <a-input v-model:value="formState.name" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="部门" name="department">
              <a-input v-model:value="formState.department" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="直属上级" name="managerId">
              <a-tree-select
                  v-model:value="formState.managerId"
                  show-search
                  style="width: 100%"
                  :dropdown-style="{ maxHeight: '400px', overflow: 'auto' }"
                  placeholder="请选择直属上级"
                  allow-clear
                  tree-default-expand-all
                  :tree-data="orgTreeDataForSelector"
                  :field-names="{ children: 'children', label: 'title', value: 'key' }"
                  :tree-node-filter-prop="'title'"
                  @change="handleManagerChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="角色" name="roleNames">
              <a-select
                  v-model:value="formState.roleNames"
                  mode="multiple"
                  placeholder="请分配角色"
                  :options="userStore.allRoles.map(r => ({ label: `${r.name} (${r.description})`, value: r.name }))"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-alert v-if="!isEditing" message="新用户默认密码为 'password'，用户首次登录将被要求修改。" type="info" show-icon />
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { useUserStore } from '@/stores/user';
import { createUser, updateUser, deleteUser, resetPassword, getOrganizationTree } from '@/api';
import { message, Modal } from 'ant-design-vue';
import { PlusOutlined, DownOutlined, EditOutlined, ReloadOutlined, UserDeleteOutlined } from '@ant-design/icons-vue';

const userStore = useUserStore();
const loading = ref(false);

const orgTreeData = ref([]);

const fetchInitialData = async () => {
  loading.value = true;
  try {
    await Promise.all([
      userStore.allUsers.length === 0 ? userStore.fetchAllUsers() : Promise.resolve(),
      userStore.allRoles.length === 0 ? userStore.fetchAllRoles() : Promise.resolve(),
      getOrganizationTree().then(data => {
        orgTreeData.value = data;
      })
    ]);
  } catch (error) {
    message.error("初始化页面数据失败");
  } finally {
    loading.value = false;
  }
};

onMounted(fetchInitialData);

const orgTreeDataForSelector = computed(() => {
  const currentUserId = isEditing.value ? formState.id : null;

  const filterAndProcessTree = (nodes) => {
    if (!nodes) return [];
    return nodes.reduce((acc, node) => {
      if (node.type === 'department') {
        const processedChildren = filterAndProcessTree(node.children);
        if (processedChildren.length > 0) {
          // Department is kept, but make it unselectable
          acc.push({ ...node, children: processedChildren, disabled: true });
        }
      } else if (node.type === 'user' && node.value !== currentUserId) {
        // User node is kept as is (it's selectable)
        acc.push(node);
      }
      return acc;
    }, []);
  };

  return filterAndProcessTree(orgTreeData.value);
});


const columns = [
  { title: '用户ID', dataIndex: 'id', key: 'id' },
  { title: '姓名', dataIndex: 'name', key: 'name' },
  { title: '角色', dataIndex: 'roleNames', key: 'roleNames' },
  { title: '状态', dataIndex: 'status', key: 'status', align: 'center' },
  { title: '直属上级', dataIndex: 'managerId', key: 'managerName', align: 'center' },
  { title: '操作', key: 'actions', align: 'center' },
];

const getRoleColor = (role) => (role === 'ADMIN' ? 'gold' : 'blue');
const getStatusColor = (status) => {
  if (status === 'ACTIVE') return 'success';
  if (status === 'INACTIVE') return 'default';
  if (status === 'LOCKED') return 'warning';
  return 'default';
};
const getStatusText = (status) => ({ ACTIVE: '正常', INACTIVE: '禁用', LOCKED: '锁定' }[status] || '未知');
const getManagerName = (managerId) => {
  if (!managerId) return '-';
  const manager = userStore.allUsers.find(u => u.id === managerId);
  return manager ? manager.name : managerId;
};

const modalVisible = ref(false);
const modalConfirmLoading = ref(false);
const isEditing = ref(false);
const formRef = ref();
const formState = reactive({
  id: '', name: '', department: '', managerId: null, roleNames: [],
});

const rules = {
  id: [{ required: true, message: '请输入用户ID' }],
  name: [{ required: true, message: '请输入用户姓名' }],
  roleNames: [{ required: true, message: '请至少选择一个角色', type: 'array' }],
};

const showModal = (user) => {
  if (user) {
    isEditing.value = true;
    // The TreeSelect now uses the 'key' (e.g., 'user_jsmith') as its value.
    // We need to construct this key from the raw managerId ('jsmith').
    const managerKey = user.managerId ? `user_${user.managerId}` : null;
    Object.assign(formState, { ...user, managerId: managerKey });
  } else {
    isEditing.value = false;
    resetForm();
  }
  modalVisible.value = true;
};

// --- 【FIX】: Refactored to remove dependency on deprecated `triggerNode` ---
const handleManagerChange = (value) => {
  // Only auto-fill department if a user is selected and the department is currently empty.
  // We check the prefix of the `value` (which is the node's `key`) to determine its type.
  if (value && value.startsWith('user_') && !formState.department) {
    const findParent = (tree, childKey) => {
      for (const node of tree) {
        if (node.children?.some(child => child.key === childKey)) {
          return node;
        }
      }
      return null;
    };
    const deptNode = findParent(orgTreeData.value, value);
    if (deptNode) {
      formState.department = deptNode.title;
    }
  }
};

const handleOk = async () => {
  try {
    await formRef.value.validate();
    modalConfirmLoading.value = true;

    // The TreeSelect v-model now holds the 'key' (e.g., 'user_jsmith').
    // We need to extract the raw ID ('jsmith') for the API.
    const rawManagerId = formState.managerId && formState.managerId.startsWith('user_')
        ? formState.managerId.substring(5)
        : null;
    const payload = { ...formState, managerId: rawManagerId };

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
    await fetchInitialData();
  } catch (error) {
    console.error('Form validation/submission failed:', error);
  } finally {
    modalConfirmLoading.value = false;
  }
};

const handleCancel = () => modalVisible.value = false;
const resetForm = () => { Object.assign(formState, { id: '', name: '', department: '', managerId: null, roleNames: [] }); formRef.value?.clearValidate(); };

const handleDelete = (userId) => {
  Modal.confirm({
    title: '确认禁用用户',
    content: `确定要禁用用户 ${userId} 吗？该用户将无法登录系统。`,
    okText: '确认禁用',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await deleteUser(userId);
        userStore.removeUser(userId);
        message.success('用户禁用成功！');
      } catch (error) {}
    },
  });
};

const handleResetPassword = (userId) => {
  Modal.confirm({
    title: '确认重置密码',
    content: `确定要将用户 ${userId} 的密码重置为默认密码 'password' 吗？`,
    okText: '确认重置',
    onOk: async () => {
      try {
        await resetPassword(userId);
        message.success('密码重置成功！用户下次登录时将被要求修改。');
      } catch (error) {}
    },
  });
};
</script>

<style scoped>
.page-container {
  background-color: #fff;
  border-radius: 4px;
}
</style>