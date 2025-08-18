<template>
  <div class="page-container">
    <a-page-header title="表单管理" sub-title="管理所有已创建的业务表单">
      <template #extra>
        <!-- 【核心修改】导航到具名的创建路由 -->
        <a-button type="primary" @click="$router.push({ name: 'form-builder-create' })">
          <template #icon><PlusOutlined /></template>
          新建表单
        </a-button>
      </template>
    </a-page-header>

    <div style="padding: 24px;">
      <a-table
          :columns="columns"
          :data-source="forms"
          :loading="loading"
          row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'createdAt' || column.key === 'updatedAt'">
            {{ new Date(record[column.dataIndex]).toLocaleString() }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <!-- 编辑：次要操作，使用 link 按钮 -->
              <a-button type="link" size="small" @click="goToBuilder(record.id)">
                编辑
              </a-button>
              <!-- 设计流程：主要操作，使用 primary 按钮 -->
              <a-button type="primary" size="small" @click="goToDesigner(record.id)">
                设计流程
              </a-button>
              <!-- 查看数据：次要操作，使用 link 按钮 -->
              <a-button type="link" size="small" @click="viewSubmissions(record.id)">
                查看数据
              </a-button>
              <a-popconfirm
                  title="确定要删除这个表单吗？"
                  content="删除后将无法恢复，请确保没有流程或菜单正在使用此表单。"
                  ok-text="确认删除"
                  cancel-text="取消"
                  @confirm="handleDelete(record.id)"
              >
                <!-- 删除：危险操作，使用 link danger 按钮 -->
                <a-button type="link" danger size="small">删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getForms, deleteForm } from '@/api';
import { message } from 'ant-design-vue';
import { PlusOutlined } from '@ant-design/icons-vue';

const router = useRouter();
const loading = ref(true);
const forms = ref([]);

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '表单名称', dataIndex: 'name', key: 'name' },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 200 },
  { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 200 },
  { title: '操作', key: 'actions', align: 'center', width: 320 },
];

const fetchForms = async () => {
  loading.value = true;
  try {
    forms.value = await getForms();
  } catch (error) {
    // 错误已全局处理
  } finally {
    loading.value = false;
  }
};

onMounted(fetchForms);

/**
 * 【新增】跳转到表单设计器（编辑模式）
 */
const goToBuilder = (formId) => {
  router.push({ name: 'form-builder-edit', params: { formId } });
};

const goToDesigner = (formId) => {
  router.push({ name: 'workflow-designer', params: { formId } });
};

const viewSubmissions = (formId) => {
  router.push({ name: 'form-submissions', params: { formId } });
};

const handleDelete = async (formId) => {
  try {
    await deleteForm(formId);
    message.success('表单删除成功！');
    await fetchForms(); // 重新加载列表
  } catch (error) {
    // 错误已全局处理
  }
};
</script>

<style scoped>
.page-container {
  background-color: #fff;
  border-radius: 4px;
}
</style>