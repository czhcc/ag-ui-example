import {createServer} from 'node:http'
import {readFile} from 'node:fs/promises'
const port = Number(process.env.MCP_APP_SANDBOX_PORT ?? 5174)
const origins = (process.env.MCP_APP_PARENT_ORIGINS ?? 'http://localhost:5173,http://127.0.0.1:5173').split(',')
for (const origin of origins) if (new URL(origin).origin !== origin) throw new Error('Use exact parent origins')
const page = await readFile(new URL('proxy.html', import.meta.url), 'utf8')
createServer((request, response) => {
  const url = new URL(request.url, 'http://localhost')
  if (request.method !== 'GET' || url.pathname !== '/proxy.html' || !origins.includes(url.searchParams.get('parentOrigin'))) {
    response.writeHead(403).end(); return
  }
  response.writeHead(200, {'Content-Type':'text/html; charset=utf-8', 'Cache-Control':'no-store',
    'Referrer-Policy':'no-referrer', 'X-Content-Type-Options':'nosniff',
    'Content-Security-Policy':`default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; frame-src 'self' blob:; frame-ancestors ${origins.join(' ')}; base-uri 'none'; form-action 'none'`,
    'Permissions-Policy':'camera=(), microphone=(), geolocation=()'})
  response.end(page)
}).listen(port, '127.0.0.1', () => console.log(`MCP App sandbox listening on ${port}`))
