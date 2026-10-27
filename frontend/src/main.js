import { createApp } from 'vue'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import { definePreset } from '@primeuix/themes'
import Aura from '@primeuix/themes/aura'
import 'primeicons/primeicons.css'
import './styles/app.css'
import App from './App.vue'
import { router } from './router'
import { quandoSessaoExpirar } from './api'
import { registrarComponentes } from './componentes'
import { ptBR } from './locale'

const Tema = definePreset(Aura, {
  semantic: {
    primary: {
      50: '#ecfdf3', 100: '#d1fadf', 200: '#a6f4c5', 300: '#6ce9a6', 400: '#32d583',
      500: '#12b76a', 600: '#0f7b3f', 700: '#0b6633', 800: '#05603a', 900: '#054f31', 950: '#032b1b'
    },
    colorScheme: {
      light: {
        formField: { background: '#f1f5f9', disabledBackground: '#e2e8f0', borderColor: '#d7e0eb', hoverBorderColor: '#94a3b8' }
      }
    }
  }
})

const app = createApp(App)
app.use(PrimeVue, {
  theme: { preset: Tema, options: { darkModeSelector: false, cssLayer: { name: 'primevue' } } },
  locale: ptBR
})
app.use(ToastService)
app.use(ConfirmationService)
app.use(router)
registrarComponentes(app)
quandoSessaoExpirar(() => router.push({ name: 'login' }))
app.mount('#app')

if ('serviceWorker' in navigator && import.meta.env.PROD) {
  window.addEventListener('load', () => navigator.serviceWorker.register('/sw.js', { scope: '/' }))
}
