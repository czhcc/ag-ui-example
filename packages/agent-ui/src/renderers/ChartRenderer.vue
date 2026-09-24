<script setup lang="ts">
import {BarChart, LineChart, PieChart, ScatterChart} from 'echarts/charts'
import {GridComponent, LegendComponent, TitleComponent, TooltipComponent} from 'echarts/components'
import * as echarts from 'echarts/core'
import {CanvasRenderer} from 'echarts/renderers'
import {computed, onBeforeUnmount, onMounted, ref, watch} from 'vue'
import type {DrillDownEvent, DrillDownHint, ResultPayload, SurfaceComponent} from '../core/types'

echarts.use([BarChart, LineChart, PieChart, ScatterChart, GridComponent, TooltipComponent, TitleComponent, LegendComponent, CanvasRenderer])

type EChartsType = ReturnType<typeof echarts.init>

const props = defineProps<{
    component: SurfaceComponent
    result: ResultPayload
}>()

const emit = defineEmits<{ (e: 'drill-down', event: DrillDownEvent): void }>()

const drillDown = computed<DrillDownHint | null>(() => {
    const drill = props.component.props.drillDown
    return drill && drill.enabled && drill.promptTemplate ? drill : null
})

function buildDrillQuestion(categoryValue: unknown): DrillDownEvent | null {
    if (!drillDown.value) return null
    const encoding = (props.component.props.encoding ?? {}) as Record<string, string>
    const rows = buildRows()
    const row = rows.find((r) => String(r[encoding.category ?? 'category'] ?? '') === String(categoryValue))
    if (!row) return null
    const context: Record<string, unknown> = {
        ...row,
        ...(props.result.resultMeta?.attributes ?? {}),
    }
    const question = drillDown.value.promptTemplate.replace(/\{(\w+)\}/g,
        (_, key: string) => String(context[key] ?? ''))
    return {
        question,
        surfaceId: props.component.id,
        dimension: drillDown.value.dimension,
        value: context[drillDown.value.dimension],
    }
}

function handleChartClick(params: { name?: string; componentType?: string }) {
    if (params.componentType !== 'series' || params.name == null) return
    const event = buildDrillQuestion(params.name)
    if (event) emit('drill-down', event)
}

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
        tooltip: {
            trigger: 'axis',
            ...(drillDown.value ? {appendToBody: true} : {}),
        },
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
        cancelIdle(pendingRender)
        pendingRender = 0
    }
}

function scheduleIdle(callback: () => void): number {
    const ric = (window as { requestIdleCallback?: (cb: () => void) => number }).requestIdleCallback
    if (typeof ric === 'function') {
        return ric(callback) as unknown as number
    }
    return requestAnimationFrame(() => requestAnimationFrame(callback))
}

function cancelIdle(handle: number) {
    const cic = (window as { cancelIdleCallback?: (h: number) => void }).cancelIdleCallback
    if (typeof cic === 'function') {
        cic(handle)
        return
    }
    cancelAnimationFrame(handle)
}

function render() {
    if (!container.value) return
    disposePending()
    pendingRender = scheduleIdle(() => {
        pendingRender = 0
        if (!container.value) return
        if (!chart) chart = echarts.init(container.value)
        pendingRender = scheduleIdle(() => {
            pendingRender = 0
            if (!chart || !container.value) return
            chart.setOption(toOption())
            chart.resize()
            if (drillDown.value) {
                chart.off('click')
                chart.on('click', handleChartClick)
            }
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
    <div v-else ref="container" class="agent-ui-chart h-72 w-full" :class="drillDown ? 'cursor-pointer' : ''"
         :title="drillDown ? (drillDown.label ?? '点击深入分析') : undefined"></div>
  </div>
</template>

<style scoped>
.agent-ui-chart {
    width: 100%;
    height: 18rem;
    min-height: 18rem;
}
</style>
