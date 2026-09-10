import { createApp } from 'vue'
import App from './App.vue'
import './style.css'
import { registerBuiltinRenderers } from '@ac/agent-ui/renderers'

registerBuiltinRenderers()

createApp(App).mount('#app')
