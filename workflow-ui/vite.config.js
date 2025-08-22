import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
// 【核心新增】导入 vue-jsx 插件
import vueJsx from '@vitejs/plugin-vue-jsx'
import { fileURLToPath, URL } from 'node:url'

// https://vitejs.dev/config/
export default defineConfig({
    // 【核心修改】在 plugins 数组中添加 vueJsx()
    plugins: [
        vue(),
        vueJsx(),
    ],
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url)),
        },
        // 新增或修改 dedupe 配置，防止 bpmn-js 依赖冲突
        dedupe: [
            'bpmn-js',
            'bpmn-js-properties-panel'
        ]
    },
    server: {
        port: 5173,
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            }
        }
    }
})