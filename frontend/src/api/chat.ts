export interface ChatRequest {
    conversationId: string
    message: string
}

export interface ChatStreamEvent {
    type: 'delta' | 'done' | 'error'
    conversationId: string
    runId: string
    content: string | null
}

export async function streamChat(
    request: ChatRequest,
    onEvent: (event: ChatStreamEvent) => void,
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
            const data = frame
                .split('\n')
                .filter((line) => line.startsWith('data:'))
                .map((line) => line.slice(5).trimStart())
                .join('\n')

            if (data) {
                onEvent(JSON.parse(data) as ChatStreamEvent)
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
