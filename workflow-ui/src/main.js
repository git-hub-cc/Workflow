import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import Antd from 'ant-design-vue';

import 'ant-design-vue/dist/reset.css';
import './assets/main.css';
import { useUserStore } from '@/stores/user';
// --- 【核心新增】引入 system store ---
import { useSystemStore } from '@/stores/system';

const app = createApp(App)
const pinia = createPinia();

app.use(pinia);
app.use(Antd);

async function startApp() {
    const userStore = useUserStore();
    // --- 【核心新增】创建 system store 实例 ---
    const systemStore = useSystemStore();

    // --- 【核心新增】在应用启动时获取公共设置 ---
    await systemStore.fetchPublicSettings();

    if (userStore.isAuthenticated) {
        try {
            await userStore.fetchAndSetMenus();
        } catch (error) {
            console.error("Failed to initialize dynamic routes:", error);
            userStore.logout();
        }
    }

    app.use(router);
    app.mount('#app');
}

startApp();