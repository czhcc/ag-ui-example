import {registerRenderer} from '../core/registry'
import ChartRenderer from './ChartRenderer.vue'
import RelationGraphRenderer from './RelationGraphRenderer.vue'
import TableRenderer from './TableRenderer.vue'
import TimelineRenderer from './TimelineRenderer.vue'

export {ChartRenderer, RelationGraphRenderer, TableRenderer, TimelineRenderer}

export function registerBuiltinRenderers(): void {
    registerRenderer('Chart', ChartRenderer)
    registerRenderer('RelationGraph', RelationGraphRenderer)
    registerRenderer('Timeline', TimelineRenderer)
    registerRenderer('Table', TableRenderer)
}
