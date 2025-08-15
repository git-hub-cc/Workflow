import axios from 'axios';
import { message } from 'ant-design-vue';
import { useUserStore } from '@/stores/user';

// --- Authentication API ---
export const login = (credentials) => service.post('/auth/login', credentials);

// 创建 axios 实例
const service = axios.create({
    baseURL: '/api', // Vite 代理会自动处理
    timeout: 10000, // 请求超时时间
});

// 请求拦截器
service.interceptors.request.use(
    config => {
        // 在 Vue 组件之外使用 Pinia store
        const userStore = useUserStore();
        if (userStore.token) {
            config.headers['Authorization'] = `Bearer ${userStore.token}`;
        }
        return config;
    },
    error => {
        console.log(error);
        return Promise.reject(error);
    }
);

// 响应拦截器
service.interceptors.response.use(
    response => {
        return response.data;
    },
    error => {
        console.error('API Error:', error);
        if (error.response?.status === 401 || error.response?.status === 403) {
            message.error('认证失败或权限不足，请重新登录。');
            // 如果认证失败，强制登出
            const userStore = useUserStore();
            userStore.logout();
        } else {
            message.error(error.response?.data?.message || error.message || '请求失败');
        }
        return Promise.reject(error);
    }
);

// --- 表单 API ---
export const getForms = () => service.get('/forms');
export const getFormById = (id) => service.get(`/forms/${id}`);
export const createForm = (data) => service.post('/forms', data);
export const getSubmissions = (formId) => service.get(`/forms/${formId}/submissions`);
export const getSubmissionById = (submissionId) => service.get(`/forms/submissions/${submissionId}`);
export const submitForm = (formId, data) => service.post(`/forms/${formId}/submissions`, data);

// --- 工作流 API ---
export const deployWorkflow = (data) => service.post('/workflows/deploy', data);
export const getWorkflowTemplate = (formId) => service.get(`/workflows/templates?formId=${formId}`);

// --- 任务 API ---
export const getPendingTasks = () => service.get('/tasks/pending'); // assigneeId is determined by backend via JWT
export const getTaskById = (taskId) => service.get(`/tasks/${taskId}`);
export const completeTask = (taskId, data) => service.post(`/tasks/${taskId}/complete`, data);

// --- 用户和管理 API ---
export const getAllUsers = () => service.get('/workflows/users');
// Admin User Management
export const createUser = (data) => service.post('/admin/users', data);
export const updateUser = (id, data) => service.put(`/admin/users/${id}`, data);
export const deleteUser = (id) => service.delete(`/admin/users/${id}`);
// Admin Instance Management
export const getActiveInstances = () => service.get('/admin/instances');
export const terminateInstance = (instanceId, reason) => service.delete(`/admin/instances/${instanceId}?reason=${reason}`);

export default service;