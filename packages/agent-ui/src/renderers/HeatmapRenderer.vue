<script setup lang="ts">
import {computed} from 'vue'
import type {RendererProps} from '../core/types'
import {cell, field, rows} from './viewData'

const props = defineProps<RendererProps>()
const cells = computed(() => rows(props.result).slice(0, Number(props.component.props.options?.limit ?? 1000)))
const xs = computed(() => [...new Set(cells.value.map(row => cell(row, field(props.component, 'x'))))])
const ys = computed(() => [...new Set(cells.value.map(row => cell(row, field(props.component, 'y'))))])
const max = computed(() => Math.max(1, ...cells.value.map(row => Number(row[field(props.component, 'value')]) || 0)))
function value(x: string, y: string): number | null {
    const row = cells.value.find(item => cell(item, field(props.component, 'x')) === x
        && cell(item, field(props.component, 'y')) === y)
    return row ? Number(row[field(props.component, 'value')]) : null
}
function color(value: number | null): string {
    if (value == null) return '#f8fafc'
    return `rgba(79, 70, 229, ${0.18 + 0.75 * Math.max(0, value) / max.value})`
}
</script>

<template>
  <section class="heatmap-view" aria-label="活动热力图">
    <h3 v-if="component.props.title">{{ component.props.title }}</h3>
    <div class="heatmap-scroll">
      <table>
        <thead><tr><th scope="col">类型 / 城市</th><th v-for="x in xs" :key="x" scope="col">{{ x }}</th></tr></thead>
        <tbody><tr v-for="y in ys" :key="y"><th scope="row">{{ y }}</th>
          <td v-for="x in xs" :key="x" :style="{backgroundColor: color(value(x,y))}"
              :title="`${x} / ${y}: ${value(x,y) ?? '无数据'}`">
            {{ value(x,y) == null ? '无数据' : component.props.options?.showLabels === false ? '' : value(x,y) }}
          </td>
        </tr></tbody>
      </table>
    </div>
  </section>
</template>

<style scoped>
.heatmap-view{padding:1rem}.heatmap-view h3{margin:0 0 .75rem;font-size:.9rem}.heatmap-scroll{overflow:auto}.heatmap-view table{border-collapse:separate;border-spacing:.3rem;width:100%;font-size:.75rem}.heatmap-view th,.heatmap-view td{padding:.7rem;text-align:center;white-space:nowrap}.heatmap-view th{color:#475569}.heatmap-view td{border-radius:.4rem;color:#1e1b4b}
</style>
