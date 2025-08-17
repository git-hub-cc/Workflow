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
        // ...登录逻辑无变化
        loading.value = true;
        try {
            const response = await login(credentials);
            token.value = response.token;
            currentUser.value = response.user;
            localStorage.setItem('token', token.value);
            localStorage.setItem('user', JSON.stringify(currentUser.value));

            await fetchAndSetMenus();

            // 登录后预加载基础数据
            await fetchUsersForPicker();
            if (isAdmin.value) {
                await fetchUsersForManagement();
                await fetchAllRoles();
                await fetchAllGroups();
            }

            const firstMenu = findFirstNavigableMenu(menus.value);
            const redirectPath = firstMenu ? firstMenu.path : '/';
            await router.push(redirectPath);
            message.success(`欢迎回来, ${currentUser.value.name}`);

        } catch (error) { /* ... */ } finally {
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
        if (!isAuthenticated.value) return;
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

    // --- 【核心重构】不再需要手动更新 state 的 actions ---
    // 增删改操作完成后，将直接调用 fetchUsersForManagement 来刷新整个列表，保证数据绝对同步。
    // 这简化了逻辑，避免了手动操作可能带来的不一致性。

    function findFirstNavigableMenu(menuItems) { /* ... */ }

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