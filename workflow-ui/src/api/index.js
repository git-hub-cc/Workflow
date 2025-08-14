import axios from 'axios';
import { message } from 'ant-design-vue';
import { useUserStore } from '@/stores/user';

// 创建 axios 实例
const service = axios.create({
    baseURL: '/api', // Vite 代理会自动处理
    timeout: 10000, // 请求超时时间
});

// 请求拦截器
service.interceptors.request.use(
    config => {
        // 在发送请求之前做些什么，比如添加 token 或用户ID
        const userStore = useUserStore();
        if (userStore.currentUserId) {
            config.headers['X-User-ID'] = userStore.currentUserId;
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
        // 对响应数据做点什么
        return response.data;
    },
    error => {
        console.error('API Error:', error);
        message.error(error.message || '请求失败，请检查网络或联系管理员');
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

// --- 工作流与用户 API ---
export const getUsers = () => service.get('/workflows/users');
export const deployWorkflow = (data) => service.post('/workflows/deploy', data);
export const getWorkflowTemplate = (formId) => service.get(`/workflows/templates?formId=${formId}`);

// --- 任务 API ---
export const getPendingTasks = (assigneeId) => service.get(`/tasks/pending?assigneeId=${assigneeId}`);
export const getTaskById = (taskId) => service.get(`/tasks/${taskId}`);
export const completeTask = (taskId, data) => service.post(`/tasks/${taskId}/complete`, data);

export default service;