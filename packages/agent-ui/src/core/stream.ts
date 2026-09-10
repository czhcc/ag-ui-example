import type {StreamEvent} from './types'

export interface StreamChatOptions {
    url?: string
    fetchImpl?: typeof fetch
    headers?: Record<string, string>
}

const DEFAULT_URL = '/api/chat/messages'

export async function streamChat(
    request: { conversationId: string; message: string },
    onEvent: (event: StreamEvent) => void,
    signal?: AbortSignal,
    options: StreamChatOptions = {},
): Promise<void> {
    const doFetch = options.fetchImpl ?? fetch
    const response = await doFetch(options.url ?? DEFAULT_URL, {
        method: 'POST',
        headers: {
            Accept: 'text/event-stream',
            'Content-Type': 'application/json',
            ...(options.headers ?? {}),
        },
        body: JSON.stringify(request),
        signal,
    })

    if (!response.ok) {
        const detail = await response.text()
        throw new Error(detail || `Request failed (${response.status})`)
    }
    if (!response.body) throw new Error('No streaming body')

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    const consume = (chunk: string) => {
        const frames = chunk.replace(/\r\n/g, '\n').split('\n\n')
        buffer = frames.pop() ?? ''
        for (const frame of frames) {
            const lines = frame.split('\n')
            const eventName = lines.find((l) => l.startsWith('event:'))?.slice(6).trim()
            const data = lines
                .filter((l) => l.startsWith('data:'))
                .map((l) => l.slice(5).trimStart())
                .join('\n')
            if (!data) continue
            const payload = JSON.parse(data)
            if (eventName === 'ui') onEvent({...payload, type: 'ui'})
            else if (eventName === 'tool') onEvent(payload as StreamEvent)
            else onEvent(payload as StreamEvent)
        }
    }

    while (true) {
        const {done, value} = await reader.read()
        if (done) break
        buffer += decoder.decode(value, {stream: true})
        consume(buffer)
    }
    buffer += decoder.decode()
    if (buffer.trim()) consume(`${buffer}\n\n`)
}

export async function fetchResult(
    dataRef: string,
    options: StreamChatOptions = {},
): Promise<import('./types').ResultPayload> {
    const doFetch = options.fetchImpl ?? fetch
    const base = options.url?.replace(/\/[^/]*$/, '') ?? '/api'
    const response = await doFetch(`${base}/results/${encodeURIComponent(dataRef)}`)
    if (!response.ok) throw new Error(`Result unavailable (${response.status})`)
    return (await response.json()) as import('./types').ResultPayload
}
