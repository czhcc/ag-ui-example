<script setup lang="ts">
import {computed, onMounted, ref, watch} from 'vue'
import {App} from '@modelcontextprotocol/ext-apps'
interface Account {id: string; label: string}
interface Transaction {id: string; source: string; target: string; amount: string; currency: string; time: string}
interface Snapshot {analysisId: string; entityId: string; nodes: Account[]; edges: Transaction[]; transactions: Transaction[];
  generatedAt: string; filters: {startTime: string; endTime: string; minimumAmount?: string}; page: {hasMore: boolean; nextCursor: string}}
const host = new App({name: 'ac-fund-flow', version: '1.0.0'}, {availableDisplayModes: ['inline']})
const data = ref<Snapshot | null>(null)
const selected = ref('')
const transactions = ref<Transaction[]>([])
const start = ref('2026-09-01T00:00:00+08:00')
const end = ref('2026-10-01T00:00:00+08:00')
const minimum = ref('0')
const busy = ref(false)
const error = ref('')
const cursor = ref('')
const pageLoaded = ref(false)
const account = computed(() => data.value?.nodes.find(n => n.id === selected.value))
function accept(result: {structuredContent?: unknown; isError?: boolean}) {
  const envelope = result.structuredContent as {success?: boolean; data?: Snapshot} | undefined
  if (result.isError || !envelope?.success || !envelope.data) throw new Error('查询失败，请调整条件后重试')
  if (envelope.data.nodes) {
    data.value = envelope.data; transactions.value = envelope.data.transactions; cursor.value = ''; pageLoaded.value = false
    start.value = envelope.data.filters.startTime; end.value = envelope.data.filters.endTime
    minimum.value = envelope.data.filters.minimumAmount ?? '0'
  } else {
    const rows = pageLoaded.value ? transactions.value : []
    transactions.value = [...new Map([...rows, ...envelope.data.transactions].map(t => [t.id, t])).values()]
    pageLoaded.value = true; cursor.value = envelope.data.page.nextCursor
  }
}
host.ontoolresult = result => {try {accept(result)} catch (e) {error.value = String(e)}}
async function query(name: string, args: Record<string, unknown>) {
  if (busy.value || !data.value) return
  busy.value = true; error.value = ''
  try {accept(await host.callServerTool({name, arguments: {analysisId: data.value.analysisId, ...args}}))}
  catch (e) {error.value = e instanceof Error ? e.message : String(e)}
  finally {busy.value = false}
}
async function analyze() {
  if (busy.value || !data.value || !selected.value) return
  busy.value = true; error.value = ''
  try {
    await host.updateModelContext({structuredContent: {
      selectedEntityIds: [selected.value], filterSummary: `${start.value} 至 ${end.value}`,
    }})
    const result = await host.sendMessage({role: 'user', content: [{type: 'text', text: `分析所选${account.value?.label ?? selected.value}的资金往来特点。`} ]})
    if (result.isError) throw new Error('继续分析暂不可用，请稍后重试')
  } catch (e) {error.value = e instanceof Error ? e.message : String(e)}
  finally {busy.value = false}
}
function filter() {
  void query('kg_fund_flow_filter', {startTime: start.value, endTime: end.value, minimumAmount: minimum.value})
}
onMounted(async () => {try {await host.connect()} catch (e) {error.value = String(e)}})
watch(selected, () => {cursor.value = ''; pageLoaded.value = false; transactions.value = data.value?.transactions ?? []})
</script>
<template>
  <main>
    <div class="notice">演示数据 · 金额保留原始精度 · 不同币种分别展示</div>
    <p v-if="error" role="alert" class="error">{{ error }}</p>
    <p v-if="!data">等待资金流数据…</p>
    <template v-else>
      <div class="filters" role="group" aria-label="资金流筛选" @keydown.enter.prevent="filter">
        <label>开始时间（含时区）<input v-model="start" :disabled="busy" required></label>
        <label>结束时间（不包含）<input v-model="end" :disabled="busy" required></label>
        <label>最低金额<input v-model="minimum" :disabled="busy" inputmode="decimal" required></label>
        <button type="button" :disabled="busy" @click="filter">{{ busy ? '查询中' : '查询' }}</button>
      </div>
      <section class="workspace">
        <div class="graph">
          <svg viewBox="0 0 440 230" role="img" aria-label="资金账户关系网络">
            <defs><marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="5" markerHeight="5" orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10 z" fill="#64748b"/></marker></defs>
            <g v-for="(edge, index) in data.edges" :key="edge.id">
              <path :d="edge.source === 'account_001' ? 'M90 110 Q200 20 350 110' : edge.source === 'account_002' ? 'M340 125 Q300 200 220 195' : 'M200 185 Q80 190 80 130'" stroke="#94a3b8" fill="none" marker-end="url(#arrow)"/>
              <text x="180" :y="40 + index * 17" font-size="11">{{ edge.amount }} {{ edge.currency }}</text>
            </g>
            <g v-for="(node, index) in data.nodes" :key="node.id" role="button" tabindex="0" :aria-label="`选择${node.label}`"
               @click="!busy && (selected = node.id)" @keydown.enter="!busy && (selected = node.id)" @keydown.space.prevent="!busy && (selected = node.id)">
              <circle :cx="[80, 350, 220][index]" :cy="[115, 115, 195][index]" r="30" :fill="selected === node.id ? '#4f46e5' : '#e0e7ff'"/>
              <text :x="[80, 350, 220][index]" :y="[119, 119, 199][index]" text-anchor="middle" :fill="selected === node.id ? 'white' : '#312e81'">{{ node.label }}</text>
            </g>
          </svg>
        </div>
        <aside><h3>{{ account?.label ?? '请选择账户' }}</h3><p>{{ account?.id }}</p>
          <button :disabled="busy || !selected" @click="query('kg_fund_flow_expand', {accountId: selected})">展开账户</button>
          <button :disabled="busy || !selected" @click="analyze">继续分析</button>
        </aside>
      </section>
      <p class="muted">数据时间：{{ data.generatedAt }} · 当前结果 {{ transactions.length }} 笔</p>
      <div class="table"><table><thead><tr><th>时间</th><th>来源</th><th>去向</th><th>金额</th><th>币种</th></tr></thead>
        <tbody><tr v-for="tx in transactions" :key="tx.id"><td>{{ tx.time }}</td><td>{{ tx.source }}</td><td>{{ tx.target }}</td><td>{{ tx.amount }}</td><td>{{ tx.currency }}</td></tr></tbody>
      </table></div>
      <p v-if="!transactions.length">当前条件没有交易</p>
      <button :disabled="busy || (pageLoaded && !cursor)" @click="query('kg_fund_flow_transactions', {...(selected ? {accountId: selected} : {}), ...(cursor ? {cursor} : {})})">{{ pageLoaded ? (cursor ? '加载更多' : '已全部加载') : '分页查询明细' }}</button>
    </template>
  </main>
