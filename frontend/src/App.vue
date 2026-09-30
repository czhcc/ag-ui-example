<script setup lang="ts">
import {computed, nextTick, onBeforeUnmount, reactive, ref} from 'vue'
import {
  createAgentUiState,
  latestAssistantMessage,
  reduceAgentUiEvent,
  RunActivityPanel,
  streamAgent,
} from '@ac/agent-ui'
import type {AgentUiState, AgUiEvent, MessagePart, RichMessage, RunAgentInput} from '@ac/agent-ui'
import UiSurfacePart from '@ac/agent-ui/components/UiSurfacePart.vue'
import RunLogPanel from './chat/RunLogPanel.vue'
import type {RunLogEntry} from './chat/RunLogPanel.vue'

interface DisplayMessage extends RichMessage { logs: RunLogEntry[] }

const createId = () => globalThis.crypto?.randomUUID?.()
    ?? `${Date.now()}-${Math.random().toString(16).slice(2)}`
const threadId = ref(sessionStorage.getItem('agent-thread-id') ?? `thread-${createId()}`)
const state = reactive(createAgentUiState()) as AgentUiState
const messages = ref<DisplayMessage[]>([
  {
    id: 'welcome', runId: 'welcome', role: 'assistant', logs: [],
    parts: [{kind: 'text', text: '你好，我是智能分析助手。告诉我你想查询或分析什么，我会按需调用工具。'}],
  },
])
const input = ref('')
const isSending = ref(false)
const errorMessage = ref('')
const controller = ref<AbortController | null>(null)
const list = ref<HTMLElement | null>(null)
let logSequence = 0

sessionStorage.setItem('agent-thread-id', threadId.value)

const identityHeaders = computed<Record<string, string>>(() => {
  if (!import.meta.env.DEV) return {} as Record<string, string>
  return {
    'X-Tenant-Id': import.meta.env.VITE_TENANT_ID || 'local-tenant',
    'X-User-Id': import.meta.env.VITE_USER_ID || 'local-user',
  }
})

function pushLog(message: DisplayMessage, kind: RunLogEntry['kind'], title: string, detail?: string) {
  message.logs.push({
    id: ++logSequence,
    time: new Date().toLocaleTimeString('zh-CN', {hour12: false}),
    kind,
    title,
    detail,
  })
}

async function send(contentOverride?: string) {
  const content = (contentOverride ?? input.value).trim()
  if (!content || isSending.value) return
  if (contentOverride === undefined) input.value = ''
  errorMessage.value = ''
  const runId = `run-${createId()}`
  const user: DisplayMessage = {
    id: `message-${createId()}`, runId, role: 'user', logs: [],
    parts: [{kind: 'text', text: content}],
  }
  const reply = reactive<DisplayMessage>({
    id: `${runId}:pending`, runId, role: 'assistant', logs: [], streaming: true,
    parts: [{kind: 'text', text: ''}],
  })
  messages.value.push(user, reply)
  const request: RunAgentInput = {
    threadId: threadId.value,
    runId,
    messages: [{id: user.id, role: 'user', content}],
    tools: [],
    context: [],
    state: {},
    forwardedProps: {
      locale: navigator.language,
      timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
      clientName: '@ac/agent-ui',
    },
    resume: [],
  }
  isSending.value = true
  controller.value = new AbortController()
  try {
    await streamAgent(request, (event) => handleEvent(reply, runId, event), controller.value.signal, {
      headers: identityHeaders.value,
      credentials: 'include',
    })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') return
    reply.failed = true
    reply.streaming = false
    const detail = error instanceof Error ? error.message : 'Agent connection failed'
    if (!reply.parts.some((part) => part.kind === 'text' && part.text)) {
      reply.parts = [{kind: 'text', text: '连接智能体失败，请确认后端已启动后重试。'}]
    }
    pushLog(reply, 'error', '连接失败', detail)
    errorMessage.value = detail
  } finally {
    reply.streaming = false
    isSending.value = false
    controller.value = null
    await scrollToBottom()
  }
}

