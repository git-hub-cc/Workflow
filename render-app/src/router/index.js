import { createRouter, createWebHistory } from 'vue-router';
import DynamicPageView from '../views/DynamicPageView.vue';
import NotFoundView from '../views/NotFoundView.vue';

const routes = [
    // 捕获所有路径的动态路由。这是渲染低代码页面的核心。
    // 注意：它应该放在路由列表的末尾，以避免覆盖其他静态路由。
    {
        path: '/:pathMatch(.*)*',
        name: 'DynamicPage',
        component: DynamicPageView,
        props: true // 将路由参数作为 props 传递给组件
    },
    // 你也可以在这里定义一些不通过低代码管理的静态页面
    // { path: '/login', component: () => import('../views/Login.vue') },
    // { path: '/404', name: 'NotFound', component: NotFoundView },
];

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes,
    // 每次路由切换时，滚动到页面顶部
    scrollBehavior(to, from, savedPosition) {
        if (savedPosition) {
            return savedPosition;
        } else {
            return { top: 0 };
        }
    },
});

export default router;