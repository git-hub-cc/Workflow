import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import Antd from 'ant-design-vue';

// 引入 Ant Design Vue 的全局样式，如果使用 unplugin-vue-components 则不需要
// import 'ant-design-vue/dist/reset.css';

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(Antd); // 如果不使用 unplugin-vue-components, 需要全局注册

app.mount('#app')