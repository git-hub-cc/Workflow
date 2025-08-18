import { ref, computed } from 'vue';
import { defineStore } from 'pinia';
import { login, getAllUsers, getRoles, getGroups, getMyMenus, getUsersForPicker } from '@/api';
import { message } from 'ant-design-vue';
import router, { resetRouter, addDynamicRoutes } from '@/router';

export const useUserStore = defineStore('user', () => {
    // --- State ---
    const token = ref(localStorage.getItem('token') || null);
    const currentUser = ref(JSON.parse(localStorage.getItem('user')) || null);
    const menus = ref(JSON.parse(localStorage.getItem('menus')) || []);
    // --- 【核心重构】数据源分离 ---
    const usersForManagement = ref([]); // 用于用户管理页面的完整用户数据
    const usersForPicker = ref([]);     // 用于选择器的简化用户数据
    const allRoles = ref([]);
    const allGroups = ref([]);
    const loading = ref(false);

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

            // ⭐ 核心修改: 先获取并设置菜单，再进行路由跳转
            await fetchAndSetMenus();

            // 登录后预加载基础数据
            // 【安全修复】调整预加载逻辑，只有管理员才加载特定数据
            if (isAdmin.value) {
                await fetchUsersForPicker();
                await fetchUsersForManagement();
                await fetchAllRoles();
                await fetchAllGroups();
            }

            // ⭐ 核心修改: 统一跳转到根路径，让导航守卫处理后续逻辑
            await router.push('/');
            message.success(`欢迎回来, ${currentUser.value.name}`);

        } catch (error) { /* 错误由拦截器处理 */ } finally {
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
        usersForPicker.value = [];
        allRoles.value = [];
        allGroups.value = [];
        localStorage.clear(); // 清空所有localStorage

        resetRouter();
        router.push('/login');
    }

    // --- 【核心重构】数据获取方法分离 ---
    async function fetchUsersForManagement() {
        if (!isAdmin.value) return;
        try {
            usersForManagement.value = await getAllUsers();
        } catch (error) {
            console.error("Failed to fetch users for management:", error);
        }
    }

    async function fetchUsersForPicker() {
        // --- 【安全修复】增加前端权限判断，与后端API权限保持一致 ---
        // 确保只有管理员才能调用这个获取全量用户的接口
        if (!isAdmin.value) {
            console.warn("Attempted to fetch all users for picker without admin rights. Skipped.");
            usersForPicker.value = []; // 清空可能存在的旧数据
            return;
        }
        try {
            usersForPicker.value = await getUsersForPicker();
        } catch (error) {
            console.error("Failed to fetch users for picker:", error);
        }
    }

    async function fetchAllRoles() {
        if (!isAdmin.value) return;
        try { allRoles.value = await getRoles(); } catch (error) { console.error(error); }
    }

    async function fetchAllGroups() {
        if (!isAdmin.value) return;
        try { allGroups.value = await getGroups(); } catch (error) { console.error(error); }
    }

    return {
        token, currentUser, menus,
        usersForManagement, usersForPicker, // 分离的数据源
        allRoles, allGroups, loading,
        isAuthenticated, isAdmin, passwordChangeRequired,
        login: handleLogin, logout,
        fetchUsersForManagement, fetchUsersForPicker, // 分离的获取方法
        fetchAllRoles, fetchAllGroups,
        fetchAndSetMenus
    };
});