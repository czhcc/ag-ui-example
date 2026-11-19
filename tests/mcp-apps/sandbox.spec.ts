import {test, expect} from '@playwright/test'
import {readFile} from 'node:fs/promises'
import {createHash} from 'node:crypto'

test('standard AppBridge renders server HTML, queries directly, continues Agent, restores and closes', async ({page}) => {
  const html = await readFile('mcp-server/target/generated-resources/mcp-apps/fund-flow/v1/index.html', 'utf8')
  let descriptor: Record<string, unknown> = {}
  let revision = 1, queries = 0, agentRuns = 0, resourceReads = 0, closed = false
  let snapshot = {
    analysisId: 'analysis', entityId: 'entity', demo: true,
    nodes: [{id: 'account_001', label: '账户 A'}, {id: 'account_002', label: '账户 B'}],
    edges: [{id: 'tx1', source: 'account_001', target: 'account_002', amount: '12000.00', currency: 'CNY', time: '2026-09-02'}],
    transactions: [{id: 'tx1', source: 'account_001', target: 'account_002', amount: '12000.00', currency: 'CNY', time: '2026-09-02'}],
    generatedAt: '2026-10-10', filters: {startTime: '2026-09-01T00:00:00+08:00', endTime: '2026-10-01T00:00:00+08:00'},
    page: {hasMore: false, nextCursor: ''},
  }
  const result = () => ({content: [{type: 'text', text: '演示摘要'}], structuredContent: {success: true, data: snapshot}, isError: false})
  await page.route('**/api/**', async route => {
    const request = route.request(), url = new URL(request.url())
    const fulfill = (json: unknown) => route.fulfill({json})
    if (url.pathname === '/api/agent') {
      const input = request.postDataJSON(); agentRuns++
      const messageId = input.runId + ':assistant'
      const events: unknown[] = [{type: 'RUN_STARTED', threadId: input.threadId, runId: input.runId},
        {type: 'TEXT_MESSAGE_START', messageId, role: 'assistant'}, {type: 'TEXT_MESSAGE_CONTENT', messageId, delta: '分析说明'}]
      if (!input.forwardedProps.appFollowupRef) {
        descriptor = {profile: 'ac.mcp-app', profileVersion: '1.0', appInstanceId: 'app_1', messageId,
          threadId: input.threadId, originRunId: input.runId, toolCallId: 'call', title: '资金流分析',
          resourceHash: 'sha256:' + createHash('sha256').update(html).digest('hex'), resultRef: 'result', revision}
        events.push({type: 'CUSTOM', name: 'mcp.app.create', value: descriptor})
      } else expect(input.forwardedProps.appFollowupRef).toBe('app_1:ticket')
      events.push({type: 'TEXT_MESSAGE_CONTENT', messageId, delta: '后续说明'}, {type: 'TEXT_MESSAGE_END', messageId},
        {type: 'RUN_FINISHED', threadId: input.threadId, runId: input.runId})
      await route.fulfill({contentType: 'text/event-stream', body: events.map(e => 'data: ' + JSON.stringify(e) + '\n\n').join('')}); return
    }
    if (request.method() === 'DELETE') {closed = true; await fulfill({}); return}
    if (url.pathname.endsWith('/resource')) {resourceReads++; await fulfill({html, resourceHash: descriptor.resourceHash}); return}
    if (url.pathname.endsWith('/latest-result')) {await fulfill(result()); return}
    if (url.pathname.endsWith('/tool-calls')) {
      queries++; expect(request.postDataJSON().expectedRevision).toBe(revision)
      snapshot = {...snapshot, nodes: [...snapshot.nodes, {id: 'account_003', label: '账户 C'}]}
      await fulfill({revision: ++revision, result: result()}); return
    }
    if (url.pathname.endsWith('/context')) {expect(request.postDataJSON().selectedEntityIds).toEqual(['account_001']); await fulfill({}); return}
    if (url.pathname.endsWith('/followups')) {await fulfill({appFollowupRef: 'app_1:ticket', threadId: descriptor.threadId, parentRunId: descriptor.originRunId}); return}
    await fulfill({...descriptor, revision, status: closed ? 'CLOSED' : 'ACTIVE'})
  })
  await page.goto('/')
  await page.getByRole('textbox').fill('分析资金流')
  await page.getByRole('button', {name: '发送', exact: true}).click()
  const view = page.frameLocator('iframe').frameLocator('iframe')
  await expect(view.getByText('演示数据 · 金额保留原始精度')).toBeVisible()
  await expect(view.getByRole('button', {name: '选择账户 A'})).toBeVisible()
  await view.getByRole('button', {name: '选择账户 A'}).click()
  await view.getByRole('button', {name: '展开账户', exact: true}).click()
  await expect(view.getByRole('button', {name: '选择账户 C'})).toBeVisible()
  expect(queries).toBe(1); expect(agentRuns).toBe(1)
  // A forged message from the main window must not be accepted as an App request.
  await page.evaluate(() => window.postMessage({jsonrpc:'2.0', id:99, method:'tools/call', params:{name:'evil'}}, '*'))
  expect(queries).toBe(1)
  await view.getByRole('button', {name: '继续分析', exact: true}).click()
  await expect.poll(() => agentRuns).toBe(2)
  await page.reload()
  await expect(view.getByRole('button', {name: '选择账户 C'})).toBeVisible()
  expect(agentRuns).toBe(2); expect(queries).toBe(1); expect(resourceReads).toBe(2)
  await page.getByRole('button', {name: '关闭', exact: true}).click()
  await expect(page.getByText('应用已关闭')).toBeVisible()
  expect(closed).toBe(true)
})
