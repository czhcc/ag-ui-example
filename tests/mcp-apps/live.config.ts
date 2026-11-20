import {defineConfig} from '@playwright/test'

// Opt-in: uses the running services and makes real model requests, without mocks.
export default defineConfig({
  testDir: '.', testMatch: 'live.spec.ts', workers: 1, timeout: 600000,
  outputDir: '../../test-results/mcp-apps-live', reporter: 'list',
  expect: {timeout: 15000},
  use: {baseURL: process.env.MCP_APPS_LIVE_URL ?? 'http://127.0.0.1:5173',
    channel: 'msedge', headless: true, viewport: {width: 1440, height: 1100},
    actionTimeout: 30000, navigationTimeout: 60000,
    trace: 'retain-on-failure', screenshot: 'only-on-failure'},
})
