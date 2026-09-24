import { createApp } from 'vue'
import App from './App.vue'
import './style.css'
import {configure} from '@ac/agent-ui'
import {registerBuiltinRenderers} from '@ac/agent-ui/renderers'

registerBuiltinRenderers()
configure({
    credentials: 'include',
    resultHeaders: () => import.meta.env.DEV ? {
        'X-Tenant-Id': import.meta.env.VITE_TENANT_ID || 'local-tenant',
        'X-User-Id': import.meta.env.VITE_USER_ID || 'local-user',
    } : {},
})

createApp(App).mount('#app')
