import type {ResultPayload, SurfaceComponent} from '../core/types'

export function rows(result: ResultPayload): Record<string, unknown>[] {
    if (Array.isArray(result.data)) return result.data.filter(
        (item): item is Record<string, unknown> => item !== null && typeof item === 'object' && !Array.isArray(item))
    return []
}

export function field(component: SurfaceComponent, key: string): string {
    const value = component.props.encoding?.[key]
    return typeof value === 'string' ? value : key
}

export function cell(row: Record<string, unknown>, name: string): string {
    const value = row[name]
    return value == null ? '' : String(value)
}
