<script setup lang="ts">
import {nextTick, onBeforeUnmount, reactive, ref, watch} from 'vue'
import {streamChat} from '@ac/agent-ui'
import type {UiSurface} from '@ac/agent-ui'
import UiSurfacePart from '@ac/agent-ui/components/UiSurfacePart.vue'
import RunLogPanel from './chat/RunLogPanel.vue'
import type {RunLogEntry} from './chat/RunLogPanel.vue'

type Role = 'user' | 'assistant'

type MessagePart =
    | { kind: 'text'; text: string }
    | { kind: 'ui'; surface: UiSurface }

interface Message {
  id: string
  role: Role
  parts: MessagePart[]
  logs: RunLogEntry[]
  streaming?: boolean
  failed?: boolean
}

let logSeq = 0

function nowTime(): string {
    return new Date().toLocaleTimeString('zh-CN', {hour12: false})
}

function pushLog(message: Message, kind: RunLogEntry['kind'], title: string, detail?: string) {
    message.logs.push({id: ++logSeq, time: nowTime(), kind, title, detail})
}

const starterPrompts = [
  '统计 person-001 最近一月的活动城市',
  '帮我概览目前可用的分析能力',
  '查询活动最频繁的城市并总结',
]

const createId = () =>
    globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(16).slice(2)}`

const createConversationId = () => `conv-${createId()}`

const conversationId = ref(sessionStorage.getItem('agent-conversation-id') ?? createConversationId())
const input = ref('')
const isSending = ref(false)
const errorMessage = ref('')
const messageList = ref<HTMLElement | null>(null)
const composer = ref<HTMLTextAreaElement | null>(null)
const controller = ref<AbortController | null>(null)
const messages = ref<Message[]>([
  {
    id: createId(),
    role: 'assistant',
    parts: [{kind: 'text', text: '你好，我是你的分析智能体。告诉我你想查询或分析什么，我会调用合适的工具协助你。'}],
    logs: [],
  },
])

sessionStorage.setItem('agent-conversation-id', conversationId.value)

watch(
    messages,
    async () => {
      await nextTick()
      const el = messageList.value
      if (!el) return
      const nearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 160
      if (nearBottom || isSending.value) {
        el.scrollTo({top: el.scrollHeight, behavior: isSending.value ? 'auto' : 'smooth'})
      }
    },
    {deep: true},
)

function resizeComposer() {
  if (!composer.value) return
  composer.value.style.height = '0px'
  composer.value.style.height = `${Math.min(composer.value.scrollHeight, 160)}px`
}

function usePrompt(prompt: string) {
  input.value = prompt
  nextTick(() => {
    resizeComposer()
    composer.value?.focus()
  })
}

async function onDrillDown(event: { question: string; value: unknown }) {
  if (isSending.value) return
  await sendContent(`🔗 ${event.question}`, false)
}

async function sendMessage() {
  const content = input.value.trim()
  if (!content || isSending.value) return
  await sendContent(content, true)
}

async function sendContent(content: string, fromUser: boolean) {
  if (isSending.value) return

  errorMessage.value = ''
  if (fromUser) input.value = ''
  nextTick(resizeComposer)

  messages.value.push({id: createId(), role: 'user', parts: [{kind: 'text', text: content}], logs: []})
  const reply = reactive<Message>({
    id: createId(),
    role: 'assistant',
    parts: [{kind: 'text', text: ''}],
    logs: [],
    streaming: true,
  })
  messages.value.push(reply)

  isSending.value = true
  controller.value = new AbortController()

  try {
    await streamChat(
        {conversationId: conversationId.value, message: content},
        (event) => {
          conversationId.value = event.conversationId
          sessionStorage.setItem('agent-conversation-id', event.conversationId)

          if (event.type === 'delta' && event.content) {
            if (reply.logs.length === 0 || reply.logs[reply.logs.length - 1].kind !== 'llm_start') {
              pushLog(reply, 'llm_start', 'LLM 开始生成回复')
            }
            const last = reply.parts[reply.parts.length - 1]
            if (last && last.kind === 'text') last.text += event.content
            else reply.parts.push({kind: 'text', text: event.content})
          } else if (event.type === 'tool_start') {
            pushLog(reply, 'tool_start', `调用 ${event.toolName}`, event.arguments ?? undefined)
          } else if (event.type === 'tool_end') {
            const detail = event.detail
                ? event.detail
                : [event.success === null ? null : `success: ${event.success}`,
                    event.resultRef ? `resultRef: ${event.resultRef}` : null,
                ].filter(Boolean).join('\n') || undefined
            pushLog(reply, 'tool_end',
                `${event.toolName} 返回${event.success === false ? '（失败）' : ''}${event.resultRef ? ` · ${event.resultRef}` : ''}`,
                detail)
          } else if (event.type === 'ui') {
            const {type: _type, ...surface} = event
            reply.parts.push({kind: 'ui', surface})
            pushLog(reply, 'ui', `渲染组件 ${surface.components[0]?.type ?? 'Unknown'}`,
                JSON.stringify(surface))
          } else if (event.type === 'done') {
            pushLog(reply, 'llm_end', 'LLM 回复完成')
            pushLog(reply, 'run_end', '本轮执行结束')
            reply.streaming = false
          } else if (event.type === 'error') {
            reply.streaming = false
            reply.failed = true
            const fallback = '智能体暂时无法响应，请稍后再试。'
            const last = reply.parts[reply.parts.length - 1]
            if (last && last.kind === 'text' && !last.text) last.text = event.content ?? fallback
            else reply.parts.push({kind: 'text', text: event.content ?? fallback})
            pushLog(reply, 'error', '执行出错', event.content ?? fallback)
            errorMessage.value = event.content ?? fallback
          }
        },
        controller.value.signal,
    )
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') return
    reply.streaming = false
    reply.failed = true
    reply.parts.push({kind: 'text', text: '连接智能体失败，请确认 Agent Client 已启动后重试。'})
    pushLog(reply, 'error', '连接失败', error instanceof Error ? error.message : '连接失败')
    errorMessage.value = error instanceof Error ? error.message : '连接失败'
  } finally {
    reply.streaming = false
    isSending.value = false
    controller.value = null
  }
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    void sendMessage()
  }
}

function resetConversation() {
  controller.value?.abort()
  conversationId.value = createConversationId()
  sessionStorage.setItem('agent-conversation-id', conversationId.value)
  messages.value = [
    {
      id: createId(),
      role: 'assistant',
      parts: [{kind: 'text', text: '新对话已创建。今天想从哪里开始？'}],
      logs: [],
    },
  ]
  errorMessage.value = ''
  nextTick(() => composer.value?.focus())
}

function messageText(message: Message): string {
  return message.parts.filter((p): p is Extract<MessagePart, { kind: 'text' }> => p.kind === 'text')
      .map((p) => p.text)
      .join('\n')
}

async function copyMessage(content: string) {
  await navigator.clipboard?.writeText(content)
}

onBeforeUnmount(() => controller.value?.abort())
</script>

<template>
  <main class="relative flex min-h-screen overflow-hidden bg-mist text-ink">
    <div class="ambient ambient-one" aria-hidden="true"></div>
    <div class="ambient ambient-two" aria-hidden="true"></div>

    <section class="relative z-10 mx-auto flex h-screen w-full max-w-[1480px] gap-5 p-3 sm:p-5 lg:p-7">
      <aside
          class="hidden w-72 shrink-0 flex-col rounded-[28px] border border-white/80 bg-white/70 p-5 shadow-panel backdrop-blur-xl lg:flex">
        <div class="flex items-center gap-3 px-1">
          <div class="grid h-11 w-11 place-items-center rounded-2xl bg-ink text-white shadow-lg shadow-slate-900/15">
            <svg viewBox="0 0 24 24" class="h-6 w-6" fill="none" aria-hidden="true">
              <path d="M7 8.5h10M7 12h6m-7.5 7 1.3-3.25A7.5 7.5 0 1 1 19.5 10.3 7.5 7.5 0 0 1 8.7 17Z"
                    stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <div>
            <p class="text-[15px] font-semibold tracking-tight">Agent Workspace</p>
            <p class="mt-0.5 text-xs text-slate-500">AI MCP Platform</p>
          </div>
        </div>

        <button
            class="mt-8 flex w-full items-center justify-center gap-2 rounded-2xl bg-ink px-4 py-3 text-sm font-medium text-white transition hover:bg-slate-700 focus:outline-none focus:ring-4 focus:ring-slate-300"
            type="button" @click="resetConversation">
          <svg viewBox="0 0 24 24" class="h-4 w-4" fill="none" aria-hidden="true">
            <path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
          新建对话
        </button>

        <div class="mt-8">
          <p class="px-2 text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-400">当前会话</p>
          <div class="mt-3 rounded-2xl border border-slate-200/80 bg-white/80 p-3.5">
            <div class="flex items-center gap-2.5">
              <span class="relative flex h-2.5 w-2.5">
                <span v-if="isSending"
                      class="absolute inline-flex h-full w-full animate-ping rounded-full bg-brand-500 opacity-60"></span>
                <span class="relative inline-flex h-2.5 w-2.5 rounded-full"
                      :class="isSending ? 'bg-brand-500' : 'bg-emerald-500'"></span>
              </span>
              <span class="text-sm font-medium">{{ isSending ? '智能体思考中' : '准备就绪' }}</span>
            </div>
            <p class="mt-2 truncate font-mono text-[10px] text-slate-400">{{ conversationId }}</p>
          </div>
        </div>

        <div class="mt-auto rounded-2xl bg-brand-50 p-4">
          <div class="flex items-center gap-2 text-xs font-semibold text-brand-700">
            <span class="h-1.5 w-1.5 rounded-full bg-brand-500"></span>
            Agent Client
          </div>
          <p class="mt-2 text-xs leading-5 text-slate-500">回答会以流式方式实时显示，复杂问题可能会调用 MCP 工具。</p>
        </div>
      </aside>

      <div
          class="flex min-w-0 flex-1 flex-col overflow-hidden rounded-[26px] border border-white/90 bg-white/80 shadow-panel backdrop-blur-xl sm:rounded-[30px]">
        <header class="flex h-[72px] shrink-0 items-center justify-between border-b border-slate-200/70 px-4 sm:px-7">
          <div class="flex items-center gap-3">
            <div
                class="grid h-10 w-10 place-items-center rounded-2xl bg-gradient-to-br from-brand-500 to-brand-700 text-white lg:hidden">
              <svg viewBox="0 0 24 24" class="h-5 w-5" fill="none" aria-hidden="true">
                <path d="M7 8.5h10M7 12h6m-7.5 7 1.3-3.25A7.5 7.5 0 1 1 19.5 10.3 7.5 7.5 0 0 1 8.7 17Z"
                      stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <div>
              <h1 class="text-[15px] font-semibold tracking-tight sm:text-base">智能分析助手</h1>
              <p class="mt-0.5 flex items-center gap-1.5 text-[11px] text-slate-500 sm:text-xs">
                <span class="h-1.5 w-1.5 rounded-full bg-emerald-500"></span>
                由 Agent Client 驱动
              </p>
            </div>
          </div>
          <button
              class="grid h-10 w-10 place-items-center rounded-xl text-slate-500 transition hover:bg-slate-100 hover:text-slate-800 lg:hidden"
              type="button" title="新建对话" aria-label="新建对话" @click="resetConversation">
            <svg viewBox="0 0 24 24" class="h-5 w-5" fill="none" aria-hidden="true">
              <path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            </svg>
          </button>
        </header>

        <div ref="messageList" class="message-scroll flex-1 overflow-y-auto px-4 py-6 sm:px-8 sm:py-8">
          <div class="mx-auto flex w-full max-w-3xl flex-col gap-7">
            <div v-for="message in messages" :key="message.id" class="group flex gap-3.5 sm:gap-4"
                 :class="message.role === 'user' ? 'flex-row-reverse' : ''">
              <div v-if="message.role === 'assistant'"
                   class="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-ink text-white shadow-md shadow-slate-900/10">
                <svg viewBox="0 0 24 24" class="h-[18px] w-[18px]" fill="none" aria-hidden="true">
                  <path d="M12 3.5 14 9l5.5 2-5.5 2-2 5.5-2-5.5-5.5-2L10 9l2-5.5Z" stroke="currentColor"
                        stroke-width="1.5" stroke-linejoin="round"/>
                </svg>
              </div>
              <div v-else
                   class="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-brand-100 text-xs font-bold text-brand-700">
                你
              </div>

              <div class="min-w-0 max-w-[85%] sm:max-w-[78%]">
                <div class="mb-1.5 flex items-center gap-2 text-[11px] font-medium text-slate-400"
                     :class="message.role === 'user' ? 'justify-end' : ''">
                  {{ message.role === 'assistant' ? 'Agent' : '你' }}
                  <span v-if="message.streaming" class="text-brand-600">正在回复</span>
                </div>
                <div class="flex flex-col gap-1"
                     :class="message.role === 'user' ? 'items-end' : 'items-start'">
                  <template v-for="(part, index) in message.parts" :key="index">
                    <div v-if="part.kind === 'text'"
                         class="message-bubble relative whitespace-pre-wrap break-words text-[14px] leading-7 sm:text-[15px]"
                         :class="[
                          message.role === 'user'
                            ? 'rounded-[20px] rounded-tr-md bg-brand-600 px-4 py-2.5 text-white shadow-md shadow-blue-700/10'
                            : 'pr-3 text-slate-700',
                          message.failed ? 'text-rose-600' : '',
                        ]">
                      <span v-if="part.text">{{ part.text }}</span>
                      <span v-if="message.streaming && !part.text && index === 0"
                            class="inline-flex items-center gap-1.5 py-2" aria-label="正在生成回答">
                        <i class="typing-dot"></i><i class="typing-dot"></i><i class="typing-dot"></i>
                      </span>
                      <span v-else-if="message.streaming && index === message.parts.length - 1"
                            class="ml-1 inline-block h-4 w-0.5 animate-pulse bg-brand-500 align-middle"></span>
                    </div>
                    <UiSurfacePart v-else-if="part.kind === 'ui'" :surface="part.surface" class="w-full"
                                   @drill-down="onDrillDown"/>
                  </template>
                </div>
                <div v-if="message.role === 'assistant' && messageText(message) && !message.streaming"
                     class="mt-2 flex items-center gap-0.5 opacity-0 transition group-hover:opacity-100 focus-within:opacity-100">
                  <button
                      class="flex items-center gap-1 rounded-lg px-2 py-1 text-[11px] text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
                      type="button" @click="copyMessage(messageText(message))">
                    <svg viewBox="0 0 24 24" class="h-3.5 w-3.5" fill="none" aria-hidden="true">
                      <rect x="8" y="8" width="11" height="11" rx="2" stroke="currentColor" stroke-width="1.7"/>
                      <path d="M16 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h2" stroke="currentColor"
                            stroke-width="1.7"/>
                    </svg>
                    复制
                  </button>
                  <RunLogPanel v-if="message.logs.length > 0" :entries="message.logs"/>
                </div>
              </div>
            </div>

            <div v-if="messages.length === 1" class="ml-12 pt-1 sm:ml-14">
              <p class="mb-3 text-xs font-medium text-slate-400">你可以这样问</p>
              <div class="flex flex-wrap gap-2">
                <button v-for="prompt in starterPrompts" :key="prompt"
                        class="rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-left text-xs leading-5 text-slate-600 shadow-sm transition hover:-translate-y-0.5 hover:border-brand-100 hover:bg-brand-50 hover:text-brand-700"
                        type="button" @click="usePrompt(prompt)">
                  {{ prompt }}
                </button>
              </div>
            </div>
          </div>
        </div>

        <footer class="shrink-0 px-4 pb-4 sm:px-8 sm:pb-7">
          <div class="mx-auto w-full max-w-3xl">
            <p v-if="errorMessage" class="mb-2 rounded-xl bg-rose-50 px-3 py-2 text-xs text-rose-600">{{
                errorMessage
              }}</p>
            <div
                class="flex items-end gap-2 rounded-[22px] border border-slate-200 bg-white p-2 shadow-composer transition focus-within:border-brand-500 focus-within:ring-4 focus-within:ring-brand-100/70">
              <textarea ref="composer" v-model="input" rows="1" maxlength="8000"
                        class="max-h-40 min-h-[44px] flex-1 resize-none bg-transparent px-3 py-2.5 text-sm leading-6 text-slate-700 outline-none placeholder:text-slate-400"
                        :disabled="isSending" placeholder="输入你的问题…" aria-label="对话内容" @input="resizeComposer"
                        @keydown="handleKeydown"></textarea>
              <button
                  class="grid h-11 w-11 shrink-0 place-items-center rounded-2xl bg-brand-600 text-white shadow-md shadow-blue-700/20 transition hover:bg-brand-700 disabled:cursor-not-allowed disabled:bg-slate-200 disabled:text-slate-400 disabled:shadow-none"
                  type="button" :disabled="!input.trim() || isSending" aria-label="发送消息" @click="sendMessage">
                <svg v-if="!isSending" viewBox="0 0 24 24" class="h-5 w-5" fill="none" aria-hidden="true">
                  <path d="m5 12 14-7-4.5 14-3-5.5L5 12Zm6.5 1.5L19 5" stroke="currentColor" stroke-width="1.8"
                        stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <span v-else class="h-4 w-4 animate-spin rounded-full border-2 border-slate-400 border-t-white"></span>
              </button>
            </div>
            <p class="mt-2.5 text-center text-[10px] text-slate-400 sm:text-[11px]">Enter 发送 · Shift + Enter 换行 · AI
              生成内容仅供参考</p>
          </div>
        </footer>
      </div>
    </section>
  </main>
</template>
