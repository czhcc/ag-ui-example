<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {context} from '../core/registry'
import type {ResultPayload, UiSurface} from '../core/types'

const props = defineProps<{ surface: UiSurface }>()

const status = ref<'loading' | 'ready' | 'error'>('loading')
const result = ref<ResultPayload | null>(null)
const errorText = ref('')

const primary = computed(() => props.surface.components[0] ?? null)
const renderer = computed(() => (primary.value ? context.resolveRenderer(primary.value.type) : null))
const unsupportedType = computed(() =>
    primary.value && !context.resolveRenderer(primary.value.type) ? primary.value.type : '',
)

onMounted(async () => {
    try {
        result.value = await context.fetchResult(props.surface.dataRef)
        status.value = 'ready'
    } catch (error) {
        status.value = 'error'
        errorText.value = error instanceof Error ? error.message : 'Data load failed'
    }
})
</script>

<template>
  <div class="agent-ui-surface">
    <div v-if="status === 'loading'" class="agent-ui-surface-loading">
      <span class="agent-ui-spinner"></span>
      <span>Loading visualization…</span>
    </div>
    <div v-else-if="status === 'error'" class="agent-ui-surface-error">
      {{ errorText }}
    </div>
    <div v-else-if="primary && !renderer" class="agent-ui-surface-unsupported">
      Unsupported component type: {{ unsupportedType }}
    </div>
    <component
        v-else-if="primary && renderer && result"
        :is="renderer"
        :component="primary"
        :result="result"
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

.agent-ui-surface-loading {
    display: flex;
    height: 8rem;
    align-items: center;
    justify-content: center;
    gap: 0.5rem;
    font-size: 0.75rem;
    color: #94a3b8;
}

.agent-ui-spinner {
    width: 0.9rem;
    height: 0.9rem;
    border-radius: 9999px;
    border: 2px solid #cbd5e1;
    border-top-color: #6366f1;
    animation: agent-ui-spin 0.8s linear infinite;
}

@keyframes agent-ui-spin {
    to {
        transform: rotate(360deg);
    }
}

.agent-ui-surface-error,
.agent-ui-surface-unsupported {
    padding: 0.75rem 1rem;
    font-size: 0.75rem;
    color: #e11d48;
}

.agent-ui-surface-unsupported {
    color: #64748b;
}
</style>