function handleEvent(reply: DisplayMessage, runId: string, event: AgUiEvent) {
  reduceAgentUiEvent(state, event, runId)
  const reduced = latestAssistantMessage(state, runId)
  if (reduced) {
    reply.id = reduced.id
    reply.parts = reduced.parts
    reply.streaming = reduced.streaming
    reply.failed = reduced.failed
  }
  const run = state.runs[runId]
  switch (event.type) {
    case 'RUN_STARTED':
      pushLog(reply, 'run_start', '本轮执行开始')
      break
    case 'TEXT_MESSAGE_START':
      pushLog(reply, 'llm_start', '开始生成回复')
      break
    case 'TEXT_MESSAGE_END':
      pushLog(reply, 'llm_end', '回复生成完成')
      break
    case 'TOOL_CALL_START':
      pushLog(reply, 'tool_start', `调用 ${event.toolCallName}`)
      break
    case 'TOOL_CALL_RESULT': {
      const safe = state.toolCalls[event.toolCallId]?.result
      const detail = [safe?.summary, safe?.resultRef ? `resultRef: ${safe.resultRef}` : undefined]
          .filter(Boolean).join('\n') || undefined
      pushLog(reply, 'tool_end', `${state.toolCalls[event.toolCallId]?.name ?? 'Tool'} 返回`, detail)
      break
    }
    case 'CUSTOM':
      if (event.name.startsWith('ui.surface.')) {
        const value = event.value && typeof event.value === 'object'
            ? event.value as Record<string, unknown> : {}
        pushLog(reply, 'ui', event.name, typeof value.surfaceId === 'string'
            ? `surfaceId: ${value.surfaceId}` : undefined)
      }
      break
    case 'RUN_FINISHED':
      pushLog(reply, 'run_end', run?.status === 'interrupted' ? '等待人工处理' : '本轮执行结束')
      reply.streaming = false
      break
    case 'RUN_ERROR':
      reply.failed = true
      reply.streaming = false
      pushLog(reply, 'error', event.code || '执行出错', event.message)
      errorMessage.value = event.message
      break
  }
  void scrollToBottom()
}

async function scrollToBottom() {
  await nextTick()
  list.value?.scrollTo({top: list.value.scrollHeight, behavior: 'smooth'})
}

function text(message: DisplayMessage): string {
  return message.parts.filter((part): part is Extract<MessagePart, {kind: 'text'}> => part.kind === 'text')
      .map((part) => part.text).join('\n')
}

async function copyMessage(message: DisplayMessage) {
  await navigator.clipboard?.writeText(text(message))
}

function reset() {
  controller.value?.abort()
  threadId.value = `thread-${createId()}`
  sessionStorage.setItem('agent-thread-id', threadId.value)
  Object.assign(state, createAgentUiState())
  messages.value = [messages.value[0]]
  errorMessage.value = ''
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    void send()
  }
}

onBeforeUnmount(() => controller.value?.abort())
</script>

<template>
  <main class="workspace">
    <aside class="sidebar">
      <div>
        <p class="eyebrow">AI MCP PLATFORM</p>
        <h1>Agent Workspace</h1>
        <p class="muted">AG-UI · ac.rich-ui/1.0</p>
      </div>
      <button class="new-chat" type="button" @click="reset">＋ 新建对话</button>
      <div class="session-card">
        <span :class="['status-dot', {active: isSending}]"></span>
        <div><strong>{{ isSending ? '智能体执行中' : '准备就绪' }}</strong><small>{{ threadId }}</small></div>
      </div>
      <p class="sidebar-note">文本、工具、状态、推理和 Rich UI 均由标准 AG-UI 事件驱动。</p>
    </aside>

    <section class="chat-shell">
      <header><div><h2>智能分析助手</h2><p>正式 AG-UI 流式会话</p></div></header>
      <div ref="list" class="message-list">
        <article v-for="message in messages" :key="message.id" :class="['message', message.role]">
          <div class="avatar">{{ message.role === 'user' ? '你' : 'AI' }}</div>
          <div class="message-body">
            <div class="message-meta">{{ message.role === 'user' ? '你' : 'Agent' }}</div>
            <template v-for="(part, index) in message.parts" :key="part.kind === 'ui' ? part.surfaceId : index">
              <div v-if="part.kind === 'text'" :class="['text-part', {failed: message.failed}]">
                {{ part.text }}<i v-if="message.streaming && index === message.parts.length - 1" class="cursor"></i>
              </div>
              <UiSurfacePart v-else :surface="part.surface" @drill-down="(item) => { void send(item.question) }"/>
            </template>
            <div v-if="message.role === 'assistant' && message.runId !== 'welcome'" class="message-actions">
              <RunLogPanel v-if="message.logs.length" :entries="message.logs"/>
              <RunActivityPanel :state="state" :run-id="message.runId"/>
              <button v-if="text(message)" type="button" @click="copyMessage(message)">复制</button>
            </div>
          </div>
        </article>
      </div>
      <footer>
        <p v-if="errorMessage" class="error-banner">{{ errorMessage }}</p>
        <div class="composer">
          <textarea v-model="input" rows="1" maxlength="8000" :disabled="isSending"
                    placeholder="输入问题；Enter 发送，Shift + Enter 换行" @keydown="onKeydown"></textarea>
          <button type="button" :disabled="!input.trim() || isSending" @click="send()">
            {{ isSending ? '执行中' : '发送' }}
          </button>
        </div>
      </footer>
    </section>
  </main>
