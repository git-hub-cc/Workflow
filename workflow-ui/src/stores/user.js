import { ref, computed } from 'vue';
import { defineStore } from 'pinia';
import { login, getAllUsers, getRoles } from '@/api';
import { message } from 'ant-design-vue';
import router from '@/router';

export const useUserStore = defineStore('user', () => {
    // --- State ---
    const token = ref(localStorage.getItem('token') || null);
    const currentUser = ref(JSON.parse(localStorage.getItem('user')) || null);
    const allUsers = ref([]); // 用于用户管理和选择器
    const allRoles = ref([]); // 【新增】用于角色分配
    const loading = ref(false);

    // --- Getters ---
    const isAuthenticated = computed(() => !!token.value && !!currentUser.value);
    const isAdmin = computed(() => currentUser.value?.role === 'ADMIN');
    // 【新增】判断是否需要修改密码
    const passwordChangeRequired = computed(() => currentUser.value?.passwordChangeRequired || false);


    // --- Actions ---
    async function handleLogin(credentials) {
        loading.value = true;
        try {
            const response = await login(credentials);
            token.value = response.token;
            // 【修改】后端返回的 user 对象现在可能包含 passwordChangeRequired 标志
            currentUser.value = response.user;

            localStorage.setItem('token', token.value);
            localStorage.setItem('user', JSON.stringify(currentUser.value));

            // 登录成功后，获取基础数据
            await fetchAllUsers();
            if (isAdmin.value) {
                await fetchAllRoles();
            }

            // 【修改】登录后的跳转逻辑由路由守卫处理
            await router.push('/');
            message.success(`欢迎回来, ${currentUser.value.name}`);

        } catch (error) {
            // 【新增】处理需要强制修改密码的情况
            if (error.response?.status === 499) {
                message.error('您的密码是初始密码或已由管理员重置，请登录后立即修改！');
                // 仍然执行登录，但路由守卫会拦截
                const response = error.response.data; // 后端应在499时也返回token和user
                token.value = response.token;
                currentUser.value = response.user;
                localStorage.setItem('token', token.value);
                localStorage.setItem('user', JSON.stringify(currentUser.value));
                await router.push('/profile'); // 手动导航到profile页
            }
            // 其他错误已在 api/index.js 中处理
            console.error("Login failed:", error);
        } finally {
            loading.value = false;
        }
    }

    function logout() {
        token.value = null;
        currentUser.value = null;
        allUsers.value = [];
        allRoles.value = [];
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        router.push('/login');
    }

    async function fetchAllUsers() {
        if (!isAuthenticated.value) return;
        try {
            allUsers.value = await getAllUsers();
        } catch (error) {
            console.error(error);
        }
    }

    // 【新增】获取所有角色
    async function fetchAllRoles() {
        if (!isAdmin.value) return;
        try {
            allRoles.value = await getRoles();
        } catch (error) {
            console.error(error);
        }
    }

    // --- Admin-related actions for UserManagement.vue ---
    function addUser(user) {
        allUsers.value.push(user);
    }
    function updateUser(updatedUser) {
        const index = allUsers.value.findIndex(u => u.id === updatedUser.id);
        if (index !== -1) {
            allUsers.value[index] = updatedUser;
        }
    }
    function removeUser(userId) {
        // 【修改】改为更新用户状态，而不是从列表中移除
        const user = allUsers.value.find(u => u.id === userId);
        if (user) {
            user.status = 'INACTIVE';
        }
    }

    // 【新增】角色管理 actions
    function addRole(role) {
        allRoles.value.push(role);
    }

    return {
        token,
        currentUser,
        allUsers,
        allRoles,
        loading,
        isAuthenticated,
        isAdmin,
        passwordChangeRequired,
        login: handleLogin,
        logout,
        fetchAllUsers,
        fetchAllRoles,
        addUser,
        updateUser,
        removeUser,
        addRole,
    };
});