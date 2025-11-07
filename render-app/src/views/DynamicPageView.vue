<template>
  <div v-if="loading" class="page-loading">
    <a-spin size="large" tip="页面加载中..." />
  </div>
  <div v-else-if="error" class="page-error">
    <a-result
        :status="error.statusCode === 404 ? '404' : '500'"
        :title="error.statusCode"
        :sub-title="error.message"
    >
      <template #extra>
        <router-link to="/">
          <a-button type="primary">返回首页</a-button>
        </router-link>
      </template>
    </a-result>
  </div>
  <PageRenderer v-else :schema="pageSchema" :business-data="businessData" />
</template>

<script setup>
import { ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';
import PageRenderer from '../components/PageRenderer.vue';

const route = useRoute();
const router = useRouter();

const loading = ref(true);
const error = ref(null);
const pageSchema = ref(null);
const businessData = ref(null);

const fetchDataForPage = async (pathSegments) => {
  loading.value = true;
  error.value = null;
  pageSchema.value = null;
  businessData.value = null;

  try {
    // 将路径数组转为API友好的key，例如 [] -> 'homepage', ['products', 'p001'] -> 'products/p001'
    const pageKey = Array.isArray(pathSegments) && pathSegments.length > 0 ? pathSegments.join('/') : 'homepage';

    // 决定页面结构模板的Key。对于详情页，我们可能使用模板。
    // 例如 /products/p001 使用 products/detail-template
    // 这是一个简化的逻辑，真实项目可能更复杂。
    let schemaKey = pageKey;
    if (pageKey.startsWith('products/')) {
      schemaKey = 'products/detail-template';
    }

    console.log(`Fetching data for pageKey: ${pageKey}, using schemaKey: ${schemaKey}`);

    // 1. 并行获取页面设计图 (Schema) 和业务数据
    const schemaPromise = axios.get(`/api/schemas/${schemaKey}`);
    let dataPromise = Promise.resolve({ data: null }); // 默认没有业务数据

    // 2. 根据页面路由，决定需要获取哪个业务数据
    if (pageKey.startsWith('products/')) {
      const productId = pathSegments[1];
      if (productId) {
        dataPromise = axios.get(`/api/products/${productId}`);
      }
    }
    // 您可以在这里添加其他业务数据的获取逻辑，例如文章
    // else if (pageKey.startsWith('articles/')) { ... }

    const [schemaResponse, businessResponse] = await Promise.all([schemaPromise, dataPromise]);

    pageSchema.value = JSON.parse(schemaResponse.data.schemaJson);
    businessData.value = businessResponse.data;

    // 更新浏览器标签页标题
    document.title = schemaResponse.data.name || 'My Awesome Site';

  } catch (e) {
    console.error("加载页面数据失败:", e);
    const statusCode = e.response?.status || 500;

    // 如果是404，跳转到404页面
    if (statusCode === 404) {
      router.replace({ name: 'NotFound', query: { from: route.fullPath } });
      return;
    }

    error.value = {
      statusCode,
      message: e.response?.data?.message || '页面加载时发生服务器错误',
    };
  } finally {
    loading.value = false;
  }
};

// 监听路由参数的变化，以便在页面间导航时重新获取数据
watch(
    () => route.params.pathMatch,
    (newPath) => {
      // Vue Router 将路径解析为数组，例如 /products/p001 -> ['products', 'p001']
      fetchDataForPage(newPath);
    },
    { immediate: true, deep: true }
);
</script>

<style scoped>
.page-loading, .page-error {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 80vh;
}
</style>