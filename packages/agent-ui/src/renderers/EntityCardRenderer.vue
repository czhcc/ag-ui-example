<script setup lang="ts">
import {computed} from 'vue'
import type {RendererProps} from '../core/types'
import {cell, field, rows} from './viewData'

const props = defineProps<RendererProps>()
const items = computed(() => rows(props.result).slice(0, Number(props.component.props.options?.limit ?? 50)))
</script>

<template>
  <section class="entity-list" aria-label="实体卡">
    <h3 v-if="component.props.title">{{ component.props.title }}</h3>
    <article v-for="row in items" :key="cell(row, field(component, 'id'))" class="entity-card">
      <strong>{{ cell(row, field(component, 'name')) }}</strong>
      <span>{{ cell(row, field(component, 'kind')) }}</span>
      <small>{{ cell(row, field(component, 'id')) }}</small>
      <p v-if="component.props.options?.showSummary !== false">{{ cell(row, field(component, 'summary')) }}</p>
    </article>
  </section>
</template>

<style scoped>
.entity-list{padding:1rem}.entity-list h3{margin:0 0 .75rem;font-size:.9rem}.entity-card{padding:.75rem;margin:.5rem 0;border:1px solid #e2e8f0;border-radius:.75rem}.entity-card strong{font-size:.9rem}.entity-card span{margin-left:.5rem;color:#6366f1;font-size:.7rem}.entity-card small{display:block;color:#64748b}.entity-card p{margin:.4rem 0 0;color:#475569;font-size:.8rem}
</style>
