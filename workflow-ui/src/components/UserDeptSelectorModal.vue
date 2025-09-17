<template>
  <a-modal
      :open="open"
      title="选择人员"
      :width="isMobile ? '95%' : '800px'"
      :confirm-loading="loading"
      @update:open="(val) => emit('update:open', val)"
      @ok="handleOk"
  >
    <a-row :gutter="16">
      <!-- 【核心修改】为栅格添加响应式断点 -->
      <a-col :xs="24" :sm="8">
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

      <!-- 【核心修改】为栅格添加响应式断点 -->
      <a-col :xs="24" :sm="16" :style="{ marginTop: isMobile ? '16px' : '0' }">
        <a-card size="small" :title="`部门: ${selectedDept || '所有'}`">
          <a-input-search v-model:value="userSearchValue" style="margin-bottom: 8px" placeholder="搜索用户" />
          <a-table
              :columns="columns"
              :data-source="filteredUsers"
              :row-selection="{ type: 'radio', selectedRowKeys, onChange: onSelectChange }"
              row-key="id"
              size="small"
              :pagination="{ pageSize: 10 }"
              :scroll="{ x: 'max-content' }"
          />
        </a-card>
      </a-col>
    </a-row>
  </a-modal>
</template>

<script setup>
import { ref, watch, computed, onMounted, onBeforeUnmount } from 'vue';
import { getOrganizationTree } from '@/api';
import { message } from 'ant-design-vue';

const props = defineProps({ open: Boolean });
const emit = defineEmits(['update:open', 'select']);

const loading = ref(false);
const treeData = ref([]);
const allUsers = ref([]);
const treeSearchValue = ref('');
const userSearchValue = ref('');
const selectedDept = ref(null);

const selectedRowKeys = ref([]);
const selectedUser = ref(null);

// --- 【核心新增】响应式断点逻辑 ---
const isMobile = ref(window.innerWidth < 768);
const handleResize = () => { isMobile.value = window.innerWidth < 768; };
onBeforeUnmount(() => { window.removeEventListener('resize', handleResize); });
// --- 响应式逻辑结束 ---

const columns = [
  { title: '姓名', dataIndex: 'name', key: 'name' },
  { title: '用户ID', dataIndex: 'id', key: 'id' },
];

const fetchTreeData = async () => {
  loading.value = true;
  try {
    const data = await getOrganizationTree();
    treeData.value = data;
    allUsers.value = data.flatMap(dept =>
        (dept.children || []).map(user => ({
          id: user.value,
          name: user.title.split(' (')[0],
          department: dept.value
        }))
    );
  } catch (error) {
    message.error('加载组织架构失败');
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  window.addEventListener('resize', handleResize);
});

watch(() => props.open, (newVal) => {
  if (newVal && treeData.value.length === 0) {
    fetchTreeData();
  }
});

const filteredTreeData = computed(() => {
  if (!treeSearchValue.value) return treeData.value;
  return treeData.value.filter(dept => dept.title.toLowerCase().includes(treeSearchValue.value.toLowerCase()));
});

const filteredUsers = computed(() => {
  let users = allUsers.value;
  if (selectedDept.value) {
    users = users.filter(user => user.department === selectedDept.value);
  }
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
  selectedRowKeys.value = [];
  selectedUser.value = null;
  selectedDept.value = null;
};
</script>