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

// Paleta do Gestão CD (ver styles/tokens.css): primário = azul-marinho Krill #1e3a8a (topo de produção), toasts .atlas-toast.
const Tema = definePreset(Aura, {
  semantic: {
    primary: {
      50: '#eff6ff', 100: '#dbeafe', 200: '#bfdbfe', 300: '#93c5fd', 400: '#60a5fa',
      500: '#1d4ed8', 600: '#1e3a8a', 700: '#1e3a8a', 800: '#1e40af', 900: '#172554', 950: '#172554'
    },
    colorScheme: {
      light: {
        primary: { color: 'var(--gc-navy)', contrastColor: 'var(--gc-panel)', hoverColor: 'var(--gc-navy-hover)', activeColor: 'var(--gc-navy-hover)' },
        text: { color: 'var(--gc-text)', hoverColor: 'var(--gc-heading)', mutedColor: 'var(--gc-muted)' },
        content: { background: 'var(--gc-panel)', borderColor: 'var(--gc-border)', color: 'var(--gc-text)' },
        formField: {
          background: 'var(--gc-input-bg)', disabledBackground: 'var(--gc-input-disabled)', filledBackground: 'var(--gc-input-bg)',
          borderColor: 'var(--gc-border-strong)', hoverBorderColor: 'var(--gc-placeholder)', focusBorderColor: 'var(--gc-focus)',
          color: 'var(--gc-heading)', placeholderColor: 'var(--gc-placeholder)', floatLabelColor: 'var(--gc-muted)'
        },
        focusRing: { color: 'var(--gc-focus)' }
      }
    }
  },
  components: {
    toast: {
      colorScheme: {
        light: {
          info: { background: 'var(--gc-toast-info-bg)', borderColor: 'var(--gc-toast-info-border)', color: 'var(--gc-toast-info-text)', detailColor: 'var(--gc-toast-info-text)' },
          success: { background: 'var(--gc-toast-ok-bg)', borderColor: 'var(--gc-toast-ok-border)', color: 'var(--gc-toast-ok-text)', detailColor: 'var(--gc-toast-ok-text)' },
          warn: { background: 'var(--gc-toast-warn-bg)', borderColor: 'var(--gc-toast-warn-border)', color: 'var(--gc-toast-warn-text)', detailColor: 'var(--gc-toast-warn-text)' },
          error: { background: 'var(--gc-toast-error-bg)', borderColor: 'var(--gc-toast-error-border)', color: 'var(--gc-toast-error-text)', detailColor: 'var(--gc-toast-error-text)' }
        }
      }
    },
    tag: {
      colorScheme: {
        light: {
          success: { background: 'var(--gc-pill-ok-bg)', color: 'var(--gc-pill-ok)' },
          info: { background: 'var(--gc-st-agendado-bg)', color: 'var(--gc-st-agendado)' },
          warn: { background: 'var(--gc-pill-warn-bg)', color: 'var(--gc-pill-warn)' },
          danger: { background: 'var(--gc-pill-danger-bg)', color: 'var(--gc-pill-danger)' },
          secondary: { background: 'var(--gc-input-disabled)', color: 'var(--gc-st-nao-compareceu)' }
        }
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
