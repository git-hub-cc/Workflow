<template>
  <a-modal
      :open="open"
      title="选择人员"
      width="800px"
      :confirm-loading="loading"
      @update:open="(val) => emit('update:open', val)"
      @ok="handleOk"
  >
    <a-row :gutter="16">
      <!-- 左侧部门树 -->
      <a-col :span="8">
        <a-card size="small" title="组织架构">
          <a-input-search v-model:value="treeSearchValue" style="margin-bottom: 8px" placeholder="搜索部门" />
          <a-tree
              v-if="treeData.length > 0"
              :tree-data="filteredTreeData"
              @select="handleTreeSelect"
              :default-expand-all="true"
          />
          <a-empty v-else-if="!loading" />
          <a-spin v-if="loading" />
        </a-card>
      </a-col>

      <!-- 右侧用户列表 -->
      <a-col :span="16">
        <a-card size="small" :title="`部门: ${selectedDept || '所有'}`">
          <a-input-search v-model:value="userSearchValue" style="margin-bottom: 8px" placeholder="搜索用户" />
          <a-table
              :columns="columns"
              :data-source="filteredUsers"
              :row-selection="{ type: 'radio', selectedRowKeys, onChange: onSelectChange }"
              row-key="id"
              size="small"
              :pagination="{ pageSize: 10 }"
          />
        </a-card>
      </a-col>
    </a-row>
  </a-modal>
</template>

<script setup>
import { ref, watch, computed } from 'vue';
import { getOrganizationTree } from '@/api';
import { message } from 'ant-design-vue';

const props = defineProps({ open: Boolean });
const emit = defineEmits(['update:open', 'select']);

const loading = ref(false);
const treeData = ref([]); // 完整的组织树数据
const allUsers = ref([]); // 扁平化的所有用户列表
const treeSearchValue = ref('');
const userSearchValue = ref('');
const selectedDept = ref(null);

const selectedRowKeys = ref([]);
const selectedUser = ref(null);

const columns = [
  { title: '姓名', dataIndex: 'name', key: 'name' },
  { title: '用户ID', dataIndex: 'id', key: 'id' },
];

// 获取并处理组织架构数据
const fetchTreeData = async () => {
  loading.value = true;
  try {
    const data = await getOrganizationTree();
    treeData.value = data;
    // 同时扁平化所有用户到一个列表中
    allUsers.value = data.flatMap(dept =>
        (dept.children || []).map(user => ({
          id: user.value,
          name: user.title.split(' (')[0], // 从 "张三 (user001)" 中提取 "张三"
          department: dept.value
        }))
    );
  } catch (error) {
    message.error('加载组织架构失败');
  } finally {
    loading.value = false;
  }
};

// 弹窗打开时加载数据
watch(() => props.open, (newVal) => {
  if (newVal && treeData.value.length === 0) {
    fetchTreeData();
  }
});

// 过滤部门树
const filteredTreeData = computed(() => {
  if (!treeSearchValue.value) return treeData.value;
  return treeData.value.filter(dept => dept.title.toLowerCase().includes(treeSearchValue.value.toLowerCase()));
});

// 过滤用户列表
const filteredUsers = computed(() => {
  let users = allUsers.value;
  // 1. 按部门过滤
  if (selectedDept.value) {
    users = users.filter(user => user.department === selectedDept.value);
  }
  // 2. 按关键词过滤
  if (userSearchValue.value) {
    const keyword = userSearchValue.value.toLowerCase();
    users = users.filter(user => user.name.toLowerCase().includes(keyword) || user.id.toLowerCase().includes(keyword));
  }
  return users;
});


const handleTreeSelect = (selectedKeys, { node }) => {
  if (node.dataRef.type === 'department') {
    selectedDept.value = node.dataRef.value;
  }
};

const onSelectChange = (keys, rows) => {
  selectedRowKeys.value = keys;
  selectedUser.value = rows[0];
};

const handleOk = () => {
  if (!selectedUser.value) {
    message.warn('请选择一个人员');
    return;
  }
  emit('select', selectedUser.value);
  emit('update:open', false);
  // 重置状态
  selectedRowKeys.value = [];
  selectedUser.value = null;
  selectedDept.value = null;
};
</script>