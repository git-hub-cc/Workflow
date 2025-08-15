import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src'),
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