<script setup lang="ts">
import {computed} from 'vue'
import type {RendererProps} from '../core/types'
import {cell, field, rows} from './viewData'

const props = defineProps<RendererProps>()
const items = computed(() => [...rows(props.result)]
    .sort((a, b) => Number(a[field(props.component, 'step')]) - Number(b[field(props.component, 'step')]))
    .slice(0, Number(props.component.props.options?.limit ?? 30)))
</script>

<template>
  <section class="path-view" aria-label="关联路径">
    <h3 v-if="component.props.title">{{ component.props.title }}</h3>
    <ol>
      <li v-for="row in items" :key="cell(row, field(component, 'step'))">
        <span class="node">{{ cell(row, field(component, 'source')) }}</span>
        <span class="arrow">→ <small v-if="component.props.options?.showLabels !== false">{{ cell(row, field(component, 'label')) }}</small> →</span>
        <span class="node">{{ cell(row, field(component, 'target')) }}</span>
      </li>
    </ol>
  </section>
</template>

<style scoped>
.path-view{padding:1rem}.path-view h3{margin:0 0 .75rem;font-size:.9rem}.path-view ol{list-style:none;padding:0;margin:0}.path-view li{display:flex;align-items:center;flex-wrap:wrap;gap:.5rem;margin:.5rem 0}.node{padding:.4rem .65rem;border-radius:.5rem;background:#eef2ff}.arrow{color:#6366f1}.arrow small{font-size:.7rem;color:#475569}
</style>
