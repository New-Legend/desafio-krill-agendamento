import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig({
  plugins: [
    vue(),
    VitePWA({
      strategies: 'injectManifest',
      srcDir: 'src',
      filename: 'sw.js',
      registerType: 'autoUpdate',
      injectRegister: false,
      devOptions: { enabled: false },
      injectManifest: { globPatterns: ['**/*.{js,css,html,png,svg,woff,woff2,ttf,eot}'] },
      manifest: {
        name: 'Agendamento de Recebimento — Rede Krill',
        short_name: 'Agendamento CD',
        description: 'Agendamento de caminhões e ocupação do recebimento',
        lang: 'pt-BR',
        start_url: '/painel',
        scope: '/',
        display: 'standalone',
        orientation: 'any',
        background_color: '#f3f6fa',
        theme_color: '#061d37',
        icons: [
          { src: '/icons/icon-192.png', sizes: '192x192', type: 'image/png' },
          { src: '/icons/icon-512.png', sizes: '512x512', type: 'image/png' },
          { src: '/icons/icon-maskable-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' }
        ]
      }
    })
  ],
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8080', '/swagger-ui': 'http://localhost:8080', '/v3': 'http://localhost:8080' }
  },
  test: {
    environment: 'jsdom',
    include: ['tests/**/*.spec.js'],
    coverage: { include: ['src/utils.js', 'src/api.js', 'src/sessao.js', 'src/composables/**', 'src/components/**'] }
  }
})
