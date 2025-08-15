import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path' // 确保导入了 path 模块

// https://vitejs.dev/config/
export default defineConfig({
    plugins: [vue()],
    resolve: {
        // 路径别名配置 (保持不变)
        alias: {
            '@': path.resolve(__dirname, './src'),
        },
        // 新增或修改 dedupe 配置
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