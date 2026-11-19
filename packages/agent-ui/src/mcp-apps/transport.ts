import type {PostMessageTransport} from '@modelcontextprotocol/ext-apps/app-bridge'

/** Exact proxy window + origin binding; no wildcard destination or trust in an opaque inner origin. */
export class OriginBoundTransport {
  onmessage?: PostMessageTransport['onmessage']
  onerror?: PostMessageTransport['onerror']
  onclose?: PostMessageTransport['onclose']
  private closed = false
  constructor(private target: Window, private origin: string) {}
  private listener = (event: MessageEvent) => {
    if (this.closed || event.source !== this.target || event.origin !== this.origin) return
    try {
      if (JSON.stringify(event.data).length > 65536 || event.data?.jsonrpc !== '2.0') throw new Error('APP_MESSAGE_INVALID')
      this.onmessage?.(event.data)
    } catch (e) {this.onerror?.(e instanceof Error ? e : new Error('APP_MESSAGE_INVALID'))}
  }
  async start() {window.addEventListener('message', this.listener)}
  async send(message: Parameters<PostMessageTransport['send']>[0]) {
    if (this.closed) throw new Error('APP_CLOSED')
    this.target.postMessage(message, this.origin)
  }
  async close() {this.closed = true; window.removeEventListener('message', this.listener); this.onclose?.()}
}