</template>

<style scoped>
.workspace { min-height: 100vh; display: grid; grid-template-columns: 270px minmax(0, 1fr); gap: 1.25rem;
  padding: 1.25rem; background: radial-gradient(circle at 10% 10%, #eef2ff, transparent 35%), #f8fafc; color: #0f172a; }
.sidebar, .chat-shell { border: 1px solid rgb(255 255 255 / .9); border-radius: 1.5rem; background: rgb(255 255 255 / .82);
  box-shadow: 0 18px 55px rgb(15 23 42 / .08); backdrop-filter: blur(18px); }
.sidebar { display: flex; flex-direction: column; padding: 1.5rem; }
.eyebrow { margin: 0 0 .5rem; color: #6366f1; font-size: .65rem; font-weight: 800; letter-spacing: .16em; }
.sidebar h1, header h2 { margin: 0; font-size: 1.05rem; }
.muted, header p { margin: .35rem 0 0; color: #94a3b8; font-size: .72rem; }
.new-chat { margin-top: 2rem; padding: .8rem; border: 0; border-radius: .85rem; background: #0f172a; color: white; cursor: pointer; }
.session-card { display: flex; gap: .65rem; align-items: center; margin-top: 1.5rem; padding: .8rem; border-radius: .85rem; background: #f8fafc; }
.session-card strong, .session-card small { display: block; max-width: 180px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.session-card strong { font-size: .76rem; }.session-card small { margin-top: .2rem; color: #94a3b8; font-size: .6rem; }
.status-dot { width: .55rem; height: .55rem; flex: none; border-radius: 50%; background: #22c55e; }.status-dot.active { background: #6366f1; }
.sidebar-note { margin-top: auto; color: #64748b; font-size: .72rem; line-height: 1.7; }
.chat-shell { min-height: calc(100vh - 2.5rem); display: grid; grid-template-rows: auto minmax(0, 1fr) auto; overflow: hidden; }
header { padding: 1.2rem 1.75rem; border-bottom: 1px solid #e2e8f0; }
.message-list { overflow-y: auto; padding: 1.75rem max(1rem, calc((100% - 800px) / 2)); }
.message { display: flex; gap: .8rem; margin-bottom: 1.8rem; }.message.user { flex-direction: row-reverse; }
.avatar { width: 2.1rem; height: 2.1rem; display: grid; place-items: center; flex: none; border-radius: .7rem; background: #0f172a; color: white; font-size: .65rem; font-weight: 700; }
.user .avatar { background: #e0e7ff; color: #4338ca; }.message-body { width: min(88%, 720px); }.user .message-body { text-align: right; }
.message-meta { margin-bottom: .35rem; color: #94a3b8; font-size: .65rem; }.text-part { white-space: pre-wrap; line-height: 1.75; color: #334155; font-size: .9rem; }
.user .text-part { display: inline-block; padding: .65rem 1rem; border-radius: 1rem 1rem .25rem 1rem; background: #4f46e5; color: white; text-align: left; }
.text-part.failed { color: #be123c; }.cursor { display: inline-block; width: 2px; height: .9rem; margin-left: .2rem; background: #6366f1; animation: blink 1s infinite; }
.message-actions { display: flex; align-items: center; gap: .4rem; margin-top: .55rem; }.message-actions > button { border: 0; background: transparent; color: #94a3b8; font-size: .68rem; cursor: pointer; }
footer { padding: 1rem max(1rem, calc((100% - 800px) / 2)) 1.5rem; }.composer { display: flex; gap: .6rem; padding: .55rem; border: 1px solid #dbe2ea; border-radius: 1.1rem; background: white; box-shadow: 0 8px 28px rgb(15 23 42 / .06); }
.composer textarea { min-height: 2.5rem; flex: 1; resize: vertical; border: 0; padding: .55rem .7rem; outline: none; font: inherit; }.composer button { border: 0; border-radius: .8rem; padding: 0 1.15rem; background: #4f46e5; color: white; cursor: pointer; }.composer button:disabled { background: #cbd5e1; cursor: not-allowed; }
.error-banner { margin: 0 0 .5rem; padding: .55rem .8rem; border-radius: .6rem; background: #fff1f2; color: #be123c; font-size: .72rem; }
@keyframes blink { 50% { opacity: 0; } }
@media (max-width: 800px) { .workspace { display: block; padding: 0; }.sidebar { display: none; }.chat-shell { min-height: 100vh; border-radius: 0; } }
</style>
