<template>
  <div class="page-container">
    <a-page-header title="页面管理" sub-title="管理所有通过低代码设计的C端页面">
      <template #extra>
        <a-button type="primary" @click="$router.push({ name: 'page-designer-create' })">
          <template #icon><PlusOutlined /></template>
          新建页面
        </a-button>
      </template>
    </a-page-header>

    <div class="content-padding">
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
          <template v-if="column.key === 'pageKey'">
            <a :href="`http://localhost:3000/${record.pageKey}`" target="_blank" title="在新窗口中预览">
              /{{ record.pageKey }} <ExportOutlined />
            </a>
          </template>
          <template v-if="column.key === 'updatedAt'">
            {{ new Date(record.updatedAt).toLocaleString() }}
          </template>
          <template v-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" size="small" @click="editPage(record.id)">编辑</a-button>
              <a-popconfirm
                  title="确定要删除这个页面吗？"
                  content="此操作不可恢复。"
                  @confirm="handleDelete(record.id)"
              >
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
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { usePaginatedFetch } from '@/composables/usePaginatedFetch';
import { message } from 'ant-design-vue';
import { PlusOutlined, ExportOutlined } from '@ant-design/icons-vue';
// 【权限修复】从 api/index.js 导入新的、正确的 API 函数
import { getPageSchemas, deletePageSchema } from '@/api';

const router = useRouter();

// 【权限修复】将 usePaginatedFetch 的第一个参数从 axios 直接调用改为导入的 API 函数
const {
  loading,
  dataSource,
  pagination,
  handleTableChange,
  fetchData,
} = usePaginatedFetch(getPageSchemas, {}, { defaultSort: 'updatedAt,desc' });

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '页面名称', dataIndex: 'name', key: 'name' },
  { title: '页面路径 (Key)', dataIndex: 'pageKey', key: 'pageKey' },
  { title: '最后更新时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 200 },
  { title: '操作', key: 'actions', align: 'center', width: 180 },
];

onMounted(fetchData);

const editPage = (schemaId) => {
  router.push({ name: 'page-designer-edit', params: { schemaId } });
};

const handleDelete = async (schemaId) => {
  try {
    // 【权限修复】使用导入的 API 函数
    await deletePageSchema(schemaId);
    message.success('页面删除成功！');
    fetchData(); // 重新加载数据
  } catch (error) {
    // 错误已由全局拦截器处理，无需额外提示
  }
};
</script>

<style scoped>
.page-container {
  background-color: #fff;
}
</style>