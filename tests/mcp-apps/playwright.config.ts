import {defineConfig} from '@playwright/test'
import {fileURLToPath} from 'node:url'
const cwd = fileURLToPath(new URL('../..', import.meta.url))
export default defineConfig({
  testDir: '.', testMatch: '*.spec.ts', workers: 1, timeout: 30000,
  use: {baseURL: 'http://127.0.0.1:19173', channel: 'msedge', headless: true, trace: 'retain-on-failure'},
  webServer: [
    {command: 'node apps/mcp-app-sandbox/server.mjs', cwd, url: 'http://127.0.0.1:19174/proxy.html?parentOrigin=http://127.0.0.1:19173',
      env: {MCP_APP_SANDBOX_PORT: '19174', MCP_APP_PARENT_ORIGINS: 'http://127.0.0.1:19173'}},
    {command: 'pnpm --filter ai-mcp-platform-frontend dev --force --host 127.0.0.1 --port 19173', cwd, url: 'http://127.0.0.1:19173',
      env: {VITE_MCP_APP_SANDBOX_URL: 'http://127.0.0.1:19174/proxy.html'}},
  ],
})
