<script setup lang="ts">
import {computed} from 'vue'
import type {ResultPayload, SurfaceComponent} from '../api/chat'

const props = defineProps<{
    component: SurfaceComponent
    result: ResultPayload
}>()

interface TimelineItem {
    time: string
    title: string
    description: string
    group: string
}

const items = computed<TimelineItem[]>(() => {
    const encoding = (props.component.props.encoding ?? {}) as Record<string, string>
    const data = props.result.data
    const rows: Record<string, unknown>[] = Array.isArray(data) ? data as Record<string, unknown>[] : []

    return rows
        .map((row) => ({
            time: String(row[encoding.time ?? 'time'] ?? ''),
            title: String(row[encoding.title ?? 'title'] ?? ''),
            description: encoding.description ? String(row[encoding.description] ?? '') : '',
            group: encoding.group ? String(row[encoding.group] ?? '') : '',
        }))
        .filter((item) => item.time || item.title)
        .sort((a, b) => (a.time < b.time ? 1 : a.time > b.time ? -1 : 0))
})

const MAX_ITEMS = 50
const displayItems = computed(() => items.value.slice(0, MAX_ITEMS))

function formatTime(value: string): string {
    if (!value) return ''
    return value.replace('T', ' ').replace(/([+-]\d{2}:\d{2}|Z)$/, '').replace(/\.\d+/, '')
}
</script>

<template>
  <div class="w-full px-2 py-1">
    <p v-if="component.props.title" class="px-2 pb-2 text-sm font-medium text-slate-700">
      {{ component.props.title }}
    </p>
    <ol class="relative ml-3 border-l border-slate-200">
      <li v-for="(item, index) in displayItems" :key="index" class="relative py-2.5 pl-5">
        <span
            class="absolute -left-[5px] top-4 h-2.5 w-2.5 rounded-full border-2 border-white bg-brand-500 shadow"></span>
        <div class="flex flex-wrap items-baseline gap-x-2">
          <span v-if="item.time" class="font-mono text-[11px] text-slate-400">{{ formatTime(item.time) }}</span>
          <span v-if="item.group"
                class="rounded bg-brand-50 px-1.5 py-0.5 text-[10px] text-brand-700">{{ item.group }}</span>
        </div>
        <p class="mt-0.5 text-sm font-medium text-slate-700">{{ item.title || '—' }}</p>
        <p v-if="item.description" class="mt-0.5 text-xs leading-5 text-slate-500">{{ item.description }}</p>
      </li>
    </ol>
    <p v-if="items.length === 0" class="px-2 py-6 text-center text-xs text-slate-400">暂无时间线数据</p>
    <p v-else-if="items.length > MAX_ITEMS" class="px-2 pt-1.5 text-[10px] text-slate-400">
      共 {{ items.length }} 条，仅显示最近 {{ MAX_ITEMS }} 条
    </p>
  </div>
</template>
