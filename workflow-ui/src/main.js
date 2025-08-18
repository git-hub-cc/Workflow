import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import Antd from 'ant-design-vue';

import 'ant-design-vue/dist/reset.css';
import './assets/main.css';
import { useUserStore } from '@/stores/user'; // 1. 引入 user store

const app = createApp(App)
const pinia = createPinia(); // 2. 创建 Pinia 实例

app.use(pinia); // 3. 提前注册 Pinia
app.use(Antd);

// 4. 定义一个异步启动函数
async function startApp() {
    const userStore = useUserStore();

    // 5. 如果用户在刷新页面时是已登录状态
    if (userStore.isAuthenticated) {
        try {
            // 在挂载应用前，先获取菜单并添加动态路由
            await userStore.fetchAndSetMenus();
        } catch (error) {
            // 如果获取失败（例如 token 过期），则登出
            console.error("Failed to initialize dynamic routes:", error);
            userStore.logout();
        }
    }

    // 6. 在所有异步操作完成后，再注册路由并挂载应用
    app.use(router);
    app.mount('#app');
}

// 7. 执行启动函数
startApp();