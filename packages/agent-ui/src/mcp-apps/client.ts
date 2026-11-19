import type {McpAppFollowup} from './types'
export interface McpAppsOptions {
  sandboxUrl: string
  headers?: () => Record<string, string>
  onFollowup?: (intent: McpAppFollowup) => Promise<void>
  isBusy?: () => boolean
}
let options: McpAppsOptions | undefined
export function configureMcpApps(value: McpAppsOptions) {options = value}
export function appOptions(): McpAppsOptions {
  if (!options) throw new Error('MCP Apps 沙箱尚未配置')
  return options
}
export async function appRequest<T>(id: string, path = '', body?: unknown, method?: string, signal?: AbortSignal): Promise<T> {
  const config = appOptions()
  const response = await fetch(`/api/mcp-apps/instances/${encodeURIComponent(id)}${path}`, {
    method: method ?? (body === undefined ? 'GET' : 'POST'), credentials: 'include', signal,
    headers: {'Content-Type': 'application/json', ...config.headers?.()}, body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (!response.ok) {
    const value = await response.json().catch(() => ({})) as {code?: string}
    throw new Error(value.code ?? `APP_HTTP_${response.status}`)
  }
  return response.json() as Promise<T>
}
