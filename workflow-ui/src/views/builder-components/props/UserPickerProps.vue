<template>
  <a-form-item label="提示">
    <a-typography-text type="secondary">
      此组件将从系统中所有用户列表进行选择。
    </a-typography-text>
  </a-form-item>
</template>

<script setup>
defineProps(['field']);
</script>