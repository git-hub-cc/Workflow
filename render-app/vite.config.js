import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

// https://vitejs.dev/config/
export default defineConfig({
    plugins: [
        vue(),
        // 配置 unplugin-vue-components 实现 Ant Design Vue 的自动按需导入
        Components({
            resolvers: [
                AntDesignVueResolver({
                    // 【核心修改】将 `true` 修改为 `'less'`
                    // 这会告诉解析器去导入 less 源文件，例如: `ant-design-vue/es/button/style/index.less`
                    importStyle: 'less',
                }),
            ],
        }),
    ],
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url))
        }
    },
    // 配置 CSS 预处理器以支持 Ant Design Vue 的主题定制
    // 这个配置是正确的，无需修改，它会处理上面 importStyle: 'less' 导入的 .less 文件
    css: {
        preprocessorOptions: {
            less: {
                javascriptEnabled: true,
                // 如果您需要自定义 antd 的主题色等，可以在这里配置
                // modifyVars: {
                //   'primary-color': '#1DA57A',
                //   'link-color': '#1DA57A',
                //   'border-radius-base': '2px',
                // },
            },
        },
    },
    server: {
        port: 3000, // 渲染端使用 3000 端口，避免与管理端冲突
        proxy: {
            // 配置 API 代理，将所有 /api 请求转发到 Spring Boot 后端
            '/api': {
                target: 'http://localhost:8080', // 您的 Spring Boot 服务地址
                changeOrigin: true,
            },
            // 如果您的图片等静态资源也通过后端提供，也需要配置代理
            '/images': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            }
        }
    }
})