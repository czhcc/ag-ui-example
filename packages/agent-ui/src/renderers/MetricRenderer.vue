<script setup lang="ts">
import {computed} from 'vue'
import type {RendererProps} from '../core/types'
import {cell, field, rows} from './viewData'

const props = defineProps<RendererProps>()
const items = computed(() => rows(props.result).slice(0, Number(props.component.props.options?.limit ?? 12)))
</script>

<template>
  <section class="phase-two-view" aria-label="指标卡">
    <h3 v-if="component.props.title">{{ component.props.title }}</h3>
    <div class="metric-grid">
      <article v-for="(row, index) in items" :key="index" class="metric-card">
        <span>{{ cell(row, field(component, 'label')) }}</span>
        <strong>{{ cell(row, field(component, 'value')) }} <small>{{ cell(row, field(component, 'unit')) }}</small></strong>
        <p v-if="component.props.options?.showChange !== false && cell(row, field(component, 'change'))">
          变化 {{ cell(row, field(component, 'change')) }}
        </p>
      </article>
    </div>
  </section>
</template>

<style scoped>
.phase-two-view{padding:1rem}.phase-two-view h3{margin:0 0 .75rem;font-size:.9rem}.metric-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(9rem,1fr));gap:.75rem}.metric-card{padding:1rem;border-radius:.75rem;background:#eef2ff}.metric-card span{display:block;color:#475569;font-size:.8rem}.metric-card strong{display:block;margin-top:.4rem;font-size:1.4rem}.metric-card small{font-size:.75rem;color:#64748b}.metric-card p{margin:.4rem 0 0;font-size:.75rem}
</style>
