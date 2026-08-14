import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

function originOf(value: string | undefined) {
  if (!value) return null;
  try {
    return new URL(value).origin;
  } catch {
    return null;
  }
}

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const mapStyleUrl = env.VITE_MAP_STYLE_URL?.trim()
    || 'https://tiles.openfreemap.org/styles/liberty';
  if (command === 'build') {
    const required = ['VITE_API_BASE_URL', 'VITE_SUPABASE_URL', 'VITE_SUPABASE_PUBLISHABLE_KEY'];
    const missing = required.filter((name) => !env[name]?.trim());
    if (missing.length > 0) {
      throw new Error(`Missing production environment variables: ${missing.join(', ')}`);
    }
    if (env.VITE_DEMO_MODE !== 'false') {
      throw new Error('Production builds require VITE_DEMO_MODE=false.');
    }
  }
  const productionSecurityMeta = {
    name: 'production-security-meta',
    transformIndexHtml() {
      const dynamicOrigins = [
        originOf(env.VITE_API_BASE_URL),
        originOf(env.VITE_SUPABASE_URL),
        originOf(mapStyleUrl),
      ].filter((value): value is string => Boolean(value));
      const connectSources = ["'self'", ...dynamicOrigins, 'https://raw.githubusercontent.com'];
      const imageSources = ["'self'", 'data:', 'blob:', ...dynamicOrigins];
      const policy = [
        "default-src 'self'",
        "base-uri 'self'",
        "object-src 'none'",
        "script-src 'self'",
        "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
        "font-src 'self' data: https://fonts.gstatic.com",
        `img-src ${imageSources.join(' ')}`,
        `connect-src ${connectSources.join(' ')}`,
        "worker-src 'self' blob:",
        "manifest-src 'self'",
        "form-action 'self'",
      ].join('; ');
      return [{
        tag: 'meta',
        attrs: { 'http-equiv': 'Content-Security-Policy', content: policy },
        injectTo: 'head-prepend' as const,
      }];
    },
  };
  return {
    plugins: [react(), ...(command === 'build' ? [productionSecurityMeta] : [])],
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
