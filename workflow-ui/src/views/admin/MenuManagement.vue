<template>
  <div class="page-container">
    <a-page-header title="菜单管理" sub-title="配置应用导航菜单和权限">
      <template #extra>
        <a-button type="primary" @click="showModal(null)">
          <template #icon><PlusOutlined /></template>
          新增菜单
        </a-button>
      </template>
    </a-page-header>

    <div style="padding: 24px;">
      <a-alert message="提示：拖拽菜单项可以调整其顺序和层级关系。" type="info" show-icon style="margin-bottom: 16px;"/>
      <a-table
          :columns="columns"
          :data-source="menuTree"
          :loading="loading"
          row-key="id"
          :pagination="false"
          :default-expand-all-rows="true"
          :custom-row="customRow"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <component :is="iconMap[record.icon]" v-if="record.icon" style="margin-right: 8px;" />
            <span>{{ record.name }}</span>
          </template>
          <template v-if="column.key === 'type'">
            <a-tag :color="getMenuTypeTagColor(record.type)">{{ getMenuTypeText(record.type) }}</a-tag>
          </template>
          <template v-if="column.key === 'visible'">
            <a-tag :color="record.visible ? 'green' : 'red'">{{ record.visible ? '是' : '否' }}</a-tag>
          </template>
          <template v-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" size="small" @click="showModal(record)">编辑</a-button>
              <a-button type="link" size="small" @click="showModal(null, record.id)">新增子菜单</a-button>
              <a-popconfirm title="确定要删除这个菜单吗？" @confirm="handleDelete(record.id)">
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 新增/编辑弹窗 -->
    <a-modal v-model:open="modalVisible" :title="modalTitle" width="600px" @ok="handleOk" :confirm-loading="confirmLoading">
      <a-form :model="formState" :rules="rules" ref="formRef" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="上级菜单" name="parentId">
              <a-tree-select v-model:value="formState.parentId" :tree-data="menuTreeForSelect" placeholder="不选则为顶级菜单" tree-default-expand-all allow-clear />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="菜单类型" name="type">
              <a-select v-model:value="formState.type" @change="onMenuTypeChange">
                <a-select-option value="DIRECTORY">目录</a-select-option>
                <a-select-option value="FORM_ENTRY">表单入口</a-select-option>
                <a-select-option value="DATA_LIST">数据列表</a-select-option>
                <a-select-option value="REPORT">报表页面</a-select-option>
                <a-select-option value="EXTERNAL_LINK">外部链接</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="菜单名称" name="name"><a-input v-model:value="formState.name" /></a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="菜单图标" name="icon">
              <a-input v-model:value="formState.icon" placeholder="点击选择图标" readonly @click="showIconPicker = true">
                <template #addonAfter>
                  <component :is="iconMap[formState.icon] || iconMap['AppstoreOutlined']" />
                </template>
              </a-input>
            </a-form-item>
          </a-col>
          <template v-if="formState.type !== 'DIRECTORY'">
            <a-col :span="12">
              <a-form-item :label="formState.type === 'EXTERNAL_LINK' ? '链接地址' : '路由路径'" name="path" :help="formState.type === 'EXTERNAL_LINK' ? '例如: https://www.google.com' : '例如: /wms/inbound'">
                <a-input v-model:value="formState.path" />
              </a-form-item>
            </a-col>
            <a-col :span="12" v-if="['FORM_ENTRY', 'DATA_LIST'].includes(formState.type)">
              <a-form-item label="关联表单" name="formDefinitionId">
                <a-select v-model:value="formState.formDefinitionId" :options="allForms" :field-names="{label: 'name', value: 'id'}" show-search option-filter-prop="name" />
              </a-form-item>
            </a-col>
          </template>

          <a-col v-if="formState.type === 'DATA_LIST'" :span="24">
            <a-form-item label="数据范围" name="dataScope" help="定义通过此菜单能看到的数据权限范围">
              <a-select v-model:value="formState.dataScope">
                <a-select-option value="ALL">全部数据 (仅管理员可用)</a-select-option>
                <a-select-option value="BY_DEPARTMENT">按部门 (查看同部门数据)</a-select-option>
                <a-select-option value="BY_GROUP">按用户组 (查看同组数据)</a-select-option>
                <a-select-option value="OWNER_ONLY">仅自己</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>

          <a-col :span="12">
            <a-form-item label="排序号" name="orderNum"><a-input-number v-model:value="formState.orderNum" style="width: 100%;"/></a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="是否可见" name="visible"><a-switch v-model:checked="formState.visible" /></a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="授权角色" name="roleNames">
              <!-- 【已修复】下拉框现在可以从本地 allRoles ref 获取数据 -->
              <a-select v-model:value="formState.roleNames" mode="multiple" placeholder="选择可以访问此菜单的角色" :options="allRoles.map(r => ({label: r.description, value: r.name}))" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
    <IconPickerModal v-model:open="showIconPicker" @select="icon => formState.icon = icon" />
  </div>
