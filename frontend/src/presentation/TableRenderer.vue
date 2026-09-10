<script setup lang="ts">
import {computed} from 'vue'
import type {ResultPayload, SurfaceComponent} from '../api/chat'

const props = defineProps<{
    component: SurfaceComponent
    result: ResultPayload
}>()

interface Column {
    field: string
    label: string
}

const rows = computed<Record<string, unknown>[]>(() => {
    const data = props.result.data
    if (Array.isArray(data)) return data as Record<string, unknown>[]
    return []
})

const columns = computed<Column[]>(() => {
    const encoding = props.component.props.encoding ?? {}
    const declared = (encoding as { columns?: Column[] }).columns
    if (Array.isArray(declared) && declared.length > 0) {
        return declared.filter((c) => c && typeof c.field === 'string')
    }
    const first = rows.value[0] ?? {}
    return Object.keys(first).map((field) => ({field, label: field}))
})

const MAX_ROWS = 100

const displayRows = computed(() => rows.value.slice(0, MAX_ROWS))

function cell(row: Record<string, unknown>, field: string): string {
    const value = row[field]
    if (value === null || value === undefined) return ''
    if (typeof value === 'object') return JSON.stringify(value)
    return String(value)
}
</script>

<template>
  <div class="w-full px-1 py-1">
    <p v-if="component.props.title" class="px-2 pb-2 text-sm font-medium text-slate-700">
      {{ component.props.title }}
    </p>
    <div class="max-h-96 overflow-auto rounded-xl border border-slate-200">
      <table class="w-full border-collapse text-left text-xs">
        <thead class="sticky top-0 bg-slate-50 text-slate-500">
        <tr>
          <th v-for="col in columns" :key="col.field" class="whitespace-nowrap px-3 py-2 font-medium">
            {{ col.label }}
          </th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(row, index) in displayRows" :key="index"
            class="border-t border-slate-100 text-slate-700 even:bg-slate-50/50">
          <td v-for="col in columns" :key="col.field" class="px-3 py-1.5">{{ cell(row, col.field) }}</td>
        </tr>
        </tbody>
      </table>
    </div>
    <p v-if="rows.length > MAX_ROWS" class="px-2 pt-1.5 text-[10px] text-slate-400">
      共 {{ rows.length }} 条，仅显示前 {{ MAX_ROWS }} 条
    </p>
  </div>
</template>
