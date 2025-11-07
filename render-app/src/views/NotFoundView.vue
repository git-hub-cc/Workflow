<template>
  <div class="not-found-container">
    <a-result
        status="404"
        title="404"
        sub-title="抱歉，您访问的页面不存在。"
    >
      <template #extra>
        <router-link to="/">
          <a-button type="primary">返回首页</a-button>
        </router-link>
      </template>
    </a-result>
  </div>
</template>

<style scoped>
.not-found-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
}
</style>