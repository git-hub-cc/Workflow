
<template>
  <div class="page-container">
    <a-page-header title="个人中心" />
    <div style="padding: 24px; max-width: 600px; margin: auto;">
      <a-card title="修改密码">
        <a-form
            :model="formState"
            :rules="rules"
            ref="formRef"
            layout="vertical"
            @finish="handleChangePassword"
        >
          <a-form-item label="旧密码" name="oldPassword">
            <a-input-password v-model:value="formState.oldPassword" placeholder="请输入当前密码" />
          </a-form-item>
          <a-form-item label="新密码" name="newPassword">
            <a-input-password v-model:value="formState.newPassword" placeholder="请输入至少6位的新密码" />
          </a-form-item>
          <a-form-item label="确认新密码" name="confirmPassword">
            <a-input-password v-model:value="formState.confirmPassword" placeholder="请再次输入新密码" />
          </a-form-item>
          <a-form-item>
            <a-button type="primary" html-type="submit" :loading="loading">
              确认修改
            </a-button>
          </a-form-item>
        </a-form>
      </a-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { useUserStore } from '@/stores/user';
import { changePassword } from '@/api';
import { message } from 'ant-design-vue';

const userStore = useUserStore();
const loading = ref(false);
const formRef = ref();
const formState = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});

const validateConfirmPassword = (rule, value) => {
  if (value === '') {
    return Promise.reject('请再次输入新密码');
  } else if (value !== formState.newPassword) {
    return Promise.reject("两次输入的密码不一致!");
  } else {
    return Promise.resolve();
  }
};

const rules = {
  oldPassword: [{ required: true, message: '请输入旧密码' }],
  newPassword: [{ required: true, message: '请输入新密码' }, { min: 6, message: '密码长度不能少于6位' }],
  confirmPassword: [{ required: true, validator: validateConfirmPassword }],
};

const handleChangePassword = async () => {
  try {
    await formRef.value.validate();
    loading.value = true;
    await changePassword({
      oldPassword: formState.oldPassword,
      newPassword: formState.newPassword,
    });
    message.success('密码修改成功，请重新登录！');
    // 修改密码成功后强制登出
    userStore.logout();
  } catch (error) {
    // API 错误已在拦截器中处理
    console.error('Password change failed:', error);
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.page-container {
  background-color: #fff;
}
</style>