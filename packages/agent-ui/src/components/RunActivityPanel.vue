<script setup lang="ts">
import {computed, ref} from 'vue'
import type {AgentUiState} from '../core/types'

const props = defineProps<{state: AgentUiState; runId: string}>()
const open = ref(false)
const run = computed(() => props.state.runs[props.runId])
const reasoning = computed(() => run.value?.reasoningIds.map((id) => props.state.reasoning[id]).filter(Boolean) ?? [])
const subagents = computed(() => Object.values(props.state.subagents).filter((item) => item.runId === props.runId))
const hasActivity = computed(() => Boolean(
    run.value && (run.value.state != null || reasoning.value.length || run.value.interrupts.length || subagents.value.length),
))

function safeJson(value: unknown): string {
    try { return JSON.stringify(value, null, 2).slice(0, 8_000) } catch { return 'Unavailable' }
}
</script>

<template>
  <button v-if="hasActivity" class="agent-activity-button" type="button" @click="open = !open">
    {{ open ? '收起状态' : '状态与推理' }}
  </button>
  <section v-if="open && run" class="agent-activity" aria-label="Agent run activity">
    <div v-if="run.interrupts.length" class="activity-block activity-hitl">
      <h4>等待人工处理</h4>
      <p v-for="interrupt in run.interrupts" :key="interrupt.id">
        {{ interrupt.message || interrupt.reason }}
      </p>
    </div>
    <div v-if="reasoning.length" class="activity-block">
      <h4>推理过程</h4>
      <p v-for="item in reasoning" :key="item.id">{{ item.content || (item.streaming ? '正在推理…' : '已完成') }}</p>
    </div>
    <div v-if="subagents.length" class="activity-block">
      <h4>子 Agent</h4>
      <p v-for="item in subagents" :key="item.id">{{ item.name }} · {{ item.value.status || item.value.taskId || item.id }}</p>
    </div>
    <details v-if="run.state != null" class="activity-block">
      <summary>运行状态</summary>
      <pre>{{ safeJson(run.state) }}</pre>
    </details>
  </section>
</template>

<style scoped>
.agent-activity-button { border: 0; background: transparent; color: #6366f1; font-size: .7rem; cursor: pointer; }
.agent-activity { width: 100%; margin-top: .4rem; padding: .75rem; border: 1px solid #e2e8f0; border-radius: .75rem;
    background: #f8fafc; color: #475569; font-size: .72rem; }
.activity-block + .activity-block { margin-top: .65rem; }
.activity-block h4 { margin: 0 0 .25rem; color: #334155; font-size: .72rem; }
.activity-block p { margin: .15rem 0; white-space: pre-wrap; }
.activity-hitl { padding: .55rem; border-radius: .5rem; background: #fffbeb; }
.activity-block summary { cursor: pointer; font-weight: 600; }
.activity-block pre { max-height: 12rem; overflow: auto; white-space: pre-wrap; word-break: break-word; }
</style>
