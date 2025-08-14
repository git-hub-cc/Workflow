import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '@/components/AppLayout.vue'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            component: AppLayout, // 使用布局组件包裹所有页面
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
                    meta: { title: '表单设计器' }
                },
                {
                    path: 'form/viewer/:formId',
                    name: 'form-viewer',
                    component: () => import('../views/FormViewer.vue'),
                    props: true, // 将路由参数 :formId 作为 props 传入组件
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
                    meta: { title: '流程设计器' }
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
                }
            ]
        }
        // 可以在这里添加 404 页面等
    ]
})

// 全局路由守卫，用于更新页面标题
router.beforeEach((to, from, next) => {
    document.title = to.meta.title || '表单工作流引擎';
    next();
});

export default router