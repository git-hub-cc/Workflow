<template>
  <div class="login-container">
    <div class="login-box">
      <div class="logo-section">
        <img src="/logo.svg" alt="logo" />
        <h1>表单工作流引擎</h1>
      </div>
      <a-form
          :model="formState"
          @finish="handleLogin"
          layout="vertical"
      >
        <a-form-item
            name="userId"
            :rules="[{ required: true, message: '请输入用户ID!' }]"
        >
          <a-input v-model:value="formState.userId" placeholder="用户ID (e.g., admin, user001)">
            <template #prefix><UserOutlined /></template>
          </a-input>
        </a-form-item>

        <a-form-item
            name="password"
            :rules="[{ required: true, message: '请输入密码!' }]"
        >
          <a-input-password v-model:value="formState.password" placeholder="密码 (e.g., admin, password)">
            <template #prefix><LockOutlined /></template>
          </a-input-password>
        </a-form-item>

        <a-form-item>
          <a-button type="primary" html-type="submit" :loading="userStore.loading" block>
            登 录
          </a-button>
        </a-form-item>
        <div class="info-text">
          <p><strong>演示用户凭据:</strong></p>
          <ul>
            <li>系统管理员: <code>admin</code> / <code>admin</code></li>
            <li>普通员工: <code>user001</code> / <code>password</code></li>
            <li>部门经理: <code>manager001</code> / <code>password</code></li>
            <li>财务总监: <code>hr001</code> / <code>password</code></li>
          </ul>
          <p style="margin-top: 8px;">
            <a-alert
                message="管理员创建的新用户或重置密码的用户，默认密码均为 'password'，登录后会被要求强制修改。"
                type="info"
                show-icon
            />
          </p>
        </div>
      </a-form>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue';
import { useUserStore } from '@/stores/user';
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue';

const userStore = useUserStore();
const formState = reactive({
  userId: 'admin', // Default for convenience
  password: 'admin', // Default for convenience
});

const handleLogin = async () => {
  // store action 现在会处理所有登录逻辑，包括强制密码修改的场景
  await userStore.login(formState);
};
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f0f2f5;
}
.login-box {
  width: 368px;
  padding: 36px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
.logo-section {
  text-align: center;
  margin-bottom: 24px;
}
.logo-section img {
  height: 64px;
}
.logo-section h1 {
  font-size: 24px;
  color: #1890ff;
  margin-top: 16px;
}
.info-text {
  font-size: 12px;
  color: #888;
  margin-top: 16px;
  border-top: 1px solid #f0f0f0;
  padding-top: 16px;
}
.info-text ul {
  padding-left: 20px;
  margin-top: 8px;
}
.info-text code {
  background-color: #f5f5f5;
  padding: 2px 4px;
  border-radius: 4px;
  color: #c41d7f;
}
:deep(.ant-alert) {
  padding: 8px 12px;
  font-size: 12px;
}
</style>