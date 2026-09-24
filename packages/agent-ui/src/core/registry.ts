import {fetchResult as requestResult} from './stream'
import type {
    AgentUiOptions,
    RendererComponent,
    ResultFetchContext,
    ResultPayload,
} from './types'

export interface AgentUiContextValue {
    fetchResult: (dataRef: string, context?: ResultFetchContext) => Promise<ResultPayload>
    resolveRenderer: (type: string) => RendererComponent | null
}

let resultUrl = '/api/results'
let resultHeaders: AgentUiOptions['resultHeaders']
let credentials: RequestCredentials = 'same-origin'
let defaultFetch: AgentUiContextValue['fetchResult'] = (dataRef, context) => requestResult(
    dataRef,
    context,
    {resultUrl, headers: resultHeaders, credentials},
)

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

export function setFetchResult(fetch: AgentUiContextValue['fetchResult']): void {
    defaultFetch = fetch
}

export function configure(options: AgentUiOptions): void {
    if (options.resultUrl) resultUrl = options.resultUrl
    if (options.resultHeaders) resultHeaders = options.resultHeaders
    if (options.credentials) credentials = options.credentials
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
