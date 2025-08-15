import { createRouter, createWebHistory } from 'vue-router';
import AppLayout from '@/components/AppLayout.vue';
import { useUserStore } from '@/stores/user';

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/login',
            name: 'login',
            component: () => import('../views/Login.vue'),
            meta: { title: '登录' }
        },
        {
            path: '/',
            component: AppLayout,
            meta: { requiresAuth: true }, // All routes under this layout require login
            children: [
                {
                    path: '',
                    name: 'home',
                    component: () => import('../views/Home.vue'),
                    meta: { title: '首页 - 表单管理' }
                },
                {
                    path: 'form/builder',
                    name: 'form-builder',
                    component: () => import('../views/FormBuilder.vue'),
                    meta: { title: '表单设计器', requiresAdmin: true }
                },
                {
                    path: 'form/viewer/:formId',
                    name: 'form-viewer',
                    component: () => import('../views/FormViewer.vue'),
                    props: true,
                    meta: { title: '填写表单' }
                },
                {
                    path: 'form/submissions/:formId',
                    name: 'form-submissions',
                    component: () => import('../views/Submissions.vue'),
                    props: true,
                    meta: { title: '查看提交数据' }
                },
                {
                    path: 'workflow/designer/:formId',
                    name: 'workflow-designer',
                    component: () => import('../views/WorkflowDesigner.vue'),
                    props: true,
                    meta: { title: '流程设计器', requiresAdmin: true }
                },
                {
                    path: 'tasks',
                    name: 'task-list',
                    component: () => import('../views/TaskList.vue'),
                    meta: { title: '我的待办' }
                },
                {
                    path: 'tasks/:taskId',
                    name: 'task-detail',
                    component: () => import('../views/TaskDetail.vue'),
                    props: true,
                    meta: { title: '任务处理' }
                },
                // Admin Routes
                {
                    path: 'admin/users',
                    name: 'admin-users',
                    component: () => import('../views/admin/UserManagement.vue'),
                    meta: { title: '用户管理', requiresAdmin: true }
                },
                {
                    path: 'admin/instances',
                    name: 'admin-instances',
                    component: () => import('../views/admin/InstanceManagement.vue'),
                    meta: { title: '实例管理', requiresAdmin: true }
                }
            ]
        }
    ]
});

// Global navigation guard
router.beforeEach((to, from, next) => {
    document.title = to.meta.title || '表单工作流引擎';
    const userStore = useUserStore();

    if (to.meta.requiresAuth && !userStore.isAuthenticated) {
        // Redirect to login if trying to access a protected page without being logged in
        next({ name: 'login' });
    } else if (to.name === 'login' && userStore.isAuthenticated) {
        // Redirect to home if trying to access login page while already logged in
        next({ name: 'home' });
    } else if (to.meta.requiresAdmin && !userStore.isAdmin) {
        // Optional: Redirect if a non-admin tries to access an admin page
        next({ name: 'home' });
    } else {
        next();
    }
});

export default router;