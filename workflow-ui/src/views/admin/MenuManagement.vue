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
              <a-form-item label="路由路径" name="path" help="例如：/wms/inbound"><a-input v-model:value="formState.path" /></a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="关联表单" name="formDefinitionId">
                <a-select v-model:value="formState.formDefinitionId" :options="allForms" :field-names="{label: 'name', value: 'id'}" show-search option-filter-prop="name" />
              </a-form-item>
            </a-col>
          </template>
          <a-col :span="12">
            <a-form-item label="排序号" name="orderNum"><a-input-number v-model:value="formState.orderNum" style="width: 100%;"/></a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="是否可见" name="visible"><a-switch v-model:checked="formState.visible" /></a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="授权角色" name="roleNames">
              <a-select v-model:value="formState.roleNames" mode="multiple" placeholder="选择可以访问此菜单的角色" :options="userStore.allRoles.map(r => ({label: r.description, value: r.name}))" />
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
import { getMenuTree, createMenu, updateMenu, deleteMenu, getForms } from '@/api';
import { useUserStore } from '@/stores/user';
import { message } from 'ant-design-vue';
import { PlusOutlined, AppstoreOutlined } from '@ant-design/icons-vue';
import { iconMap } from '@/utils/iconLibrary.js';
import IconPickerModal from '@/components/IconPickerModal.vue';

const userStore = useUserStore();
const loading = ref(true);
const menuTree = ref([]);
const allForms = ref([]);

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
    allForms.value = await getForms();
  } catch (e) {}
};

onMounted(() => {
  fetchMenus();
  fetchForms();
  if (userStore.allRoles.length === 0) userStore.fetchAllRoles();
});

// Modal state
const modalVisible = ref(false);
const confirmLoading = ref(false);
const isEditing = ref(false);
const formRef = ref();
const formState = reactive({
  id: null, parentId: null, type: 'FORM_ENTRY', name: '', icon: 'AppstoreOutlined',
  path: '', formDefinitionId: null, orderNum: 0, visible: true, roleNames: [],
});
const showIconPicker = ref(false);

const modalTitle = computed(() => isEditing.value ? '编辑菜单' : '新增菜单');
const rules = {
  type: [{ required: true, message: '请选择菜单类型' }],
  name: [{ required: true, message: '请输入菜单名称' }],
  path: [{ required: true, message: '请输入路由路径' }],
  formDefinitionId: [{ required: true, message: '请关联一个表单' }],
};

const menuTreeForSelect = computed(() => {
  const process = (nodes) => {
    return nodes.map(node => ({
      title: node.name, value: node.id,
      // 关键：编辑时，不能选择自己或自己的子孙作为上级
      disabled: isEditing.value && (node.id === formState.id || node.path?.startsWith(formState.path + '/')),
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
    });
  }
  modalVisible.value = true;
};

const onMenuTypeChange = (type) => {
  if (type === 'DIRECTORY') {
    formState.path = undefined;
    formState.formDefinitionId = undefined;
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

// Helper functions
const getMenuTypeText = (type) => ({ 'DIRECTORY': '目录', 'FORM_ENTRY': '表单入口', 'DATA_LIST': '数据列表' }[type] || '未知');
const getMenuTypeTagColor = (type) => ({ 'DIRECTORY': 'blue', 'FORM_ENTRY': 'green', 'DATA_LIST': 'purple' }[type] || 'default');
</script>