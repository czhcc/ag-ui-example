<script setup lang="ts">
import {onBeforeUnmount, onMounted, ref, watch} from 'vue'
import {Graph} from '@antv/g6'
import type {ResultPayload, SurfaceComponent} from '../api/chat'

const props = defineProps<{
    component: SurfaceComponent
    result: ResultPayload
}>()

const container = ref<HTMLDivElement | null>(null)
const failed = ref('')
let graph: Graph | null = null

interface GraphData {
    nodes: { id: string; data: { label?: string } }[]
    edges: { id: string; source: string; target: string; data: { label?: string } }[]
}

function buildGraphData(): GraphData {
    const encoding = (props.component.props.encoding ?? {}) as Record<string, string>
    const data = props.result.data
    const rows: Record<string, unknown>[] = Array.isArray(data) ? data as Record<string, unknown>[] : []

    const nodes = new Map<string, { id: string; data: { label?: string } }>()
    const edges: GraphData['edges'] = []
    let edgeIndex = 0

    for (const row of rows) {
        const sourceId = String(row[encoding.source ?? 'source'] ?? '')
        const targetId = String(row[encoding.target ?? 'target'] ?? '')
        if (!sourceId || !targetId) continue
        if (!nodes.has(sourceId)) nodes.set(sourceId, {id: sourceId, data: {}})
        if (!nodes.has(targetId)) nodes.set(targetId, {id: targetId, data: {}})
        edges.push({
            id: `e${edgeIndex++}`,
            source: sourceId,
            target: targetId,
            data: encoding.label ? {label: String(row[encoding.label] ?? '')} : {},
        })
    }

    const labelKey = encoding.sourceLabel ?? 'sourceName'
    for (const row of rows) {
        const sourceId = String(row[encoding.source ?? 'source'] ?? '')
        const name = row[labelKey]
        if (sourceId && name && nodes.has(sourceId)) nodes.get(sourceId)!.data.label = String(name)
    }

    return {nodes: [...nodes.values()], edges}
}

async function render() {
    if (!container.value) return
    failed.value = ''
    try {
        const data = buildGraphData()
        graph?.destroy()
        graph = new Graph({
            container: container.value,
            autoFit: 'view',
            data,
            node: {
                style: {
                    size: 36,
                    label: true,
                    labelText: (d: { id: string; data?: { label?: string } }) => d.data?.label || d.id,
                    labelPlacement: 'bottom',
                    labelTextBaseline: 'top',
                    fill: '#4f46e5',
                    labelFill: '#334155',
                    labelFontSize: 11,
                },
            },
            edge: {
                style: {
                    stroke: '#cbd5e1',
                    label: true,
                    labelText: (d: { data?: { label?: string } }) => d.data?.label ?? '',
                    labelFill: '#64748b',
                    labelFontSize: 10,
                    endArrow: true,
                },
            },
            layout: {type: 'force', preventOverlap: true, nodeStrength: -60, linkDistance: 120},
            behaviors: ['drag-canvas', 'zoom-canvas', 'drag-element'],
        })
        await graph.render()
    } catch (error) {
        failed.value = error instanceof Error ? error.message : '关系图渲染失败'
    }
}

onMounted(render)
watch(() => props.result, render, {deep: true})
onBeforeUnmount(() => {
    graph?.destroy()
    graph = null
})
</script>

<template>
  <div class="w-full px-1 py-1">
    <p v-if="component.props.title" class="px-2 pb-2 text-sm font-medium text-slate-700">
      {{ component.props.title }}
    </p>
    <div v-if="failed" class="rounded-xl bg-rose-50 px-4 py-3 text-xs text-rose-600">{{ failed }}</div>
    <div v-else ref="container" class="h-80 w-full"></div>
  </div>
</template>
