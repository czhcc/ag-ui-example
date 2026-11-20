import {test, expect} from '@playwright/test'
import {writeFile} from 'node:fs/promises'

test('Q01 and Q07-Q12: real model, MCP server and sandbox', async ({page}, info) => {
  test.skip(process.env.MCP_APPS_LIVE !== '1', 'Requires running services and explicit real-model opt-in')
  const runs: {input: any; events: any[]}[] = []
  const queries: {input: any; status?: number; output?: any}[] = []
  const browserErrors: string[] = []
  const pending: Promise<void>[] = []
  const answer = () => runs.at(-1)!.events.filter(e => e.type === 'TEXT_MESSAGE_CONTENT').map(e => e.delta).join('')
  page.on('pageerror', error => browserErrors.push(error.message))
  page.on('response', response => {
    const request = response.request(), path = new URL(response.url()).pathname
    if (path === '/api/agent' && request.method() === 'POST') {
      const run = {input: request.postDataJSON(), events: [] as any[]}; runs.push(run)
      pending.push(response.text().then(body => {
        run.events = body.split('\n').filter(line => line.startsWith('data:'))
          .map(line => JSON.parse(line.slice(5)))
      }))
    }
    if (path.endsWith('/tool-calls') && request.method() === 'POST') {
      const query = {input: request.postDataJSON(), status: response.status(), output: undefined as any}; queries.push(query)
      pending.push(response.json().then(body => {query.output = body}))
    }
  })
  async function settled() {
    await expect(page.getByRole('textbox')).toBeEnabled({timeout: 180000})
    await Promise.all(pending)
    for (const run of runs) {
      expect(run.events.filter(e => e.type === 'RUN_ERROR')).toEqual([])
      expect(run.events.some(e => e.type === 'RUN_FINISHED')).toBe(true)
    }
  }
  async function send(question: string) {
    await page.getByRole('textbox').fill(question)
    console.log('Sending:', question)
    const response = page.waitForResponse(r => new URL(r.url()).pathname === '/api/agent' && r.request().method() === 'POST', {timeout: 180000})
    await page.getByRole('button', {name: '发送', exact: true}).click()
    expect((await response).ok()).toBe(true)
    await settled()
    console.log('Completed Agent runs:', runs.length)
  }
  const view = page.frameLocator('.mcp-app-part iframe').frameLocator('iframe')
  async function query(button: string) {
    const response = page.waitForResponse(r => new URL(r.url()).pathname.endsWith('/tool-calls'), {timeout: 30000})
    await view.getByRole('button', {name: button, exact: true}).click()
    expect((await response).ok()).toBe(true)
    await expect(view.getByRole('button', {name: '查询', exact: true})).toBeEnabled()
    await Promise.all(pending)
    expect(queries.at(-1)?.output.result.isError).not.toBe(true)
    expect(runs).toHaveLength(1)
    console.log('Completed direct query:', queries.at(-1)?.input.toolName)
  }
  try {
    await page.goto('/')
    await test.step('Q01: create one App with a real entry tool call', async () => {
      await send('请调用资金流分析工具 kg_fund_flow_graph，查询实体 entity_demo_001 在 2026-09-01T00:00:00+08:00 至 2026-10-01T00:00:00+08:00 的资金往来，展示交互式资金流界面，并简要说明这是演示数据。')
      await expect(page.locator('.mcp-app-part')).toHaveCount(1)
      await expect(view.getByRole('button', {name: '选择账户 A'})).toBeVisible()
      expect(runs[0]!.events.filter(e => e.type === 'CUSTOM' && e.name === 'mcp.app.create')).toHaveLength(1)
      expect(runs[0]!.events.some(e => e.type === 'TOOL_CALL_START' && e.toolCallName === 'kg_fund_flow_graph')).toBe(true)
      await page.locator('.mcp-app-part').scrollIntoViewIfNeeded()
      await page.screenshot({path: info.outputPath('Q01.png')})
    })
    await test.step('Q07: expand without a model run', async () => {
      await view.getByRole('button', {name: '选择账户 A'}).click()
      await query('展开账户')
      await expect(view.getByRole('button', {name: '选择账户 C'})).toBeVisible()
    })
    await test.step('Q08: amount filter survives expand and pagination', async () => {
      await view.getByLabel('最低金额').fill('3000')
      await query('查询')
      await expect(view.locator('tbody tr')).toHaveCount(2)
      await expect(view.locator('tbody')).toContainText('12000.00')
      await expect(view.locator('tbody')).toContainText('3800.50')
      await query('展开账户')
      await expect(view.getByLabel('最低金额')).toHaveValue('3000')
      await query('分页查询明细')
      // Account A participates only in 12000 CNY; 3800.50 CNY is B -> C.
      await expect(view.locator('tbody tr')).toHaveCount(1)
      await expect(view.locator('tbody')).toContainText('12000.00')
      expect(queries.at(-1)?.output.result.structuredContent.data.filters.minimumAmount).toBe('3000')
    })
    await test.step('Q09: pagination deduplicates and account switch resets cursor', async () => {
      await view.getByLabel('最低金额').fill('0')
      await query('查询')
      await query('展开账户')
      await query('分页查询明细')
      await expect(view.locator('tbody tr')).toHaveCount(2)
      await query('加载更多')
      await expect(view.locator('tbody tr')).toHaveCount(4)
      await view.getByRole('button', {name: '选择账户 B'}).click()
      await query('分页查询明细')
      expect(queries.at(-1)?.input.arguments.accountId).toBe('account_002')
      expect(queries.at(-1)?.input.arguments.cursor).toBeUndefined()
      await query('加载更多')
      await expect(view.locator('tbody tr')).toHaveCount(4)
      await page.locator('.mcp-app-part').scrollIntoViewIfNeeded()
      await page.screenshot({path: info.outputPath('Q09.png')})
    })
    await test.step('Q10: continue analysis starts a new run in the same thread', async () => {
      await view.getByRole('button', {name: '选择账户 A'}).click()
      const response = page.waitForResponse(r => new URL(r.url()).pathname === '/api/agent' && r.request().method() === 'POST', {timeout: 180000})
      await view.getByRole('button', {name: '继续分析', exact: true}).click()
      expect((await response).ok()).toBe(true)
      await settled()
      expect(runs).toHaveLength(2)
      expect(runs[1]!.input.threadId).toBe(runs[0]!.input.threadId)
      expect(runs[1]!.input.runId).not.toBe(runs[0]!.input.runId)
      expect(runs[1]!.input.forwardedProps.appFollowupRef).toBeTruthy()
      await expect(page.locator('.message.user').last()).toContainText('分析所选账户 A')
      expect(answer()).toMatch(/账户\s*A|account_001/)
    })
    await test.step('Q11: explain source and bounded context', async () => {
      await send('请说明刚才对所选账户的分析使用了哪些交易、时间范围和币种；如果只拿到了部分数据，请明确说明，暂时不要查询新数据。')
      expect(runs.at(-1)!.events.filter(e => e.type === 'TOOL_CALL_START')).toEqual([])
      expect(answer()).toMatch(/2026/)
      expect(answer()).toMatch(/USD|美元/)
      expect(answer()).toMatch(/CNY|人民币/)
      expect(answer()).toMatch(/片段|部分|有限|完整|20/)
    })
    await test.step('Q12: explain currencies without invented conversion', async () => {
      await send('请解释所选账户涉及的人民币和美元交易，分别列举金额，不要直接把不同币种相加，也不要在没有汇率依据时给出折算合计。')
      const text = answer().replace(/[,，]/g, '')
      for (const amount of ['12000', '2400', '950', '60']) {
        expect(text).toMatch(new RegExp(`(?<!\\d)${amount}(?:\\.0+)?(?!\\d)`))
      }
      expect(text).toMatch(/USD|美元/)
      expect(text).toMatch(/CNY|人民币/)
      await page.locator('.message.assistant').last().scrollIntoViewIfNeeded()
      await page.screenshot({path: info.outputPath('Q12.png')})
    })
    expect(runs).toHaveLength(4)
    expect(browserErrors).toEqual([])
  } finally {
    await Promise.allSettled(pending)
    const history = await page.evaluate(() => {
      const thread = sessionStorage.getItem('agent-thread-id')
      return JSON.parse(sessionStorage.getItem(`agent-history:${thread}`) ?? '[]')
    }).catch(() => [])
    const path = info.outputPath('evidence.json')
    await writeFile(path, JSON.stringify({runs, queries, browserErrors, history}, null, 2))
    await info.attach('real-model-evidence', {path, contentType: 'application/json'})
  }
})
