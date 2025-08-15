import { ref, computed } from 'vue';
import { defineStore } from 'pinia';
import { login, getAllUsers } from '@/api';
import { message } from 'ant-design-vue';
import router from '@/router';

export const useUserStore = defineStore('user', () => {
    // --- State ---
    const token = ref(localStorage.getItem('token') || null);
    const currentUser = ref(JSON.parse(localStorage.getItem('user')) || null);
    const allUsers = ref([]); // For dropdowns
    const loading = ref(false);

    // --- Getters ---
    const isAuthenticated = computed(() => !!token.value && !!currentUser.value);
    const isAdmin = computed(() => currentUser.value?.role === 'ADMIN');

    // --- Actions ---
    async function handleLogin(credentials) {
        loading.value = true;
        try {
            const response = await login(credentials);
            token.value = response.token;
            currentUser.value = response.user;

            localStorage.setItem('token', token.value);
            localStorage.setItem('user', JSON.stringify(currentUser.value));

            // After successful login, fetch all users for the dropdown
            await fetchAllUsers();

            await router.push('/');
            message.success(`欢迎回来, ${currentUser.value.name}`);
        } catch (error) {
            console.error("Login failed:", error);
            // Error is already handled by the global interceptor
        } finally {
            loading.value = false;
        }
    }

    function logout() {
        token.value = null;
        currentUser.value = null;
        allUsers.value = [];
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        router.push('/login');
    }

    async function fetchAllUsers() {
        if (!isAuthenticated.value) return;
        try {
            allUsers.value = await getAllUsers();
        } catch (error) {
            message.error('获取用户列表失败!');
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
        allUsers.value = allUsers.value.filter(u => u.id !== userId);
    }


    return {
        token,
        currentUser,
        allUsers,
        loading,
        isAuthenticated,
        isAdmin,
        login: handleLogin,
        logout,
        fetchAllUsers,
        addUser,
        updateUser,
        removeUser,
    };
});