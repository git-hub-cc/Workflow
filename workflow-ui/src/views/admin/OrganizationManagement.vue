<template>
  <div class="page-container">
    <a-page-header title="组织架构管理" sub-title="管理部门层级和员工归属关系">
      <template #extra>
        <a-space>
          <a-button type="primary" @click="showModal('create')">
            <template #icon><PlusOutlined /></template>
            新增部门
          </a-button>
          <a-button @click="fetchData" :loading="loading">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </a-space>
      </template>
    </a-page-header>

    <div style="padding: 24px;">
      <a-spin :spinning="loading">
        <a-alert
            message="操作说明"
            description="1. 点击部门旁的 [+] 新增子部门，[*] 编辑，[x] 删除。 2. 您依然可以将员工节点拖拽到不同的部门节点上，以更新其所属部门。"
            type="info"
            show-icon
            style="margin-bottom: 24px;"
        />
        <div class="tree-container">
          <a-tree
              v-if="treeData.length > 0"
              v-model:expandedKeys="expandedKeys"
              :tree-data="treeData"
              :draggable="true"
              @dragstart="handleDragStart"
              @drop="handleDrop"
              block-node
          >
            <template #title="{ title, dataRef }">
              <div class="tree-node">
                <span :class="{ 'user-node': dataRef.type === 'user', 'dept-node': dataRef.type === 'department' }">
                  <component :is="dataRef.type === 'department' ? ApartmentOutlined : UserOutlined" style="margin-right: 8px;" />
                  {{ title }}
                </span>
                <!-- 【核心修改】将操作按钮始终可见，而不是依赖 hover -->
                <a-space v-if="dataRef.type === 'department'" class="node-actions" @click.stop>
                  <a-tooltip title="新增子部门">
                    <a-button type="text" size="small" @click="showModal('create-sub', dataRef)">
                      <PlusCircleOutlined />
                    </a-button>
                  </a-tooltip>
                  <a-tooltip title="编辑部门">
                    <a-button type="text" size="small" @click="showModal('edit', dataRef)">
                      <EditOutlined />
                    </a-button>
                  </a-tooltip>
                  <a-tooltip title="删除部门">
                    <a-popconfirm
                        title="确定要删除这个部门吗？"
                        content="只有当部门下无子部门和员工时才能删除。"
                        @confirm="handleDelete(dataRef)"
                    >
                      <a-button type="text" danger size="small"><DeleteOutlined /></a-button>
                    </a-popconfirm>
                  </a-tooltip>
                </a-space>
              </div>
            </template>
          </a-tree>
        </div>
      </a-spin>
    </div>

    <!-- 新增/编辑部门的弹窗 -->
    <a-modal v-model:open="modalVisible" :title="modalTitle" :confirm-loading="modalConfirmLoading" @ok="handleOk">
      <a-form ref="formRef" :model="formState" :rules="rules" layout="vertical">
        <a-form-item label="部门名称" name="name">
          <a-input v-model:value="formState.name" placeholder="请输入部门名称" />
        </a-form-item>
        <a-form-item label="上级部门" name="parentId">
          <a-tree-select
              v-model:value="formState.parentId"
              :tree-data="departmentTreeForSelect"
              placeholder="不选则为顶级部门"
              tree-default-expand-all
              allow-clear
          />
        </a-form-item>
        <a-form-item label="部门负责人" name="managerId">
          <a-select
              v-model:value="formState.managerId"
              :options="allUsers.map(u => ({ label: `${u.name} (${u.id})`, value: u.id }))"
              placeholder="请选择部门负责人"
              show-search
              option-filter-prop="label"
              allow-clear
          />
        </a-form-item>
        <a-form-item label="显示排序" name="orderNum">
          <a-input-number v-model:value="formState.orderNum" style="width: 100%;" />
        </a-form-item>
      </a-form>
    </a-modal>

  </div>
</template>

<script setup>
import { ref, onMounted, reactive, computed } from 'vue';
import { getOrganizationTree, updateUser, getAllUsers, createDepartment, updateDepartment, deleteDepartment } from '@/api';
import { message, Modal } from 'ant-design-vue';
import {
  ReloadOutlined,
  PlusOutlined,
  ApartmentOutlined,
  UserOutlined,
  EditOutlined,
  DeleteOutlined,
  PlusCircleOutlined
} from '@ant-design/icons-vue';

const loading = ref(true);
const treeData = ref([]);
const expandedKeys = ref([]);
const allUsers = ref([]);
const usersCache = ref(new Map());

const fetchData = async () => {
  loading.value = true;
  try {
    const [orgTree, usersResponse] = await Promise.all([
      getOrganizationTree(),
      getAllUsers({ page: 0, size: 1000 })
    ]);

    treeData.value = orgTree;
    expandedKeys.value = orgTree.map(dept => dept.key);
    allUsers.value = usersResponse.content;

    usersCache.value.clear();
    allUsers.value.forEach(u => usersCache.value.set(u.id, u));

  } catch (error) {
    message.error('加载组织架构失败');
  } finally {
    loading.value = false;
  }
};

onMounted(fetchData);