</template>
<style>
*{box-sizing:border-box}body{margin:0;font:14px system-ui,sans-serif;color:#0f172a;background:white}main{padding:16px}.notice{padding:10px;background:#eef2ff;border-radius:8px;color:#4338ca}
.filters{display:flex;flex-wrap:wrap;gap:8px;margin:14px 0}label{display:flex;flex-direction:column;gap:4px;font-size:11px}input{padding:8px;border:1px solid #cbd5e1;border-radius:6px;width:180px}button{padding:8px 12px;border:0;border-radius:6px;color:white;background:#4f46e5;cursor:pointer}button:disabled{opacity:.5;cursor:default}
.workspace{display:grid;grid-template-columns:1fr 150px;border:1px solid #e2e8f0;border-radius:8px}.graph svg{width:100%;min-height:230px}aside{padding:12px;background:#f8fafc}aside button{margin:4px 0}aside p,.muted{font-size:11px;color:#64748b;overflow-wrap:anywhere}.table{overflow:auto}table{width:100%;border-collapse:collapse;font-size:11px}td,th{padding:8px;border-bottom:1px solid #e2e8f0;text-align:left}.error{color:#be123c}svg g[role=button]{cursor:pointer}@media(max-width:480px){.workspace{grid-template-columns:1fr}aside{display:flex;gap:8px;align-items:center}}
</style>
