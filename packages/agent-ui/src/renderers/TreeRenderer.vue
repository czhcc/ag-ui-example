<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import type {DrillDownEvent, RendererProps} from '../core/types'
import {cell, field, rows} from './viewData'

const props = defineProps<RendererProps>()
const emit = defineEmits<{(e: 'drill-down', event: DrillDownEvent): void}>()
const expanded = ref(new Set<string>())
watch(() => props.result.resultRef, () => { expanded.value = new Set() })

const visible = computed(() => {
    const all = rows(props.result).slice(0, Number(props.component.props.options?.limit ?? 500))
    const idKey = field(props.component, 'id')
    const parentKey = field(props.component, 'parentId')
    const children = new Map<string, Record<string, unknown>[]>()
    for (const row of all) {
        const parent = row[parentKey]
        if (parent == null) continue
        const key = String(parent)
        children.set(key, [...(children.get(key) ?? []), row])
    }
    const flattened: Array<{row: Record<string, unknown>; depth: number; loadedChildren: boolean}> = []
    const walk = (row: Record<string, unknown>, depth: number) => {
        const id = cell(row, idKey)
        const next = children.get(id) ?? []
        flattened.push({row, depth, loadedChildren: next.length > 0})
        if (expanded.value.has(id)) next.forEach(child => walk(child, depth + 1))
    }
    all.filter(row => row[parentKey] == null).forEach(row => walk(row, 0))
    return flattened
})

function toggle(id: string) {
    const next = new Set(expanded.value)
    if (next.has(id)) next.delete(id)
    else next.add(id)
    expanded.value = next
}

function details(row: Record<string, unknown>) {
    const drill = props.component.props.drillDown
    if (!drill?.enabled || !drill.promptTemplate) return
    const context = {...row, ...(props.result.resultMeta?.attributes ?? {})}
    let missing = false
    const question = drill.promptTemplate.replace(/\{(\w+)\}/g, (_, key: string) => {
        if (context[key] == null || String(context[key]).trim() === '') { missing = true; return '' }
        return String(context[key])
    })
    if (missing || !question.trim()) return
    emit('drill-down', {question, surfaceId: props.component.id,
        dimension: drill.dimension, value: context[drill.dimension]})
}
</script>

<template>
  <section class="tree-view" aria-label="组织树">
    <h3 v-if="component.props.title">{{ component.props.title }}</h3>
    <ul role="tree">
      <li v-for="item in visible" :key="cell(item.row, field(component, 'id'))" role="treeitem"
          :style="{paddingLeft: `${item.depth * 1.25}rem`}">
        <button v-if="item.loadedChildren" type="button"
                :aria-expanded="expanded.has(cell(item.row, field(component, 'id')))"
                @click="toggle(cell(item.row, field(component, 'id')))">
          {{ expanded.has(cell(item.row, field(component, 'id'))) ? '▾' : '▸' }}
          {{ cell(item.row, field(component, 'label')) }}
        </button>
        <span v-else>{{ cell(item.row, field(component, 'label')) }}</span>
        <button v-if="!item.loadedChildren && item.row[field(component, 'hasChildren')] === true
                  && component.props.drillDown?.enabled"
                type="button" class="detail" @click="details(item.row)">查看子节点详情</button>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.tree-view{padding:1rem}.tree-view h3{margin:0 0 .75rem;font-size:.9rem}.tree-view ul{list-style:none;margin:0;padding:0}.tree-view li{padding-top:.35rem;padding-bottom:.35rem}.tree-view button{border:0;background:none;cursor:pointer;color:#334155;font:inherit}.tree-view .detail{margin-left:.5rem;color:#4f46e5;font-size:.75rem}
</style>
