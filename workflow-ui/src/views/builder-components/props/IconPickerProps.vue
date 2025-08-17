<template>
  <div>
    <!-- 图标选择器目前只使用通用属性 -->
    <GenericProps :field="field" :all-fields="allFields" />
  </div>
</template>

<script setup>
import GenericProps from './GenericProps.vue';

// 定义 props 以接收来自父组件的数据
defineProps({
  field: {
    type: Object,
    required: true,
  },
  allFields: {
    type: Array,
    required: true,
  },
});
</script>