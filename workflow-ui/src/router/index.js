import { createRouter, createWebHistory } from 'vue-router';
import AppLayout from '@/components/AppLayout.vue';
import { useUserStore } from '@/stores/user';
import { message } from "ant-design-vue";

// --- 1. 静态路由定义 ---
const staticRoutes = [
    {
        path: '/login',
        name: 'login',
        component: () => import('../views/Login.vue'),
        meta: { title: '登录' }
    },
    {
        path: '/',
        name: 'layout',
        component: AppLayout,
        meta: { requiresAuth: true },
        children: [
            // 基础页面
            { path: '', name: 'home', component: () => import('../views/Home.vue'), meta: { title: '首页' } },
            { path: 'profile', name: 'profile', component: () => import('../views/Profile.vue'), meta: { title: '个人设置' } },
            // 管理员专属的静态页面
            { path: 'admin/dashboard', name: 'admin-dashboard', component: () => import('../views/admin/Dashboard.vue'), meta: { title: '仪表盘', requiresAdmin: true } },
            { path: 'admin/forms', name: 'admin-forms', component: () => import('../views/admin/FormManagement.vue'), meta: { title: '表单管理', requiresAdmin: true } },
            { path: 'admin/users', name: 'admin-users', component: () => import('../views/admin/UserManagement.vue'), meta: { title: '用户管理', requiresAdmin: true } },
            { path: 'admin/roles', name: 'admin-roles', component: () => import('../views/admin/RoleManagement.vue'), meta: { title: '角色管理', requiresAdmin: true } },
            { path: 'admin/groups', name: 'admin-groups', component: () => import('../views/admin/UserGroupManagement.vue'), meta: { title: '用户组管理', requiresAdmin: true } },
            { path: 'admin/menus', name: 'admin-menus', component: () => import('../views/admin/MenuManagement.vue'), meta: { title: '菜单管理', requiresAdmin: true } },
            { path: 'admin/instances', name: 'admin-instances', component: () => import('../views/admin/InstanceManagement.vue'), meta: { title: '实例管理', requiresAdmin: true } },
            { path: 'admin/org-chart', name: 'admin-org-chart', component: () => import('../views/admin/OrganizationChart.vue'), meta: { title: '组织架构图', requiresAdmin: true } },
            { path: 'admin/org-management', name: 'admin-org-management', component: () => import('../views/admin/OrganizationManagement.vue'), meta: { title: '组织架构管理', requiresAdmin: true } },
            { path: 'admin/logs/login', name: 'admin-login-log', component: () => import('../views/admin/LoginLog.vue'), meta: { title: '登录日志', requiresAdmin: true } },
            { path: 'admin/logs/operation', name: 'admin-operation-log', component: () => import('../views/admin/OperationLog.vue'), meta: { title: '操作日志', requiresAdmin: true } },
            // --- 【核心新增】系统设置路由 ---
            { path: 'admin/settings', name: 'admin-settings', component: () => import('../views/admin/SystemSettings.vue'), meta: { title: '系统设置', requiresAdmin: true } },

            {
                path: 'form/builder',
                name: 'form-builder-create',
                component: () => import('../views/FormBuilder.vue'),
                meta: { title: '新建表单', requiresAdmin: true }
            },
            {
                path: 'form/builder/:formId',
                name: 'form-builder-edit',
                component: () => import('../views/FormBuilder.vue'),
                props: true,
                meta: { title: '编辑表单', requiresAdmin: true }
            },
            {
                path: 'form/viewer/:formId',
                name: 'form-viewer',
                component: () => import('../views/FormViewer.vue'),
                props: true,
                meta: { title: '填写申请' }
            },

            { path: 'forms/:formId/submissions', name: 'form-submissions', component: () => import('../views/Submissions.vue'), props: true, meta: { title: '提交记录' } },
            { path: 'submission/:submissionId', name: 'submission-detail', component: () => import('../views/SubmissionDetail.vue'), props: true, meta: { title: '申请详情' } },
            { path: 'workflow/designer/:formId', name: 'workflow-designer', component: () => import('../views/WorkflowDesigner.vue'), props: true, meta: { title: '流程设计器', requiresAdmin: true } },
            { path: 'tasks', name: 'task-list', component: () => import('../views/TaskList.vue'), meta: { title: '我的待办' } },
            { path: 'tasks/:taskId', name: 'task-detail', component: () => import('../views/TaskDetail.vue'), props: true, meta: { title: '任务处理' } },
            { path: 'my-submissions', name: 'my-submissions', component: () => import('../views/MySubmissions.vue'), meta: { title: '我的申请' } },
        ]
    },
    {
        path: '/:pathMatch(.*)*',
        name: 'not-found',
        redirect: { name: 'home' }
    }
];

let router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: staticRoutes
});

const dynamicRouteModules = {
    'FORM_ENTRY': () => import('../views/FormViewer.vue'),
    'DATA_LIST': () => import('../views/DataListView.vue'),
};

export function addDynamicRoutes(menus) {
    const layoutRouteName = 'layout';
    if (!router.hasRoute(layoutRouteName)) {
        console.error(`未找到名为 '${layoutRouteName}' 的布局路由!`);
        return;
    }
    const processMenus = (menuItems) => {
        for (const menu of menuItems) {
            if (menu.path && menu.type !== 'DIRECTORY') {
                const componentLoader = dynamicRouteModules[menu.type];
                if (componentLoader) {
                    const route = {
                        path: menu.path,
                        name: menu.path.replace(/^\//, '').replace(/\//g, '-'),
                        component: componentLoader,
                        meta: { title: menu.name, formId: menu.formDefinitionId, menuId: menu.id },
                        props: route => ({ formId: route.meta.formId })
                    };
                    if (!router.hasRoute(route.name)) {
                        router.addRoute(layoutRouteName, route);
                    }
                }
            }
            if (menu.children && menu.children.length > 0) {
                processMenus(menu.children);
            }
        }
    };
    processMenus(menus);
}

export function resetRouter() {
    const newRouter = createRouter({
        history: createWebHistory(import.meta.env.BASE_URL),
        routes: staticRoutes
    });
    router.matcher = newRouter.matcher;
}

router.beforeEach(async (to, from, next) => {
    document.title = to.meta.title || '表单工作流引擎';
    const userStore = useUserStore();

    if (to.meta.requiresAuth && !userStore.isAuthenticated) {
        next({ name: 'login' });
    }
    else if (to.name === 'login' && userStore.isAuthenticated) {
        next({ name: 'home' });
    }
    else if (to.meta.requiresAdmin && !userStore.isAdmin) {
        message.warn('您没有权限访问此页面');
        next({ name: 'home' });
    }
    else if (userStore.isAuthenticated && userStore.passwordChangeRequired && to.name !== 'profile') {
        message.warn('为了您的账户安全，请先修改初始密码。');
        next({ name: 'profile' });
    }
    else {
        next();
    }
});

export default router;