import vue from '@vitejs/plugin-vue';
import path from 'path';
import {defineConfig, loadEnv} from 'vite';

export default defineConfig(({mode}) => {
  const env = loadEnv(mode, '.', '');
  return {
    plugins: [vue()],
    define: {
      'process.env.GEMINI_API_KEY': JSON.stringify(env.GEMINI_API_KEY),
    },
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },
    server: {
      hmr: process.env.DISABLE_HMR !== 'true',
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
        // WebSocket 代理配置（当前未使用，前端直接连接到 ws://localhost:8083）
        // 如果需要通过网关访问，可以启用此配置并修改 websocket.ts 中的地址
        '/ws': {
          target: 'ws://localhost:8083',
          ws: true,
          changeOrigin: true,
        }
      }
    },

  };
});
