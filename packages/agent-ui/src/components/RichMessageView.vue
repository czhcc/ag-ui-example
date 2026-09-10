<script setup lang="ts">
import type {RichMessage} from '../core/types'
import UiSurfacePart from './UiSurfacePart.vue'

defineProps<{ message: RichMessage }>()
</script>

<template>
  <div class="agent-ui-message">
    <template v-for="(part, index) in message.parts" :key="index">
      <div v-if="part.kind === 'text'" class="agent-ui-text">{{ part.text }}</div>
      <UiSurfacePart v-else-if="part.kind === 'ui'" :surface="part.surface"/>
      <span v-if="message.streaming && index === message.parts.length - 1"
            class="agent-ui-cursor"></span>
    </template>
  </div>
</template>

<style scoped>
.agent-ui-message {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
    font-size: 0.9rem;
    line-height: 1.75;
    color: #334155;
    white-space: pre-wrap;
    word-break: break-word;
}

.agent-ui-cursor {
    display: inline-block;
    width: 2px;
    height: 1rem;
    margin-left: 2px;
    vertical-align: middle;
    background: #6366f1;
    animation: agent-ui-blink 1s step-end infinite;
}

@keyframes agent-ui-blink {
    50% {
        opacity: 0;
    }
}
</style>
