export interface ChatRequest {
    conversationId: string
    message: string
}

export interface SurfaceComponent {
    id: string
    type: 'Chart' | 'RelationGraph' | 'Timeline' | 'Table' | string
    props: {
        subType?: string
        title?: string
        description?: string
        encoding?: Record<string, unknown>
        options?: Record<string, unknown>
    }
}

export interface UiEventPayload {
    conversationId: string
    runId: string
    surfaceId: string
    dataRef: string
    components: SurfaceComponent[]
}

export type ChatStreamEvent =
    | { type: 'delta'; conversationId: string; runId: string; content: string }
    | { type: 'done'; conversationId: string; runId: string; content: null }
    | { type: 'error'; conversationId: string; runId: string; content: string }

export interface ToolEventPayload {
    type: 'tool_start' | 'tool_end'
    conversationId: string
    runId: string
    toolCallId: string
    toolName: string
    arguments: string | null
    success: boolean | null
    resultRef: string | null
    detail: string | null
}

export type StreamEvent = ChatStreamEvent | (UiEventPayload & { type: 'ui' }) | ToolEventPayload

export interface ResultPayload {
    resultRef: string
    serverCode: string
    toolName: string
    data: unknown
    summary: { count?: number; description?: string } | null
    expiresAtEpochMs: number | null
}

export async function fetchResult(resultRef: string): Promise<ResultPayload> {
    const response = await fetch(`/api/results/${encodeURIComponent(resultRef)}`)
    if (!response.ok) throw new Error(`结果数据不可用（${response.status}）`)
    return (await response.json()) as ResultPayload
}

export async function streamChat(
    request: ChatRequest,
    onEvent: (event: StreamEvent) => void,
    signal?: AbortSignal,
): Promise<void> {
    const response = await fetch('/api/chat/messages', {
        method: 'POST',
        headers: {
            Accept: 'text/event-stream',
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
        signal,
    })

    if (!response.ok) {
        const detail = await response.text()
        throw new Error(detail || `请求失败（${response.status}）`)
    }

    if (!response.body) {
        throw new Error('浏览器未收到流式响应')
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    const consume = (chunk: string) => {
        const frames = chunk.replace(/\r\n/g, '\n').split('\n\n')
        buffer = frames.pop() ?? ''

        for (const frame of frames) {
            const lines = frame.split('\n')
            const eventName = lines
                .find((line) => line.startsWith('event:'))
                ?.slice(6)
                .trim()
            const data = lines
                .filter((line) => line.startsWith('data:'))
                .map((line) => line.slice(5).trimStart())
                .join('\n')

            if (!data) continue

            const payload = JSON.parse(data)
            if (eventName === 'ui') {
                onEvent({...payload, type: 'ui'})
            } else if (eventName === 'tool') {
                onEvent(payload as ToolEventPayload)
            } else {
                onEvent(payload)
            }
        }
    }

    while (true) {
        const {done, value} = await reader.read()
        if (done) break
        buffer += decoder.decode(value, {stream: true})
        consume(buffer)
    }

    buffer += decoder.decode()
    if (buffer.trim()) {
        consume(`${buffer}\n\n`)
    }
}
