import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import { fileURLToPath, URL } from 'node:url'
import Components from 'unplugin-vue-components/vite'
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'

// https://vitejs.dev/config/
export default defineConfig({
    base: './',
    plugins: [
        vue(),
        vueJsx(),
        // 【核心新增】配置 unplugin-vue-components 实现自动按需导入
        Components({
            resolvers: [
                AntDesignVueResolver({
                    importStyle: false, // css in lazy-loaded components will be imported automatically
                    resolveIcons: true,
                }),
            ],
        }),
    ],
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url)),
        },
        // 【核心修改】移除了不再需要的 'bpmn-js-properties-panel'
        dedupe: [
            'bpmn-js'
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