import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import {viteSingleFile} from 'vite-plugin-singlefile'
export default defineConfig({plugins: [vue(), viteSingleFile()], build: {
  outDir: '../../mcp-server/target/generated-resources/mcp-apps/fund-flow/v1', emptyOutDir: false,
}})
