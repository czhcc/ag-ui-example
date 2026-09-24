<script setup lang="ts">
import {computed, ref} from 'vue'

export interface RunLogEntry {
    id: number
    time: string
    kind: 'run_start' | 'llm_start' | 'tool_start' | 'tool_end' | 'ui' | 'llm_end' | 'run_end' | 'error'
    title: string
    detail?: string
}

const props = defineProps<{ entries: RunLogEntry[] }>()

const open = ref(false)

const kindMeta: Record<RunLogEntry['kind'], { label: string; dot: string; text: string }> = {
    run_start: {label: 'RUN', dot: 'bg-slate-400', text: 'text-slate-500'},
    llm_start: {label: 'LLM', dot: 'bg-brand-500', text: 'text-brand-700'},
    tool_start: {label: 'TOOL', dot: 'bg-amber-500', text: 'text-amber-700'},
    tool_end: {label: 'TOOL', dot: 'bg-amber-500', text: 'text-amber-700'},
    ui: {label: 'UI', dot: 'bg-emerald-500', text: 'text-emerald-700'},
    llm_end: {label: 'LLM', dot: 'bg-brand-500', text: 'text-brand-700'},
    run_end: {label: 'RUN', dot: 'bg-slate-400', text: 'text-slate-500'},
    error: {label: 'ERROR', dot: 'bg-rose-500', text: 'text-rose-600'},
}

const totalTools = computed(() => props.entries.filter((e) => e.kind === 'tool_end').length)
const totalUi = computed(() => props.entries.filter((e) => e.kind === 'ui').length)
const expanded = ref<Record<number, boolean>>({})

function toggle(id: number) {
    expanded.value[id] = !expanded.value[id]
}

function prettyDetail(entry: RunLogEntry): string {
    if (!entry.detail) return ''
    if (entry.kind === 'tool_start') return ''
    try {
        const parsed: unknown = JSON.parse(entry.detail)
        if (entry.kind === 'tool_end' && parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
            const value = parsed as Record<string, unknown>
            return JSON.stringify({
                kind: value.kind,
                summary: value.summary,
                resultRef: value.resultRef,
            }, null, 2)
        }
        return JSON.stringify(parsed, null, 2).slice(0, 2_000)
    } catch {
        return entry.detail.slice(0, 2_000)
    }
}
</script>

<template>
  <button
      class="flex items-center gap-1 rounded-lg px-2 py-1 text-[11px] text-slate-400 opacity-0 transition hover:bg-slate-100 hover:text-slate-600 group-hover:opacity-100 focus:opacity-100"
      type="button" @click="open = true">
    <svg viewBox="0 0 24 24" class="h-3.5 w-3.5" fill="none" aria-hidden="true">
      <path d="M4 6h16M4 6l2 2.5L4 11m0 5h16m-16 0 2 2.5L4 21M4 11h.01M4 21h.01"
            stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
    </svg>
    日志
  </button>

  <Teleport to="body">
    <Transition name="drawer">
      <div v-if="open" class="fixed inset-0 z-50 flex justify-end" role="dialog" aria-label="执行日志">
        <div class="absolute inset-0 bg-slate-900/30 backdrop-blur-sm" @click="open = false"></div>
        <aside class="relative flex h-full w-full max-w-xl flex-col bg-white shadow-2xl">
          <header class="flex shrink-0 items-center justify-between border-b border-slate-200 px-5 py-4">
            <div>
              <h2 class="text-sm font-semibold text-slate-800">本轮执行日志</h2>
              <p class="mt-0.5 text-[11px] text-slate-400">
                工具调用 {{ totalTools }} 次 · UI 组件 {{ totalUi }} 个
              </p>
            </div>
            <button
                class="grid h-8 w-8 place-items-center rounded-lg text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
                type="button" aria-label="关闭" @click="open = false">
              <svg viewBox="0 0 24 24" class="h-4 w-4" fill="none">
                <path d="m6 6 12 12M18 6 6 18" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </button>
          </header>

          <div class="flex-1 overflow-y-auto px-5 py-4">
            <ol class="relative ml-2 border-l border-slate-200">
              <li v-for="entry in entries" :key="entry.id" class="relative py-2.5 pl-5">
                <span class="absolute -left-[5px] top-3.5 h-2.5 w-2.5 rounded-full border-2 border-white shadow"
                      :class="kindMeta[entry.kind].dot"></span>
                <div class="flex flex-wrap items-baseline gap-x-2">
                  <span class="font-mono text-[10px] text-slate-400">{{ entry.time }}</span>
                  <span class="rounded px-1.5 py-0.5 text-[10px] font-medium"
                        :class="[kindMeta[entry.kind].text, entry.kind === 'tool_start' || entry.kind === 'tool_end' ? 'bg-amber-50' : 'bg-slate-100']">
                    {{ kindMeta[entry.kind].label }}
                  </span>
                  <span class="text-xs font-medium text-slate-700">{{ entry.title }}</span>
                  <button v-if="entry.detail && entry.detail.length > 60"
                          class="text-[10px] text-brand-600 hover:underline" type="button"
                          @click="toggle(entry.id)">{{ expanded[entry.id] ? '收起' : '展开' }}
                  </button>
                </div>
                <pre v-if="entry.detail && (entry.detail.length <= 60 || expanded[entry.id])"
                     class="mt-1 max-h-64 overflow-auto whitespace-pre-wrap break-all rounded-lg bg-slate-50 px-3 py-2 font-mono text-[11px] leading-5 text-slate-600">{{ prettyDetail(entry) }}</pre>
              </li>
            </ol>
          </div>
        </aside>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.drawer-enter-active,
.drawer-leave-active {
    transition: opacity 0.2s ease;
}

.drawer-enter-active aside,
.drawer-leave-active aside {
    transition: transform 0.25s ease;
}

.drawer-enter-from,
.drawer-leave-to {
    opacity: 0;
}

.drawer-enter-from aside,
.drawer-leave-to aside {
    transform: translateX(24px);
}
</style>
