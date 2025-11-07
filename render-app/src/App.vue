<template>
  <a-config-provider
      :theme="{
      token: {
        colorPrimary: '#00b96b',
      },
    }"
  >
    <router-view />
  </a-config-provider>
</template>

<script setup>
// 这里可以放置一些全局逻辑，例如获取用户信息等
</script>

<style>
/* 全局样式 */
body {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial,
  'Noto Sans', sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol',
  'Noto Color Emoji';
}
</style>