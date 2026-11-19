export interface McpAppDescriptor {
  profile: 'ac.mcp-app'
  profileVersion: '1.0'
  appInstanceId: string
  messageId: string
  threadId: string
  originRunId: string
  toolCallId: string
  title: string
  resourceHash: string
  resultRef: string
  revision: number
  status?: string
  error?: string
  needsSync?: boolean
}
export interface McpAppFollowup {question: string; appFollowupRef: string; threadId: string; parentRunId: string}
export function validateApp(value: unknown, threadId: string, runId: string): McpAppDescriptor | null {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return null
  const v = value as Record<string, unknown>
  const keys = ['profile', 'profileVersion', 'appInstanceId', 'messageId', 'threadId', 'originRunId', 'toolCallId', 'title', 'resourceHash', 'resultRef', 'revision']
  if (Object.keys(v).some(k => !keys.includes(k)) || v.profile !== 'ac.mcp-app' || v.profileVersion !== '1.0'
    || v.threadId !== threadId || v.originRunId !== runId || !Number.isSafeInteger(v.revision) || Number(v.revision) < 1
    || !keys.filter(k => k !== 'revision').every(k => typeof v[k] === 'string' && (v[k] as string).length > 0 && (v[k] as string).length <= 256)
    || !/^sha256:[a-f0-9]{64}$/.test(String(v.resourceHash))
    || ['__proto__', 'constructor', 'prototype'].some(id => [v.appInstanceId, v.messageId].includes(id))) return null
  return {...v} as unknown as McpAppDescriptor
}
