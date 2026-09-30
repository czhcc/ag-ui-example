<script setup lang="ts">
import {computed} from 'vue'
import type {RendererProps} from '../core/types'
import {cell, field, rows} from './viewData'

const props = defineProps<RendererProps>()
const items = computed(() => [...rows(props.result)]
    .sort((a, b) => Number(a[field(props.component, 'step')]) - Number(b[field(props.component, 'step')]))
    .slice(0, Number(props.component.props.options?.limit ?? 50)))
</script>

<template>
  <section class="chain-view" aria-label="证据链">
    <h3 v-if="component.props.title">{{ component.props.title }}</h3>
    <ol>
      <li v-for="row in items" :key="cell(row, field(component, 'id'))">
        <strong>{{ cell(row, field(component, 'title')) }}</strong>
        <span v-if="component.props.options?.showSource !== false">来源：{{ cell(row, field(component, 'source')) }}</span>
        <p>{{ cell(row, field(component, 'summary')) }}</p>
        <small>{{ cell(row, field(component, 'evidenceRef')) }}</small>
      </li>
    </ol>
  </section>
</template>

<style scoped>
.chain-view{padding:1rem}.chain-view h3{margin:0 0 .75rem;font-size:.9rem}.chain-view ol{margin:0;padding-left:1.5rem}.chain-view li{padding:.45rem .5rem;border-left:2px solid #818cf8}.chain-view strong{display:block}.chain-view span,.chain-view small{display:block;color:#64748b;font-size:.75rem}.chain-view p{margin:.3rem 0;font-size:.8rem}
</style>
