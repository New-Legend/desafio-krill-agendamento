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
        background_color: '#f8fafc',
        theme_color: '#1e3a8a',
        icons: [
          { src: '/icons/icon-192.png', sizes: '192x192', type: 'image/png' },
          { src: '/icons/icon-512.png', sizes: '512x512', type: 'image/png' },
          { src: '/icons/maskable-192.png', sizes: '192x192', type: 'image/png', purpose: 'maskable' },
          { src: '/icons/maskable-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' }
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
    setupFiles: ['tests/setup.js'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'json-summary'],
      // Partes críticas: formulário e lista de agendamentos, cadastros, guarda de rota/login, telas do porte do recebimento e base (api/sessão/utils).
      include: [
        'src/utils.js', 'src/api.js', 'src/sessao.js', 'src/router.js', 'src/composables/**', 'src/components/**',
        'src/views/LoginView.vue', 'src/views/NovoAgendamentoView.vue', 'src/views/AgendamentosView.vue',
        'src/views/FornecedoresView.vue', 'src/views/CentrosDistribuicaoView.vue', 'src/views/ProdutosView.vue',
        'src/views/TiposCaminhaoView.vue', 'src/views/TiposPaletizacaoView.vue', 'src/views/FaixasView.vue',
        'src/views/ParametrosDescargaView.vue', 'src/views/FornecedorPublicoView.vue', 'src/views/DivergenciasView.vue',
        'src/views/DescargaView.vue', 'src/views/RotinaView.vue', 'src/views/ContaCorrenteView.vue',
        'src/views/ReciboPaleteView.vue', 'src/views/MovimentosPaleteView.vue'
      ],
      exclude: ['src/components/AssinaturaPad.vue'],
      // Funções ficam de fora do corte: o v8 conta cada arrow inline dos templates e cada import() lazy de rota como função.
      thresholds: { lines: 80, statements: 80, branches: 80 }
    }
  }
})
