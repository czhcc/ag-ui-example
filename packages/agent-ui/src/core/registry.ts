import type {AgentUiOptions, RendererComponent, ResultPayload} from './types'

export interface AgentUiContextValue {
    fetchResult: (dataRef: string) => Promise<ResultPayload>
    resolveRenderer: (type: string) => RendererComponent | null
}

let defaultFetch: (dataRef: string) => Promise<ResultPayload> = async (dataRef) => {
    const response = await fetch(`/api/results/${encodeURIComponent(dataRef)}`)
    if (!response.ok) throw new Error(`Result unavailable (${response.status})`)
    return (await response.json()) as ResultPayload
}

const registry = new Map<string, RendererComponent>()

export function registerRenderer(type: string, renderer: RendererComponent): void {
    registry.set(type, renderer)
}

export function unregisterRenderer(type: string): boolean {
    return registry.delete(type)
}

export function listRenderers(): string[] {
    return [...registry.keys()]
}

export function setFetchResult(fetch: (dataRef: string) => Promise<ResultPayload>): void {
    defaultFetch = fetch
}

export function configure(options: AgentUiOptions): void {
    if (options.fetchResult) setFetchResult(options.fetchResult)
    if (options.renderers) {
        for (const [type, renderer] of Object.entries(options.renderers)) {
            registerRenderer(type, renderer)
        }
    }
}

export const context: AgentUiContextValue = {
    get fetchResult() {
        return defaultFetch
    },
    resolveRenderer(type: string): RendererComponent | null {
        return registry.get(type) ?? null
    },
}
