<script setup lang="ts">
import {onMounted, onBeforeUnmount, ref, watch} from 'vue'
import {AppBridge} from '@modelcontextprotocol/ext-apps/app-bridge'
import type {McpAppDescriptor} from '../mcp-apps/types'
import {appRequest, appOptions} from '../mcp-apps/client'
import {OriginBoundTransport} from '../mcp-apps/transport'

const props = defineProps<{descriptor: McpAppDescriptor}>()
const frame = ref<HTMLIFrameElement | null>(null)
const status = ref('正在加载应用')
const error = ref('')
const closed = ref(false)
const height = ref(560)
let bridge: AppBridge | undefined
let controller = new AbortController()
let initialized = false
let revision = props.descriptor.revision
let generation = 0
let timer: ReturnType<typeof setTimeout> | undefined
async function load() {
  const ownGeneration = ++generation
  controller.abort(); controller = new AbortController()
  await bridge?.close(); bridge = undefined; initialized = false; error.value = ''
  status.value = '正在加载应用'
  if (timer) clearTimeout(timer)
  try {
    const config = appOptions()
    const url = new URL(config.sandboxUrl, window.location.href)
    if (url.origin === window.location.origin || !['https:', 'http:'].includes(url.protocol)
      || (url.protocol === 'http:' && !['localhost', '127.0.0.1'].includes(url.hostname))) throw new Error('APP_SANDBOX_ORIGIN_INVALID')
    const snapshot = await appRequest<McpAppDescriptor>(props.descriptor.appInstanceId, '', undefined, undefined, controller.signal)
    if (snapshot.status !== 'ACTIVE') {closed.value = true; status.value = '应用已关闭'; return}
    const resource = await appRequest<{html: string; resourceHash: string}>(props.descriptor.appInstanceId, '/resource', undefined, undefined, controller.signal)
    if (resource.resourceHash !== props.descriptor.resourceHash) throw new Error('APP_VERSION_MISMATCH')
    const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(resource.html))
    const hash = 'sha256:' + [...new Uint8Array(digest)].map(v => v.toString(16).padStart(2, '0')).join('')
    if (hash !== resource.resourceHash) throw new Error('APP_VERSION_MISMATCH')
    const result = await appRequest<Parameters<AppBridge['sendToolResult']>[0]>(props.descriptor.appInstanceId, '/latest-result', undefined, undefined, controller.signal)
    if (ownGeneration !== generation || !frame.value?.contentWindow) return
    revision = snapshot.revision
    const current = new AppBridge(null, {name: 'ac-agent-host', version: '1.0.0'}, {
      serverTools: {}, message: {text: {}}, updateModelContext: {text: {}},
      sandbox: {csp: {connectDomains: [], resourceDomains: []}},
    })
    bridge = current
    current.onsandboxready = () => {void current.sendSandboxResourceReady({html: resource.html, sandbox: 'allow-scripts', csp: {connectDomains: [], resourceDomains: []}})}
    current.oninitialized = () => {
      if (ownGeneration !== generation) return
      initialized = true; status.value = '应用已就绪'; if (timer) clearTimeout(timer)
      void current.sendToolInput({arguments: {}}).then(() => current.sendToolResult(result)).catch(showError)
    }
    current.onsizechange = size => {height.value = Math.max(320, Math.min(900, size.height ?? 560))}
    current.oncalltool = async params => {
      if (!initialized || closed.value) throw new Error('APP_NOT_READY')
      let response: {revision: number; result: Awaited<ReturnType<NonNullable<AppBridge['oncalltool']>>>}
      try {response = await appRequest(props.descriptor.appInstanceId, '/tool-calls', {
        clientRequestId: crypto.randomUUID(), expectedRevision: revision, toolName: params.name, arguments: params.arguments ?? {},
      }, undefined, controller.signal)} catch (e) {
        if (e instanceof Error && ['APP_ACCESS_DENIED', 'APP_NOT_ALLOWED', 'APP_EXPIRED', 'APP_CLOSED', 'APP_VERSION_MISMATCH'].includes(e.message)) {
          showError(e); setTimeout(() => {void dispose()}, 0)
        }
        throw e
      }
      revision = response.revision
      return response.result
    }
    current.onupdatemodelcontext = async params => {
      if (!initialized || closed.value) throw new Error('APP_NOT_READY')
      await appRequest(props.descriptor.appInstanceId, '/context', params.structuredContent ?? {})
      return {}
    }
    current.onmessage = async params => {
      if (!initialized || closed.value || config.isBusy?.()) throw new Error('APP_SESSION_BUSY')
      if (!config.onFollowup) throw new Error('APP_FOLLOWUP_UNAVAILABLE')
      const question = params.content.filter(v => v.type === 'text').map(v => v.type === 'text' ? v.text : '').join('\n')
      if (!question.trim()) throw new Error('APP_INPUT_INVALID')
      const intent = await appRequest<{appFollowupRef: string; threadId: string; parentRunId: string}>(props.descriptor.appInstanceId, '/followups', {question})
      await config.onFollowup({...intent, question})
      return {}
    }
    current.onerror = showError
    await current.connect(new OriginBoundTransport(frame.value.contentWindow, url.origin))
    timer = setTimeout(() => {showError(new Error('APP_INIT_TIMEOUT')); void current.close()}, 10000)
    url.searchParams.set('parentOrigin', window.location.origin)
    frame.value.src = url.href
  } catch (e) {if (ownGeneration === generation && !controller.signal.aborted) showError(e)}
}
function showError(e: unknown) {error.value = e instanceof Error ? e.message : 'APP_LOAD_FAILED'; status.value = '应用暂不可用'}
async function close() {
  try {await appRequest(props.descriptor.appInstanceId, '', undefined, 'DELETE'); closed.value = true; status.value = '应用已关闭'; await dispose()}
  catch (e) {showError(e)}
}
async function dispose() {generation++; controller.abort(); if (timer) clearTimeout(timer); await bridge?.close(); bridge = undefined}
onMounted(() => {void load()})
watch(() => props.descriptor.status, status => {
  if (status === 'CLOSED') {closed.value = true; void dispose()}
})
watch(() => props.descriptor.revision, () => {
  if (!closed.value && (props.descriptor.needsSync || props.descriptor.resultRef)) void load()
})
onBeforeUnmount(() => {void dispose()})
</script>

<template>
  <section class="mcp-app-part">
    <header><strong>{{ descriptor.title }}</strong><small>{{ status }}</small><button v-if="!closed" @click="close">关闭</button></header>
    <p v-if="error" role="alert">{{ error }} <button @click="load">重新加载界面</button></p>
    <iframe v-show="!closed && !error" ref="frame" :title="descriptor.title" sandbox="allow-scripts allow-same-origin" :style="{height: `${height}px`}"></iframe>
  </section>
</template>
<style scoped>
.mcp-app-part{margin:1rem 0;border:1px solid #dbe2ea;border-radius:12px;overflow:hidden;background:white}
header{display:flex;gap:12px;align-items:center;padding:12px;background:#f8fafc}small{flex:1;color:#64748b}button{cursor:pointer}
iframe{display:block;width:100%;border:0}p{padding:12px;color:#be123c}
</style>
