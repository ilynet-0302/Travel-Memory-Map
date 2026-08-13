import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    include: ['src/**/*.test.{ts,tsx}'],
    environment: 'node',
    env: {
      VITE_DEMO_MODE: 'true',
    },
  },
});
