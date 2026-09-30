import {registerRenderer} from '../core/registry'
import ChartRenderer from './ChartRenderer.vue'
import RelationGraphRenderer from './RelationGraphRenderer.vue'
import TableRenderer from './TableRenderer.vue'
import TimelineRenderer from './TimelineRenderer.vue'
import MetricRenderer from './MetricRenderer.vue'
import EntityCardRenderer from './EntityCardRenderer.vue'
import TreeRenderer from './TreeRenderer.vue'
import HeatmapRenderer from './HeatmapRenderer.vue'
import RelationshipPathRenderer from './RelationshipPathRenderer.vue'
import EvidenceChainRenderer from './EvidenceChainRenderer.vue'

export {ChartRenderer, RelationGraphRenderer, TableRenderer, TimelineRenderer,
    MetricRenderer, EntityCardRenderer, TreeRenderer, HeatmapRenderer,
    RelationshipPathRenderer, EvidenceChainRenderer}

export function registerBuiltinRenderers(): void {
    registerRenderer('Chart', ChartRenderer)
    registerRenderer('RelationGraph', RelationGraphRenderer)
    registerRenderer('Timeline', TimelineRenderer)
    registerRenderer('Table', TableRenderer)
    registerRenderer('Metric', MetricRenderer)
    registerRenderer('EntityCard', EntityCardRenderer)
    registerRenderer('Tree', TreeRenderer)
    registerRenderer('Heatmap', HeatmapRenderer)
    registerRenderer('RelationshipPath', RelationshipPathRenderer)
    registerRenderer('EvidenceChain', EvidenceChainRenderer)
}
