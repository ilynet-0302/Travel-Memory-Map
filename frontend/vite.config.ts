import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  return {
    plugins: [react()],
    base: command === 'build' ? env.VITE_BASE_PATH || '/travel-memory-map/' : '/',
    server: {
      port: 5173,
      strictPort: true,
    },
    preview: {
      port: 4173,
      strictPort: true,
    },
    build: {
      // MapLibre is isolated behind the lazy trip-detail route (about 277 kB gzip).
      chunkSizeWarningLimit: 1_100,
    },
  };
});