</template>

<script setup>
import { ref, onMounted, reactive, computed } from 'vue';
// 【已修复】引入 getRoles API
import { getMenuTree, createMenu, updateMenu, deleteMenu, getForms, updateMenuTree, getRoles } from '@/api';
import { message } from 'ant-design-vue';
import { PlusOutlined } from '@ant-design/icons-vue';
import { iconMap } from '@/utils/iconLibrary.js';
import IconPickerModal from '@/components/IconPickerModal.vue';
import { cloneDeep } from 'lodash-es';

const loading = ref(true);
const menuTree = ref([]);
const allForms = ref([]);
// 【已修复】创建本地 ref 来存储角色列表
const allRoles = ref([]);
let draggedNode = null;

const columns = [
  { title: '菜单名称', key: 'name', dataIndex: 'name' },
  { title: '路由路径', key: 'path', dataIndex: 'path' },
  { title: '类型', key: 'type', dataIndex: 'type', align: 'center' },
  { title: '可见', key: 'visible', dataIndex: 'visible', align: 'center' },
  { title: '排序', key: 'orderNum', dataIndex: 'orderNum', align: 'center' },
  { title: '操作', key: 'actions', width: 220, align: 'center' },
];

const fetchMenus = async () => {
  loading.value = true;
  try {
    menuTree.value = await getMenuTree();
  } catch (e) { /* error handled globally */ } finally {
    loading.value = false;
  }
};

const fetchForms = async () => {
  try {
    const response = await getForms({ page: 0, size: 1000 });
    allForms.value = response.content;
  } catch (e) {}
};

// 【已修复】创建获取角色的函数
const fetchRoles = async () => {
  try {
    const response = await getRoles({ page: 0, size: 1000 });
    allRoles.value = response.content;
  } catch(e) {}
};

onMounted(() => {
  fetchMenus();
  fetchForms();
  fetchRoles(); // 【已修复】在 onMounted 中调用
});

// --- 拖拽排序逻辑 ---
const findNodeAndParent = (key, tree, parent = null) => {
  for (let i = 0; i < tree.length; i++) {
    const node = tree[i];
    if (node.id === key) return { node, parent, index: i };
    if (node.children) {
      const found = findNodeAndParent(key, node.children, node);
      if (found) return found;
    }
  }
  return null;
};

