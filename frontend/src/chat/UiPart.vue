<script setup lang="ts">
import {onMounted, ref, computed, defineAsyncComponent} from 'vue'
import {fetchResult} from '../api/chat'
import type {ResultPayload, SurfaceComponent, UiEventPayload} from '../api/chat'

const props = defineProps<{ surface: UiEventPayload }>()

const status = ref<'loading' | 'ready' | 'error'>('loading')
const result = ref<ResultPayload | null>(null)
const errorText = ref('')

const registry: Record<string, ReturnType<typeof defineAsyncComponent>> = {
    Chart: defineAsyncComponent(() => import('../presentation/ChartRenderer.vue')),
    Table: defineAsyncComponent(() => import('../presentation/TableRenderer.vue')),
    Timeline: defineAsyncComponent(() => import('../presentation/TimelineRenderer.vue')),
    RelationGraph: defineAsyncComponent(() => import('../presentation/RelationGraphRenderer.vue')),
}

const primary = computed<SurfaceComponent | null>(() => props.surface.components[0] ?? null)
const renderer = computed(() => (primary.value && registry[primary.value.type]) || null)
const unsupportedType = computed(() => (primary.value && !registry[primary.value.type] ? primary.value.type : ''))

onMounted(async () => {
    try {
        result.value = await fetchResult(props.surface.dataRef)
        status.value = 'ready'
    } catch (error) {
        status.value = 'error'
        errorText.value = error instanceof Error ? error.message : '数据加载失败'
    }
})
</script>

<template>
  <div class="my-3 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
    <div v-if="status === 'loading'" class="flex h-40 items-center justify-center gap-2 text-xs text-slate-400">
      <span class="h-3.5 w-3.5 animate-spin rounded-full border-2 border-slate-300 border-t-brand-500"></span>
      正在加载图表数据…
    </div>
    <div v-else-if="status === 'error'" class="px-4 py-3 text-xs text-rose-600">
      图表数据加载失败：{{ errorText }}
    </div>
    <template v-else-if="primary">
      <div v-if="unsupportedType" class="px-4 py-3 text-xs text-slate-500">
        暂不支持的组件类型：{{ unsupportedType }}
      </div>
      <component v-else :is="renderer" :component="primary" :result="result!" class="px-3 py-2"/>
    </template>
  </div>
</template>
