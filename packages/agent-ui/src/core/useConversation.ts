import {computed, reactive, ref} from 'vue'
import {createAgentUiState, reduceAgentUiEvent} from './reducer'
import {streamAgent} from './stream'
import type {AgentStreamOptions} from './stream'
import type {AgentUiState, AgUiEvent, AgUiResume, RunAgentInput} from './types'

export interface UseConversationOptions {
    threadId?: string
    stream?: (
        request: RunAgentInput,
        onEvent: (event: AgUiEvent) => void,
        signal?: AbortSignal,
    ) => Promise<void>
    streamOptions?: AgentStreamOptions
    initialState?: unknown
    forwardedProps?: Record<string, unknown>
}

export interface SendOptions {
    parentRunId?: string
    resume?: AgUiResume[]
    signal?: AbortSignal
}

export function useConversation(options: UseConversationOptions = {}) {
    const threadId = ref(options.threadId ?? `thread-${createId()}`)
    const state = reactive(createAgentUiState()) as AgentUiState
    const sending = ref(false)
    const activeRunId = ref<string | null>(null)
    const messages = computed(() => Object.values(state.messages))
    const sendStream = options.stream ?? ((request, onEvent, signal) =>
        streamAgent(request, onEvent, signal, options.streamOptions))

    async function send(content: string, sendOptions: SendOptions = {}): Promise<string | null> {
        if (sending.value || (!content.trim() && !sendOptions.resume?.length)) return null
        const runId = `run-${createId()}`
        const request: RunAgentInput = {
            threadId: threadId.value,
            runId,
            parentRunId: sendOptions.parentRunId,
            state: options.initialState,
            messages: content.trim() ? [{id: `message-${createId()}`, role: 'user', content}] : [],
            tools: [],
            context: [],
            forwardedProps: options.forwardedProps ?? {},
            resume: sendOptions.resume ?? [],
        }
        sending.value = true
        activeRunId.value = runId
        try {
            await sendStream(request, (event) => reduceAgentUiEvent(state, event, runId), sendOptions.signal)
            return runId
        } finally {
            sending.value = false
            activeRunId.value = null
        }
    }

    return {threadId, state, messages, sending, activeRunId, send}
}

function createId(): string {
    return globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(16).slice(2)}`
}
