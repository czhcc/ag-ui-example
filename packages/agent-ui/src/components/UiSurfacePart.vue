<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {context} from '../core/registry'
import type {ResultPayload, UiSurface} from '../core/types'

const props = defineProps<{ surface: UiSurface }>()

const emit = defineEmits<{ (e: 'drill-down', event: import('../core/types').DrillDownEvent): void }>()

const status = ref<'loading' | 'ready' | 'error'>('loading')
const result = ref<ResultPayload | null>(null)
const errorText = ref('')

const primary = computed(() => props.surface.components[0] ?? null)
const renderer = computed(() => (primary.value ? context.resolveRenderer(primary.value.type) : null))

const typeLabel = computed(() => {
    switch (primary.value?.type) {
        case 'Chart': return '统计图'
        case 'Timeline': return '时间线'
        case 'RelationGraph': return '关系图'
        case 'Table': return '数据表'
        default: return '可视化组件'
    }
})

onMounted(() => {
    void (async () => {
        try {
            result.value = await context.fetchResult(props.surface.dataRef)
            await new Promise<void>((resolve) =>
                requestAnimationFrame(() => requestAnimationFrame(() => resolve())))
            status.value = 'ready'
        } catch (error) {
            status.value = 'error'
            errorText.value = error instanceof Error ? error.message : '数据加载失败'
        }
    })()
})
</script>

<template>
  <div class="agent-ui-surface">
    <div v-if="status === 'loading'" class="agent-ui-loading" role="status" aria-live="polite">
      <div class="agent-ui-loading-bar">
        <span class="agent-ui-loading-icon">
          <svg viewBox="0 0 24 24" fill="none">
            <path d="M4 20V10m5.3 10V4m5.4 16v-7M20 20v-3" stroke="currentColor" stroke-width="2"
                  stroke-linecap="round"/>
          </svg>
        </span>
        <span class="agent-ui-loading-text">正在加载{{ typeLabel }}…</span>
        <span class="agent-ui-loading-hint">数据准备中，文字不受影响</span>
      </div>
      <div class="agent-ui-skeleton">
        <div class="agent-ui-skeleton-item" style="height: 55%; animation-delay: 0s"></div>
        <div class="agent-ui-skeleton-item" style="height: 75%; animation-delay: .12s"></div>
        <div class="agent-ui-skeleton-item" style="height: 40%; animation-delay: .24s"></div>
        <div class="agent-ui-skeleton-item" style="height: 88%; animation-delay: .36s"></div>
        <div class="agent-ui-skeleton-item" style="height: 62%; animation-delay: .48s"></div>
      </div>
    </div>

    <div v-else-if="status === 'error'" class="agent-ui-surface-error">
      {{ typeLabel }}加载失败：{{ errorText }}
    </div>

    <div v-else-if="primary && !renderer" class="agent-ui-surface-unsupported">
      暂不支持的组件类型：{{ primary.type }}
    </div>

    <component
        v-else-if="primary && renderer && result"
        :is="renderer"
        :component="primary"
        :result="result"
        @drill-down="(event: import('../core/types').DrillDownEvent) => emit('drill-down', event)"
    />
  </div>
</template>

<style scoped>
.agent-ui-surface {
    width: 100%;
    margin: 0.5rem 0;
    border-radius: 1rem;
    border: 1px solid #e2e8f0;
    background: #fff;
    box-shadow: 0 1px 2px rgb(0 0 0 / 0.05);
    overflow: hidden;
}

.agent-ui-loading {
    padding: 1rem 1.25rem 1.25rem;
}

.agent-ui-loading-bar {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding-bottom: 0.75rem;
}

.agent-ui-loading-icon {
    display: inline-flex;
    width: 1.15rem;
    height: 1.15rem;
    color: #6366f1;
    animation: agent-ui-pulse 1.2s ease-in-out infinite;
}

.agent-ui-loading-icon svg {
    width: 100%;
    height: 100%;
}

.agent-ui-loading-text {
    font-size: 0.8rem;
    font-weight: 500;
    color: #475569;
}

.agent-ui-loading-hint {
    margin-left: auto;
    font-size: 0.68rem;
    color: #94a3b8;
}

.agent-ui-skeleton {
    display: flex;
    align-items: flex-end;
    gap: 4%;
    height: 15rem;
    padding: 0 0.25rem;
}

.agent-ui-skeleton-item {
    flex: 1;
    border-radius: 0.5rem 0.5rem 0 0;
    background: linear-gradient(90deg, #eef2f7 25%, #e2e8f0 50%, #eef2f7 75%);
    background-size: 200% 100%;
    animation: agent-ui-shimmer 1.4s ease-in-out infinite;
}

@keyframes agent-ui-shimmer {
    0% { background-position: 200% 0; }
    100% { background-position: -200% 0; }
}

@keyframes agent-ui-pulse {
    0%, 100% { opacity: 1; transform: scaleY(1); }
    50% { opacity: 0.45; transform: scaleY(0.82); }
}

.agent-ui-surface-error {
    padding: 0.75rem 1rem;
    font-size: 0.75rem;
    color: #e11d48;
}

.agent-ui-surface-unsupported {
    padding: 0.75rem 1rem;
    font-size: 0.75rem;
    color: #64748b;
}
</style>