const handleDragStart = ({ event, node }) => {
  if (node.dataRef.type !== 'user') {
    event.preventDefault();
    return;
  }
  event.dataTransfer.setData('sourceUserKey', node.key);
};

const handleDrop = async ({ event, node, dragNode }) => {
  event.preventDefault();
  const dropKey = node.key;
  const sourceUserKey = event.dataTransfer.getData('sourceUserKey');
  if (!sourceUserKey) return;

  const dropNode = node.dataRef;
  const dragNodeData = dragNode.dataRef;

  if (dragNodeData.type !== 'user' || dropNode.type !== 'department') {
    message.warn('只能将员工拖拽到部门节点上！');
    return;
  }
  const userId = dragNodeData.value;
  const user = usersCache.value.get(userId);
  if (dropKey === `dept_${user?.departmentId}`) {
    return;
  }

  Modal.confirm({
    title: '确认移动员工',
    content: `确定要将员工 “${user.name}” 移动到 “${dropNode.title}” 部门吗？`,
    onOk: async () => {
      loading.value = true;
      try {
        const payload = { ...user, departmentId: parseInt(dropKey.replace('dept_', '')) };
        await updateUser(userId, payload);
        message.success(`员工 “${user.name}” 已成功移动！`);
        await fetchData();
      } catch (error) {
        message.error('更新失败，请重试');
        loading.value = false;
      }
    },
  });
};


const modalVisible = ref(false);
const modalConfirmLoading = ref(false);
const isEditing = ref(false);
const formRef = ref();
const modalTitle = ref('');

const defaultFormState = {
  id: null, name: '', parentId: null, managerId: null, orderNum: 0,
};
const formState = reactive({ ...defaultFormState });
const rules = { name: [{ required: true, message: '请输入部门名称' }] };

const departmentTreeForSelect = computed(() => {
  const disableNodeAndChildren = (nodes, disableId) => {
    if (!disableId) return nodes;
    return nodes.map(node => {
      const isDisabled = node.value === disableId;
      const children = node.children ? disableNodeAndChildren(node.children, disableId) : undefined;
      return { ...node, children, disabled: isDisabled };
    }).filter(node => node.value !== disableId);
  };

  const rawTree = transformDeptTreeForSelect(treeData.value);
  return isEditing.value ? disableNodeAndChildren(rawTree, formState.id) : rawTree;
});

const transformDeptTreeForSelect = (nodes) => {
  return nodes.filter(n => n.type === 'department').map(node => ({
    title: node.title,
    value: node.value,
    children: node.children ? transformDeptTreeForSelect(node.children) : [],
  }));
};

const showModal = (mode, data = null) => {
  formRef.value?.resetFields();
  Object.assign(formState, defaultFormState);

  if (mode === 'edit') {
    isEditing.value = true;
    modalTitle.value = '编辑部门';
    const deptToEdit = allUsers.value.find(u => u.departmentId === data.value) || {};
    Object.assign(formState, {
      id: data.value,
      name: data.title.split(' (')[0],
      managerId: deptToEdit.managerId
    });
  } else if (mode === 'create-sub') {
    isEditing.value = false;
    modalTitle.value = `新增子部门 (上级: ${data.title})`;
    formState.parentId = data.value;
  } else {
    isEditing.value = false;
    modalTitle.value = '新增顶级部门';
  }

  modalVisible.value = true;
};

const handleOk = async () => {
  try {
    await formRef.value.validate();
    modalConfirmLoading.value = true;
    if (isEditing.value) {
      await updateDepartment(formState.id, formState);
      message.success('部门更新成功！');
    } else {
      await createDepartment(formState);
      message.success('部门创建成功！');
    }
    modalVisible.value = false;
    await fetchData();
  } catch (error) {
  } finally {
    modalConfirmLoading.value = false;
  }
};

const handleDelete = async (dataRef) => {
  try {
    await deleteDepartment(dataRef.value);
    message.success(`部门 “${dataRef.title}” 删除成功！`);
    await fetchData();
  } catch (error) {}
};
</script>

<style scoped>
.page-container {
  background-color: #fff;
}
.tree-container {
  border: 1px solid #f0f0f0;
  padding: 16px;
  border-radius: 4px;
  min-height: 400px;
}

.tree-node {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.dept-node {
  font-weight: 500;
}

.user-node {
  color: #595959;
  font-weight: normal;
}

/* 【核心修改】调整为始终显示，并优化触摸设备上的体验 */
.node-actions {
  display: inline-flex;
}
@media (hover: hover) and (pointer: fine) {
  .node-actions {
    display: none;
  }
  :deep(.ant-tree-treenode:hover) .node-actions {
    display: inline-flex;
  }
}
:deep(.ant-tree-treenode-selected) .node-actions {
  display: inline-flex;
}


:deep(.ant-tree-node-content-wrapper) {
  padding: 5px 8px !important;
  border-radius: 4px;
}

:deep(.ant-tree-node-content-wrapper.ant-tree-node-selected) {
  background-color: #e6f7ff !important;
}

:deep(.ant-tree-node-content-wrapper.drop-hover) {
  background-color: #bae7ff !important;
}
</style>