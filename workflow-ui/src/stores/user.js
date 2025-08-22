import { ref, computed } from 'vue';
import { defineStore } from 'pinia';
// 【核心新增】导入 getPendingTasks API
import { login, getAllUsers, getRoles, getGroups, getMyMenus, getUsersForPicker, getPendingTasks } from '@/api';
import { message } from 'ant-design-vue';
import router, { resetRouter, addDynamicRoutes } from '@/router';

export const useUserStore = defineStore('user', () => {
// --- State ---
    const token = ref(localStorage.getItem('token') || null);
    const currentUser = ref(JSON.parse(localStorage.getItem('user')) || null);
    const menus = ref(JSON.parse(localStorage.getItem('menus')) || []);
    const usersForManagement = ref([]);
    const allRoles = ref([]);
    const allGroups = ref([]);
    const loading = ref(false);
    // 【核心新增】待办任务数量状态
    const pendingTasksCount = ref(0);

// --- Getters ---
    const isAuthenticated = computed(() => !!token.value && !!currentUser.value);
    const isAdmin = computed(() => currentUser.value?.role === 'ADMIN');
    const passwordChangeRequired = computed(() => currentUser.value?.passwordChangeRequired || false);

// --- Actions ---
    async function handleLogin(credentials) {
        loading.value = true;
        try {
            const response = await login(credentials);
            token.value = response.token;
            currentUser.value = response.user;
            localStorage.setItem('token', token.value);
            localStorage.setItem('user', JSON.stringify(currentUser.value));

            await fetchAndSetMenus();
            // 登录成功后，获取一次待办数量
            await fetchPendingTasksCount();

            if (currentUser.value.passwordChangeRequired) {
                message.warn('为了您的账户安全，请先修改初始密码。');
                await router.push({ name: 'profile' });
            } else {
                message.success(`欢迎回来, ${currentUser.value.name}`);
                await router.push('/');
            }

        } catch (error) {
            console.error("Login failed:", error);
        } finally {
            loading.value = false;
        }
    }

    async function fetchAndSetMenus() {
        if (!isAuthenticated.value) return;
        try {
            const menuData = await getMyMenus();
            menus.value = menuData;
            localStorage.setItem('menus', JSON.stringify(menuData));
            addDynamicRoutes(menuData);
        } catch (error) {
            console.error("获取用户菜单失败:", error);
            message.error("获取用户菜单失败，请联系管理员");
            logout();
        }
    }

    function logout() {
        token.value = null;
        currentUser.value = null;
        menus.value = [];
        usersForManagement.value = [];
        allRoles.value = [];
        allGroups.value = [];
        // 【核心新增】登出时重置数量
        pendingTasksCount.value = 0;
        localStorage.clear();

        resetRouter();
        router.push('/login');
    }

    // --- 【核心新增】获取待办任务数量的 Action ---
    async function fetchPendingTasksCount() {
        if (!isAuthenticated.value) return;
        try {
            const response = await getPendingTasks({ page: 0, size: 1 });
            pendingTasksCount.value = response.totalElements;
        } catch (error) {
            console.error("Failed to fetch pending tasks count:", error);
            pendingTasksCount.value = 0; // 出错时重置为0
        }
    }

    async function fetchUsersForManagement(force = false) {
        if (!isAdmin.value || (usersForManagement.value.length > 0 && !force)) return;
        loading.value = true;
        try {
            usersForManagement.value = await getAllUsers();
        } catch (error) {
            console.error("Failed to fetch users for management:", error);
        } finally {
            loading.value = false;
        }
    }

    async function fetchUsersForPicker(params = {}) {
        if (!isAdmin.value) return [];
        try {
            const response = await getUsersForPicker(params);
            return response;
        } catch (error) {
            console.error("Failed to fetch users for picker:", error);
            return { content: [], totalElements: 0 };
        }
    }

    async function fetchAllRoles(force = false) {
        if (!isAdmin.value || (allRoles.value.length > 0 && !force)) return;
        loading.value = true;
        try { allRoles.value = await getRoles(); } catch (error) { console.error(error); } finally { loading.value = false; }
    }

    async function fetchAllGroups(force = false) {
        if (!isAdmin.value || (allGroups.value.length > 0 && !force)) return;
        loading.value = true;
        try { allGroups.value = await getGroups(); } catch (error) { console.error(error); } finally { loading.value = false; }
    }

    function updateCurrentUser(profileData) {
        if(currentUser.value) {
            currentUser.value.name = profileData.name;
            currentUser.value.email = profileData.email;
            localStorage.setItem('user', JSON.stringify(currentUser.value));
        }
    }

    return {
        token, currentUser, menus,
        usersForManagement,
        allRoles, allGroups, loading,
        // 【核心新增】导出新状态和方法
        pendingTasksCount,
        isAuthenticated, isAdmin, passwordChangeRequired,
        login: handleLogin, logout,
        fetchUsersForManagement, fetchUsersForPicker,
        fetchAllRoles, fetchAllGroups,
        fetchAndSetMenus,
        updateCurrentUser,
        // 【核心新增】导出新状态和方法
        fetchPendingTasksCount
    };
});