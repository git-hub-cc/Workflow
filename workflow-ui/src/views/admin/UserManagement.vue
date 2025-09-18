<template>
  <div class="page-container">
    <a-page-header title="用户管理" sub-title="管理系统中的所有用户及其角色和用户组">
      <template #extra>
        <a-button type="primary" @click="showModal(null)">
          <template #icon><PlusOutlined /></template>
          新增用户
        </a-button>
      </template>
    </a-page-header>

    <div class="content-padding">
      <a-card :bordered="false" style="margin-bottom: 24px;">
        <a-form :model="filterState" layout="inline" class="responsive-filter-form">
          <a-form-item label="关键字">
            <a-input v-model:value="filterState.keyword" placeholder="按用户ID或姓名搜索" allow-clear />
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
          <template v-if="column.key === 'roleNames'">
            <a-tag v-for="role in record.roleNames" :key="role" :color="getRoleColor(role)">
              {{ role }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'groupNames'">
            <a-tag v-for="group in record.groupNames" :key="group" color="blue">
              {{ group }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'managerName'">
            {{ getManagerName(record.managerId) }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" size="small" @click="showModal(record)">
                编辑
              </a-button>
              <a-button type="link" size="small" @click="handleResetPassword(record.id)">
                重置密码
              </a-button>
              <a-popconfirm
                  v-if="record.status === 'ACTIVE'"
                  title="确认禁用用户？"
                  content="该用户将无法登录系统。"
                  ok-text="确认禁用"
                  @confirm="handleDisable(record.id)"
              >
                <a-button
                    type="link"
                    size="small"
                    danger
                    :disabled="record.id === userStore.currentUser.id"
                >
                  禁用
                </a-button>
              </a-popconfirm>
              <a-popconfirm
                  v-if="record.status === 'INACTIVE'"
                  title="确认启用用户？"
                  content="该用户将可以正常登录系统。"
                  ok-text="确认启用"
                  @confirm="handleEnable(record.id)"
              >
                <a-button type="link" size="small">
                  启用
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <a-modal
        :title="isEditing ? '编辑用户' : '新增用户'"
        v-model:open="modalVisible"
        :confirm-loading="modalConfirmLoading"
        @ok="handleOk"
        @cancel="handleCancel"
        :width="isMobile ? '95%' : '800px'"
        destroyOnClose
    >
      <a-form :model="formState" :rules="rules" ref="formRef" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="用户ID (登录名)" name="id">
              <a-input v-model:value="formState.id" :disabled="isEditing" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="用户姓名" name="name">
              <a-input v-model:value="formState.name" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="邮箱" name="email">
              <a-input v-model:value="formState.email" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="手机号" name="phoneNumber">
              <a-input v-model:value="formState.phoneNumber" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="部门" name="departmentId">
              <a-tree-select
                  v-model:value="formState.departmentId"
                  :tree-data="departmentTree"
                  placeholder="请选择所属部门"
                  tree-default-expand-all
                  allow-clear
              />
            </a-form-item>
          </a-col>

          <a-col :xs="24" :sm="12">
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
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="角色" name="roleNames">
              <a-select
                  v-model:value="formState.roleNames"
                  mode="multiple"
                  placeholder="请分配角色"
                  :options="allRoles.map(r => ({ label: `${r.name} (${r.description})`, value: r.name }))"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="用户组" name="groupNames">
              <a-select
                  v-model:value="formState.groupNames"
                  mode="multiple"
                  placeholder="请分配用户组"
                  :options="allGroups.map(g => ({ label: `${g.name} (${g.description})`, value: g.name }))"
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
import { ref, reactive, onMounted, computed, onBeforeUnmount } from 'vue';
import { useUserStore } from '@/stores/user';
import {
  createUser, updateUser, disableUser, enableUser, resetPassword,
  getOrganizationTree, getDepartmentTree,
  getAllUsers, getRoles, getGroups
} from '@/api';
import { usePaginatedFetch } from '@/composables/usePaginatedFetch';
import { message, Modal } from 'ant-design-vue';
import { PlusOutlined, SearchOutlined, ReloadOutlined } from '@ant-design/icons-vue';

const userStore = useUserStore();

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
    getAllUsers,
    { keyword: '' },
    { defaultSort: 'id,asc' }
);

const orgTreeData = ref([]);
const departmentTree = ref([]);
const allRoles = ref([]);
const allGroups = ref([]);

const isMobile = ref(window.innerWidth < 768);
const handleResize = () => { isMobile.value = window.innerWidth < 768; };
onBeforeUnmount(() => { window.removeEventListener('resize', handleResize); });

const fetchAuxiliaryData = async () => {
  try {
    const promises = [
      getOrganizationTree().then(data => { orgTreeData.value = data; }),
      getDepartmentTree().then(data => { departmentTree.value = transformDeptTree(data); }),
      getRoles({ page: 0, size: 1000 }).then(res => { allRoles.value = res.content; }),
      getGroups({ page: 0, size: 1000 }).then(res => { allGroups.value = res.content; }),
    ];
    await Promise.all(promises);
  } catch (error) {
    message.error("加载辅助数据失败");
  }
};

onMounted(() => {
  window.addEventListener('resize', handleResize);
  fetchData();
  fetchAuxiliaryData();
});

const transformDeptTree = (nodes) => {
  return nodes.map(node => ({
    title: node.name,
    value: node.id,
    key: node.id,
    children: node.children ? transformDeptTree(node.children) : []
  }));
};

const orgTreeDataForSelector = computed(() => {
  const currentUserId = isEditing.value ? formState.id : null;
  const filterAndProcessTree = (nodes) => {
    if (!nodes) return [];
    return nodes.reduce((acc, node) => {
      if (node.type === 'department') {
        const processedChildren = filterAndProcessTree(node.children);
        acc.push({ ...node, children: processedChildren });
      } else if (node.type === 'user' && node.value !== currentUserId) {
        acc.push(node);
      }
      return acc;
    }, []);
  };
  return filterAndProcessTree(orgTreeData.value);
});


const columns = [
  { title: '用户ID', dataIndex: 'id', key: 'id', sorter: true, width: 120 },
  { title: '姓名', dataIndex: 'name', key: 'name', sorter: true, width: 120 },
  { title: '部门', dataIndex: 'departmentName', key: 'departmentName' },
  { title: '角色', dataIndex: 'roleNames', key: 'roleNames' },
  { title: '用户组', dataIndex: 'groupNames', key: 'groupNames' },
  { title: '状态', dataIndex: 'status', key: 'status', align: 'center', width: 100 },
  { title: '直属上级', dataIndex: 'managerId', key: 'managerName', align: 'center' },
  { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 200 },
];

const getRoleColor = (role) => (role === 'ADMIN' ? 'gold' : 'purple');
const getStatusColor = (status) => {
  if (status === 'ACTIVE') return 'success';
  if (status === 'INACTIVE') return 'default';
  if (status === 'LOCKED') return 'warning';
  return 'default';
};
const getStatusText = (status) => ({ ACTIVE: '正常', INACTIVE: '禁用', LOCKED: '锁定' }[status] || '未知');
const getManagerName = (managerId) => {
  if (!managerId) return '-';
  const manager = dataSource.value.find(u => u.id === managerId);
  return manager ? manager.name : managerId;
};

const modalVisible = ref(false);
const modalConfirmLoading = ref(false);
const isEditing = ref(false);
const formRef = ref();
const formState = reactive({
  id: '', name: '', email: '', phoneNumber: '',
  departmentId: null, managerId: null, roleNames: [], groupNames: [],
});

const rules = {
  id: [{ required: true, message: '请输入用户ID' }],
  name: [{ required: true, message: '请输入用户姓名' }],
  roleNames: [{ required: true, message: '请至少选择一个角色', type: 'array' }],
  email: [{ type: 'email', message: '请输入有效的邮箱地址' }],
};

const showModal = (user) => {
  if (user) {
    isEditing.value = true;
    const managerKey = user.managerId ? `user_${user.managerId}` : null;
    Object.assign(formState, { ...user, managerId: managerKey });
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

    const rawManagerId = formState.managerId && formState.managerId.startsWith('user_')
        ? formState.managerId.substring(5)
        : null;
    const payload = { ...formState, managerId: rawManagerId };

    if (isEditing.value) {
      await updateUser(payload.id, payload);
      message.success('用户更新成功！');
    } else {
      await createUser(payload);
      message.success('用户创建成功！');
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
  Object.assign(formState, {
    id: '', name: '', email: '', phoneNumber: '',
    departmentId: null, managerId: null, roleNames: [], groupNames: []
  });
};

const handleDisable = async (userId) => {
  try {
    await disableUser(userId);
    message.success('用户禁用成功！');
    await fetchData();
  } catch (error) {}
};

const handleEnable = async (userId) => {
  try {
    await enableUser(userId);
    message.success('用户启用成功！');
    await fetchData();
  } catch (error) {}
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