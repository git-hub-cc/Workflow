<template>
  <div class="page-renderer">
    <div v-for="(component, index) in schema.components" :key="component.id || index">
      <!--
        使用Vue的动态组件 <component>
        - `:is` 绑定到 componentMap 中对应的组件
        - `v-bind` 将 schema 中定义的所有 props 传递给子组件
        - `business-data` 将整个页面的业务数据传递下去，供子组件内部进行数据绑定
      -->
      <component
          :is="componentMap[component.type]"
          v-if="componentMap[component.type]"
          :business-data="businessData"
          v-bind="component.props"
      />
      <!-- 降级处理：如果找不到对应的组件，则显示一个提示 -->
      <div v-else class="unknown-component">
        未知的组件类型: {{ component.type }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineAsyncComponent } from 'vue';

const props = defineProps({
  schema: {
    type: Object,
    required: true,
  },
  businessData: {
    type: Object,
    default: () => null,
  },
});

// 组件映射表
// Key: schema 中定义的组件类型 (e.g., "hero-banner")
// Value: 使用 defineAsyncComponent 异步加载对应的 Vue 组件
// 这样做可以实现代码分割，只有当页面实际用到某个组件时，才会去加载它的代码。
const componentMap = {
  'hero-banner': defineAsyncComponent(() => import('./low-code/HeroBanner.vue')),
  'product-grid': defineAsyncComponent(() => import('./low-code/ProductGrid.vue')),
  'rich-text': defineAsyncComponent(() => import('./low-code/RichText.vue')),
  'image-box': defineAsyncComponent(() => import('./low-code/ImageBox.vue')),
  // ... 在这里注册您所有的低代码物料组件
};
</script>

<style scoped>
.unknown-component {
  background-color: #fffbe6;
  border: 1px solid #ffe58f;
  padding: 16px;
  margin: 16px;
  text-align: center;
  color: #faad14;
  border-radius: 4px;
}
</style>