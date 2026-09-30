<script setup lang="ts">
import {computed, onBeforeUnmount, ref, watch} from 'vue'
import {context} from '../core/registry'
import {ResultUnavailableError} from '../core/stream'
import type {DrillDownEvent, ResultPayload, UiSurface} from '../core/types'

const props = defineProps<{surface: UiSurface}>()
const emit = defineEmits<{(e: 'drill-down', event: DrillDownEvent): void}>()

type Status = 'loading' | 'ready' | 'invalid' | 'unsupported' | 'forbidden' | 'not-found' | 'expired' | 'error'
const status = ref<Status>('loading')
const result = ref<ResultPayload | null>(null)
const errorText = ref('')
let request: AbortController | null = null

const primary = computed(() => props.surface.components[0] ?? null)
const renderer = computed(() => primary.value ? context.resolveRenderer(primary.value.type) : null)
const typeLabel = computed(() => primary.value?.props.title || primary.value?.type || 'UI surface')

watch(
    () => [props.surface.dataRef, props.surface.profile, props.surface.profileVersion,
        JSON.stringify(props.surface.components)],
    () => void load(),
    {immediate: true},
)

async function load() {
    request?.abort()
    result.value = null
    errorText.value = ''
    if (props.surface.validationIssue) {
        status.value = 'invalid'
        errorText.value = props.surface.validationIssue.message
        return
    }
    if (!primary.value || !renderer.value) {
        status.value = 'unsupported'
        return
    }
    status.value = 'loading'
    request = new AbortController()
    const active = request
    try {
        const loaded = await context.fetchResult(props.surface.dataRef, {
            threadId: props.surface.threadId,
            runId: props.surface.runId,
            signal: active.signal,
        })
        if (request !== active || active.signal.aborted) return
        result.value = loaded
        status.value = 'ready'
    } catch (error) {
        if (request !== active || active.signal.aborted) return
        if (error instanceof DOMException && error.name === 'AbortError') return
        if (error instanceof ResultUnavailableError) {
            status.value = error.reason === 'http-error' ? 'error' : error.reason
            errorText.value = error.message
        } else {
            status.value = 'error'
            errorText.value = error instanceof Error ? error.message : 'Result loading failed'
        }
    }
}

onBeforeUnmount(() => request?.abort())
</script>

<template>
  <section class="agent-ui-surface" :data-surface-id="surface.surfaceId">
    <div v-if="status === 'loading'" class="agent-ui-notice" role="status" aria-live="polite">
      <span class="agent-ui-spinner"></span>
      正在加载 {{ typeLabel }}…
    </div>

    <div v-else-if="status === 'invalid'" class="agent-ui-notice agent-ui-warning">
      <strong>无法显示此内容</strong>
      <span>{{ errorText }}</span>
    </div>

    <div v-else-if="status === 'unsupported'" class="agent-ui-notice agent-ui-warning">
      <strong>暂不支持的组件</strong>
      <span>{{ primary?.type || 'Unknown' }}；文字回答仍可正常使用。</span>
    </div>

    <div v-else-if="status === 'forbidden'" class="agent-ui-notice agent-ui-error">
      <strong>无权访问结果</strong><span>{{ errorText }}</span>
    </div>
    <div v-else-if="status === 'expired'" class="agent-ui-notice agent-ui-warning">
      <strong>结果已经过期</strong><span>请重新运行查询后再试。</span>
    </div>
    <div v-else-if="status === 'not-found'" class="agent-ui-notice agent-ui-warning">
      <strong>结果不存在</strong><span>它可能已被清理，请重新运行查询。</span>
    </div>
    <div v-else-if="status === 'error'" class="agent-ui-notice agent-ui-error">
      <strong>结果加载失败</strong><span>{{ errorText }}</span>
    </div>

    <component
        v-else-if="status === 'ready' && primary && renderer && result"
        :is="renderer"
        :component="primary"
        :result="result"
        @drill-down="(event: DrillDownEvent) => emit('drill-down', {...event, surfaceId: surface.surfaceId})"
    />
  </section>
</template>

<style scoped>
.agent-ui-surface {
    width: 100%; margin: .5rem 0; overflow: hidden; border: 1px solid #e2e8f0;
    border-radius: 1rem; background: #fff; box-shadow: 0 1px 2px rgb(0 0 0 / .05);
}
.agent-ui-notice { display: flex; align-items: center; gap: .6rem; padding: 1rem; color: #475569; font-size: .78rem; }
.agent-ui-notice span { color: #64748b; }
.agent-ui-notice strong { white-space: nowrap; color: #334155; }
.agent-ui-warning { background: #fffbeb; }
.agent-ui-error { background: #fff1f2; }
.agent-ui-error strong { color: #be123c; }
.agent-ui-spinner { width: .9rem; height: .9rem; border: 2px solid #c7d2fe; border-top-color: #4f46e5;
    border-radius: 999px; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