const customRow = (record) => ({
  draggable: true,
  ondragstart: (e) => {
    e.stopPropagation();
    draggedNode = record;
  },
  ondragover: (e) => {
    e.preventDefault();
    e.stopPropagation();
  },
  ondrop: async (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (!draggedNode || draggedNode.id === record.id) return;

    loading.value = true;
    const newTree = cloneDeep(menuTree.value);

    const sourceInfo = findNodeAndParent(draggedNode.id, newTree);
    if (!sourceInfo) { loading.value = false; return; }
    const sourceList = sourceInfo.parent ? sourceInfo.parent.children : newTree;
    sourceList.splice(sourceInfo.index, 1);

    const targetInfo = findNodeAndParent(record.id, newTree);
    if (!targetInfo) { loading.value = false; return; }
    const targetList = targetInfo.parent ? targetInfo.parent.children : newTree;
    targetList.splice(targetInfo.index, 0, draggedNode);

    draggedNode.parentId = targetInfo.parent ? targetInfo.parent.id : null;

    try {
      await updateMenuTree(newTree);
      message.success('菜单顺序已更新！');
      await fetchMenus();
    } catch(err) {
      // global handler
    } finally {
      loading.value = false;
      draggedNode = null;
    }
  },
});


// Modal state
const modalVisible = ref(false);
const confirmLoading = ref(false);
const isEditing = ref(false);
const formRef = ref();
const formState = reactive({
  id: null, parentId: null, type: 'FORM_ENTRY', name: '', icon: 'AppstoreOutlined',
  path: '', formDefinitionId: null, orderNum: 0, visible: true, roleNames: [],
  dataScope: 'ALL',
});
const showIconPicker = ref(false);

const modalTitle = computed(() => isEditing.value ? '编辑菜单' : '新增菜单');
const rules = {
  type: [{ required: true, message: '请选择菜单类型' }],
  name: [{ required: true, message: '请输入菜单名称' }],
  path: [{ required: true, message: '请输入路径或链接' }],
  formDefinitionId: [{ required: true, message: '请关联一个表单' }],
  dataScope: [{ required: true, message: '请选择数据范围' }],
};

const menuTreeForSelect = computed(() => {
  const process = (nodes) => {
    return nodes.map(node => ({
      title: node.name, value: node.id,
      disabled: isEditing.value && (node.id === formState.id || (node.path && formState.path && node.path.startsWith(formState.path + '/'))),
      children: node.children ? process(node.children) : []
    }));
  };
  return process(menuTree.value);
});


const showModal = (menu, parentId = null) => {
  if (menu) {
    isEditing.value = true;
    Object.assign(formState, menu);
  } else {
    isEditing.value = false;
    Object.assign(formState, {
      id: null, parentId: parentId, type: 'FORM_ENTRY', name: '', icon: 'AppstoreOutlined',
      path: '', formDefinitionId: null, orderNum: 0, visible: true, roleNames: [],
      dataScope: 'ALL',
    });
  }
  modalVisible.value = true;
};

const onMenuTypeChange = (type) => {
  if (type === 'DIRECTORY') {
    formState.path = undefined;
    formState.formDefinitionId = undefined;
    formState.dataScope = undefined;
  } else if (type === 'FORM_ENTRY') {
    formState.dataScope = undefined;
  } else {
    formState.dataScope = 'ALL';
  }
};

const handleOk = async () => {
  try {
    await formRef.value.validate();
    confirmLoading.value = true;
    if (isEditing.value) {
      await updateMenu(formState.id, formState);
      message.success('更新成功');
    } else {
      await createMenu(formState);
      message.success('创建成功');
    }
    modalVisible.value = false;
    await fetchMenus();
  } catch (e) {
    console.error(e);
  } finally {
    confirmLoading.value = false;
  }
};

const handleDelete = async (id) => {
  try {
    await deleteMenu(id);
    message.success('删除成功');
    await fetchMenus();
  } catch (e) {}
};

const getMenuTypeText = (type) => ({ 'DIRECTORY': '目录', 'FORM_ENTRY': '表单入口', 'DATA_LIST': '数据列表', 'REPORT': '报表', 'EXTERNAL_LINK': '外链' }[type] || '未知');
const getMenuTypeTagColor = (type) => ({ 'DIRECTORY': 'blue', 'FORM_ENTRY': 'green', 'DATA_LIST': 'purple', 'REPORT': 'orange', 'EXTERNAL_LINK': 'cyan' }[type] || 'default');
</script>