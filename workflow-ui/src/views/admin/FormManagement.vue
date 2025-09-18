<template>
  <div class="page-container">
    <a-page-header title="表单管理" sub-title="管理所有已创建的业务表单">
      <template #extra>
        <a-button type="primary" @click="$router.push({ name: 'form-builder-create' })">
          <template #icon><PlusOutlined /></template>
          新建表单
        </a-button>
      </template>
    </a-page-header>

    <!-- 【样式完善】使用 content-padding 和 responsive-filter-form class -->
    <div class="content-padding">
      <a-card :bordered="false" style="margin-bottom: 24px;">
        <a-form :model="filterState" layout="inline" class="responsive-filter-form">
          <a-form-item label="表单名称">
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
          :scroll="{ x: 'max-content' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'createdAt' || column.key === 'updatedAt'">
            {{ new Date(record[column.dataIndex]).toLocaleString() }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" size="small" @click="goToBuilder(record.id)">
                编辑
              </a-button>
              <a-button type="primary" size="small" @click="goToDesigner(record.id)">
                设计流程
              </a-button>
              <a-button type="link" size="small" @click="viewSubmissions(record.id)">
                查看数据
              </a-button>
              <a-button type="link" danger size="small" @click="showDeleteModal(record)">删除</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <a-modal
        v-model:open="deleteModalInfo.visible"
        :title="`删除确认：${deleteModalInfo.formName}`"
        :confirm-loading="deleteModalInfo.loading"
        @cancel="closeDeleteModal"
    >
      <template #footer>
        <a-button key="back" @click="closeDeleteModal">取消</a-button>
        <a-button
            key="submit"
            type="primary"
            danger
            :loading="deleteModalInfo.loading"
            :disabled="!cascadeDeleteConfirmed && deleteModalInfo.canCascade"
            @click="handleCascadeDelete"
        >
          确认删除
        </a-button>
      </template>

      <a-alert
          message="无法删除此表单"
          :description="`表单 “${deleteModalInfo.formName}” 仍被以下项目使用，请先解除关联：`"
          type="error"
          show-icon
      />
      <a-list :data-source="deleteModalInfo.dependencies" size="small" style="margin-top: 16px;">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #avatar>
                <a-avatar style="background-color: #f56a00"><component :is="getDependencyIcon(item.type)" /></a-avatar>
              </template>
              <template #title>
                <a @click="navigateToDependency(item)" :title="`跳转到 ${item.name}`">{{ item.name }}</a>
              </template>
              <template #description>
                {{ getDependencyDescription(item) }}
              </template>
            </a-list-item-meta>
          </a-list-item>
        </template>
      </a-list>
      <div v-if="deleteModalInfo.canCascade" style="margin-top: 24px; border-top: 1px solid #f0f0f0; padding-top: 16px;">
        <a-checkbox v-model:checked="cascadeDeleteConfirmed">
          <span style="color: #cf1322; font-weight: bold;">我已了解风险，并确认要一并删除所有关联的 <strong>工作流模板</strong> 和 <strong>全部提交数据</strong>。此操作不可恢复！</span>
        </a-checkbox>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import {ref, onMounted, reactive, h} from 'vue';
import { useRouter } from 'vue-router';
import { getForms, deleteForm, getFormDependencies } from '@/api';
import { usePaginatedFetch } from '@/composables/usePaginatedFetch';
import { message, Modal } from 'ant-design-vue';
import {
  PlusOutlined,
  ExclamationCircleOutlined,
  ForkOutlined,
  MenuOutlined,
  DatabaseOutlined,
  SearchOutlined,
  ReloadOutlined
} from '@ant-design/icons-vue';

const router = useRouter();

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
    getForms,
    { name: '' },
    { defaultSort: 'updatedAt,desc' }
);

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80, fixed: 'left' },
  { title: '表单名称', dataIndex: 'name', key: 'name', sorter: true },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 200, sorter: true },
  { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 200, sorter: true },
  { title: '操作', key: 'actions', align: 'center', width: 320, fixed: 'right' },
];

onMounted(fetchData);

const goToBuilder = (formId) => router.push({ name: 'form-builder-edit', params: { formId } });
const goToDesigner = (formId) => router.push({ name: 'workflow-designer', params: { formId } });
const viewSubmissions = (formId) => router.push({ name: 'form-submissions', params: { formId } });

const deleteModalInfo = reactive({
  visible: false,
  loading: false,
  formId: null,
  formName: '',
  dependencies: [],
  canCascade: false,
});
const cascadeDeleteConfirmed = ref(false);

const showDeleteModal = async (record) => {
  deleteModalInfo.loading = true;
  try {
    const res = await getFormDependencies(record.id);
    if (res.canDelete) {
      confirmSimpleDelete(record);
    } else {
      deleteModalInfo.formId = record.id;
      deleteModalInfo.formName = record.name;
      deleteModalInfo.dependencies = res.dependencies;
      deleteModalInfo.canCascade = !res.dependencies.some(d => d.type === 'MENU');
      deleteModalInfo.visible = true;
    }
  } catch(e) {
  } finally {
    deleteModalInfo.loading = false;
  }
};

const confirmSimpleDelete = (record) => {
  Modal.confirm({
    title: `确定要删除表单 “${record.name}” 吗？`,
    icon: h(ExclamationCircleOutlined),
    content: '此操作不可恢复。',
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await deleteForm(record.id, false);
        message.success('表单删除成功！');
        await fetchData();
      } catch (error) {}
    },
  });
};

const handleCascadeDelete = async () => {
  deleteModalInfo.loading = true;
  try {
    await deleteForm(deleteModalInfo.formId, true);
    message.success(`表单 “${deleteModalInfo.formName}” 及其关联数据已成功删除！`);
    closeDeleteModal();
    await fetchData();
  } catch (error) {
  } finally {
    deleteModalInfo.loading = false;
  }
};

const closeDeleteModal = () => {
  deleteModalInfo.visible = false;
  deleteModalInfo.dependencies = [];
  deleteModalInfo.formId = null;
  cascadeDeleteConfirmed.value = false;
};

const getDependencyIcon = (type) => {
  const map = { WORKFLOW: ForkOutlined, MENU: MenuOutlined, SUBMISSION: DatabaseOutlined };
  return map[type];
};

const getDependencyDescription = (item) => {
  const map = {
    WORKFLOW: `ID: ${item.id}`,
    MENU: `ID: ${item.id}`,
    SUBMISSION: `共 ${item.count} 条记录`
  };
  return map[item.type];
};

const navigateToDependency = (item) => {
  if (item.type === 'WORKFLOW') {
    goToDesigner(deleteModalInfo.formId);
  } else if (item.type === 'MENU') {
    router.push({ name: 'admin-menus' });
  }
  closeDeleteModal();
};

</script>

<style scoped>
.page-container {
  background-color: #fff;
  border-radius: 4px;
}
</style>