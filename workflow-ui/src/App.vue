<template>
  <!-- 使用 a-config-provider 包裹，并绑定动态主题 -->
  <a-config-provider :locale="zhCN" :theme="themeConfig">
    <router-view />
  </a-config-provider>
</template>

<script setup>
import { computed, watchEffect } from 'vue';
import { ConfigProvider as AConfigProvider } from 'ant-design-vue';
import zhCN from 'ant-design-vue/es/locale/zh_CN';
// --- 引入 system store ---
import { useSystemStore } from '@/stores/system';

const systemStore = useSystemStore();

// --- 【核心修改】创建动态主题配置，并增加有效性校验 ---
const themeConfig = computed(() => {
  const themeColor = systemStore.settings.THEME_COLOR;
  // 校验 themeColor 是否为有效的十六进制颜色字符串
  if (themeColor && /^#([0-9A-Fa-f]{3}){1,2}$/.test(themeColor)) {
    return {
      token: {
        colorPrimary: themeColor,
      },
    };
  }
  // 如果颜色值无效或为空，返回空对象以使用 antd-vue 的默认主题
  return {};
});

// --- 使用 watchEffect 实时应用系统设置 ---
watchEffect(() => {
  const settings = systemStore.settings;

  // 1. 更新浏览器标签页标题
  if (settings.SYSTEM_NAME) {
    document.title = settings.SYSTEM_NAME;
  }

  // 2. 更新 Favicon
  const favicon = document.querySelector("link[rel~='icon']");
  if (favicon) {
    // 【核心修改】直接使用 store 中已经生成好的安全 blob URL，避免重复请求
    if (systemStore.iconBlobUrl) {
      favicon.href = systemStore.iconBlobUrl;
    } else {
      favicon.href = '/favicon.ico'; // 默认图标
    }
  }
});
</script>

<style>
/* 这里可以留空，或者放一些全局样式 */
</style>