import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  // Relative base so the built app works from any sub-path (e.g. GitHub Pages).
  base: '/ledger/',
});
