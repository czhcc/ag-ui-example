import {reactive} from 'vue'
import type {RichMessage, StreamEvent, UiSurface} from './types'

export interface UseConversationOptions {
    conversationId?: string
    send: (request: { conversationId: string; message: string },
           onEvent: (event: StreamEvent) => void,
           signal?: AbortSignal) => Promise<void>
}

export function useConversation(options: UseConversationOptions) {
    const conversationId = options.conversationId ?? `conv-${crypto.randomUUID()}`
    const messages = reactive<RichMessage[]>([])
    let sending = false

    function appendText(message: RichMessage, text: string) {
        const last = message.parts[message.parts.length - 1]
        if (last && last.kind === 'text') last.text += text
        else message.parts.push({kind: 'text', text})
    }

    async function send(content: string, signal?: AbortSignal): Promise<void> {
        if (sending || !content.trim()) return
        sending = true
        messages.push({id: crypto.randomUUID(), role: 'user', parts: [{kind: 'text', text: content}]})
        const reply: RichMessage = reactive({
            id: crypto.randomUUID(),
            role: 'assistant',
            parts: [{kind: 'text', text: ''}],
            streaming: true,
        })
        messages.push(reply)
        try {
            await options.send({conversationId, message: content}, (event) => {
                if (event.type === 'delta' && event.content) {
                    appendText(reply, event.content)
                } else if (event.type === 'ui') {
                    const {type: _t, conversationId: _c, runId: _r, ...surface} = event
                    reply.parts.push({kind: 'ui', surface: surface as UiSurface})
                } else if (event.type === 'done') {
                    reply.streaming = false
                } else if (event.type === 'error') {
                    reply.streaming = false
                    reply.failed = true
                    appendText(reply, event.content ?? 'Agent temporarily unavailable.')
                }
            }, signal)
        } finally {
            reply.streaming = false
            sending = false
        }
    }

    return {conversationId, messages, send}
}
