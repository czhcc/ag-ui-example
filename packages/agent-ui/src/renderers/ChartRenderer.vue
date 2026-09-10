<script setup lang="ts">
import {BarChart, LineChart, PieChart, ScatterChart} from 'echarts/charts'
import {GridComponent, LegendComponent, TitleComponent, TooltipComponent} from 'echarts/components'
import * as echarts from 'echarts/core'
import {CanvasRenderer} from 'echarts/renderers'
import {onBeforeUnmount, onMounted, ref, watch} from 'vue'
import type {ResultPayload, SurfaceComponent} from '../core/types'

echarts.use([BarChart, LineChart, PieChart, ScatterChart, GridComponent, TooltipComponent, TitleComponent, LegendComponent, CanvasRenderer])

type EChartsType = ReturnType<typeof echarts.init>

const props = defineProps<{
    component: SurfaceComponent
    result: ResultPayload
}>()

const container = ref<HTMLDivElement | null>(null)
let chart: EChartsType | null = null
let observer: ResizeObserver | null = null
let pendingRender = 0
const failed = ref('')

const ALLOWED_CHART_TYPES = new Set(['bar', 'line', 'area', 'pie', 'scatter'])

function buildRows(): Record<string, unknown>[] {
    const data = props.result.data
    if (Array.isArray(data)) return data as Record<string, unknown>[]
    if (data && typeof data === 'object' && Array.isArray((data as { rows?: unknown[] }).rows)) {
        return (data as { rows: Record<string, unknown>[] }).rows
    }
    return []
}

type Option = Record<string, unknown>

function toOption(): Option {
    const encoding = (props.component.props.encoding ?? {}) as Record<string, string>
    const rows = buildRows()
    const subType = ALLOWED_CHART_TYPES.has(props.component.props.subType ?? '')
        ? (props.component.props.subType as string)
        : 'bar'

    if (subType === 'pie') {
        return {
            title: props.component.props.title ? {text: props.component.props.title, left: 'center'} : undefined,
            tooltip: {trigger: 'item'},
            series: [{
                type: 'pie',
                radius: '60%',
                data: rows.map((row) => ({
                    name: String(row[encoding.category] ?? ''),
                    value: Number(row[encoding.value] ?? 0),
                })),
            }],
        }
    }

    return {
        title: props.component.props.title ? {text: props.component.props.title, left: 'center'} : undefined,
        tooltip: {trigger: 'axis'},
        grid: {left: 56, right: 32, top: 56, bottom: 40, containLabel: false},
        xAxis: {
            type: 'category',
            data: rows.map((row) => String(row[encoding.category] ?? '')),
            boundaryGap: true,
            axisLabel: {hideOverlap: false, interval: 0},
        },
        yAxis: {type: 'value'},
        series: [{
            type: subType === 'line' ? 'line' : subType === 'scatter' ? 'scatter' : 'bar',
            areaStyle: subType === 'area' ? {} : undefined,
            barMaxWidth: 64,
            barCategoryGap: '40%',
            itemStyle: {borderRadius: [6, 6, 0, 0], color: '#6366f1'},
            data: rows.map((row) => Number(row[encoding.value] ?? 0)),
        }],
    }
}

function disposePending() {
    if (pendingRender) {
        cancelAnimationFrame(pendingRender)
        pendingRender = 0
    }
}

function render() {
    if (!container.value) return
    disposePending()
    pendingRender = requestAnimationFrame(() => {
        pendingRender = requestAnimationFrame(() => {
            pendingRender = 0
            if (!container.value) return
            if (!chart) chart = echarts.init(container.value)
            chart.setOption(toOption())
            chart.resize()
        })
    })
}

function handleResize() {
    chart?.resize()
}

onMounted(() => {
    render()
    if (container.value && typeof ResizeObserver !== 'undefined') {
        observer = new ResizeObserver(() => chart?.resize())
        observer.observe(container.value)
    }
    window.addEventListener('resize', handleResize)
})
watch(() => props.result, render, {deep: true})

onBeforeUnmount(() => {
    disposePending()
    observer?.disconnect()
    observer = null
    window.removeEventListener('resize', handleResize)
    chart?.dispose()
    chart = null
})
</script>

<template>
  <div class="w-full">
    <div v-if="failed" class="rounded-xl bg-rose-50 px-4 py-3 text-xs text-rose-600">{{ failed }}</div>
    <div v-else ref="container" class="h-72 w-full"></div>
  </div>
</template>
