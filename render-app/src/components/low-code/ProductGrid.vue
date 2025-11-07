<template>
  <div class="product-grid-container">
    <h2 v-if="resolvedTitle" class="grid-title">{{ resolvedTitle }}</h2>
    <div v-if="loading" class="grid-loading">
      <a-spin />
    </div>
    <div v-else-if="error" class="grid-error">
      <a-alert :message="error" type="error" show-icon />
    </div>
    <a-row v-else :gutter="[16, 16]">
      <a-col v-for="product in products" :key="product.id" :xs="24" :sm="12" :md="8" :lg="6">
        <router-link :to="`/products/${product.id}`">
          <a-card hoverable class="product-card">
            <template #cover>
              <img :alt="product.name" :src="product.mainImageUrl" class="product-image" />
            </template>
            <a-card-meta :title="product.name">
              <template #description>
                <div class="product-price">¥{{ product.price }}</div>
              </template>
            </a-card-meta>
          </a-card>
        </router-link>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import axios from 'axios';
import { resolveBinding } from '@/utils/bindingResolver';

const props = defineProps({
  title: String,
  category: String,
  count: Number,
  businessData: Object,
});

const loading = ref(true);
const error = ref(null);
const products = ref([]);

const resolvedTitle = computed(() => resolveBinding(props.title, props.businessData));
const resolvedCategory = computed(() => resolveBinding(props.category, props.businessData));
const resolvedCount = computed(() => resolveBinding(props.count, props.businessData) || 4);

const fetchProducts = async () => {
  loading.value = true;
  error.value = null;
  try {
    const params = {
      category: resolvedCategory.value,
      // limit: resolvedCount.value, // 假设API支持limit参数
    };
    const response = await axios.get('/api/products', { params });
    // 在客户端进行截断，因为模拟API不支持limit
    products.value = response.data.slice(0, resolvedCount.value);
  } catch (e) {
    error.value = '加载商品列表失败';
    console.error(e);
  } finally {
    loading.value = false;
  }
};

// 监听props变化，以便在设计器中实时预览
watch(
    [() => props.category, () => props.count],
    () => {
      fetchProducts();
    },
    { immediate: true }
);

</script>

<style scoped>
.product-grid-container {
  padding: 40px 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.grid-title {
  text-align: center;
  font-size: 2rem;
  margin-bottom: 30px;
}

.product-card {
  transition: all 0.3s ease;
}
.product-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.product-image {
  height: 200px;
  object-fit: cover;
}

.product-price {
  font-size: 1.2rem;
  color: #f5222d;
  font-weight: 500;
  margin-top: 8px;
}

.grid-loading, .grid-error {
  min-height: 200px;
  display: flex;
  justify-content: center;
  align-items: center;
}
</style>