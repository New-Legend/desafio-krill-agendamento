import { createRouter, createWebHistory } from 'vue-router'
import { logado, isAdmin } from './sessao'

export const routes = [
  { path: '/', redirect: '/inicio' },
  { path: '/inicio', name: 'inicio', component: () => import('./views/InicioView.vue'), meta: { titulo: 'Início' } },
  { path: '/fornecedor/:token', name: 'fornecedor', component: () => import('./views/FornecedorPublicoView.vue'), meta: { publica: true, livre: true, titulo: 'Seu agendamento' } },
  { path: '/vales-palete', name: 'vales', component: () => import('./views/ValesPaleteView.vue'), meta: { titulo: 'Vale-palete' } },
  { path: '/vales-palete/novo', name: 'vale-novo', component: () => import('./views/ValePaleteView.vue'), meta: { admin: true, titulo: 'Novo vale-palete' } },
  { path: '/vales-palete/:id(\\d+)', name: 'vale', component: () => import('./views/ValePaleteView.vue'), meta: { titulo: 'Vale-palete' } },
  { path: '/login', name: 'login', component: () => import('./views/LoginView.vue'), meta: { publica: true, titulo: 'Entrar' } },
  { path: '/painel', name: 'painel', component: () => import('./views/PainelView.vue'), meta: { titulo: 'Ocupação da semana' } },
  { path: '/agendamentos', name: 'agendamentos', component: () => import('./views/AgendamentosView.vue'), meta: { titulo: 'Agendamentos' } },
  { path: '/agendamentos/novo', name: 'novo', component: () => import('./views/NovoAgendamentoView.vue'), meta: { admin: true, titulo: 'Novo agendamento' } },
  { path: '/agendamentos/:id(\\d+)', name: 'detalhe', component: () => import('./views/AgendamentosView.vue'), meta: { titulo: 'Agendamentos' } },
  { path: '/relatorio', name: 'relatorio', component: () => import('./views/RelatorioView.vue'), meta: { titulo: 'Relatório' } },
  { path: '/cadastros/capacidade', name: 'capacidade', component: () => import('./views/CapacidadeView.vue'), meta: { admin: true, titulo: 'Capacidade diária' } },
  { path: '/cadastros/faixas', name: 'faixas', component: () => import('./views/FaixasView.vue'), meta: { admin: true, titulo: 'Faixas horárias' } },
  { path: '/cadastros/caminhoes', name: 'caminhoes', component: () => import('./views/TiposCaminhaoView.vue'), meta: { admin: true, titulo: 'Tipos de caminhão' } },
  { path: '/cadastros/paletizacao', name: 'paletizacao', component: () => import('./views/TiposPaletizacaoView.vue'), meta: { admin: true, titulo: 'Tipos de paletização' } },
  { path: '/rotina', name: 'rotina', component: () => import('./views/RotinaView.vue'), meta: { titulo: 'Rotina do dia' } },
  { path: '/divergencias', name: 'divergencias', component: () => import('./views/DivergenciasView.vue'), meta: { titulo: 'Divergências' } },
  { path: '/relatorios/descarga', name: 'descarga', component: () => import('./views/DescargaView.vue'), meta: { titulo: 'Tempos de descarga' } },
  { path: '/paletes/saldos', name: 'saldos', component: () => import('./views/ContaCorrenteView.vue'), meta: { titulo: 'Conta-corrente de paletes' } },
  { path: '/paletes/cobrancas', name: 'cobrancas', component: () => import('./views/ContaCorrenteView.vue'), meta: { titulo: 'Cobranças de paletes' } },
  { path: '/paletes/recibos/:id(\\d+)', name: 'recibo', component: () => import('./views/ReciboPaleteView.vue'), meta: { titulo: 'Recibo de paletes' } },
  { path: '/paletes/lojas', name: 'lojas', component: () => import('./views/MovimentosPaleteView.vue'), meta: { titulo: 'Paletes nas lojas' } },
  { path: '/cadastros/cds', name: 'cds', component: () => import('./views/CentrosDistribuicaoView.vue'), meta: { admin: true, titulo: 'Centros de distribuição' } },
  { path: '/cadastros/fornecedores', name: 'fornecedores', component: () => import('./views/FornecedoresView.vue'), meta: { admin: true, titulo: 'Fornecedores' } },
  { path: '/cadastros/produtos', name: 'produtos', component: () => import('./views/ProdutosView.vue'), meta: { admin: true, titulo: 'Produtos e pesos' } },
  { path: '/cadastros/descarga', name: 'parametros-descarga', component: () => import('./views/ParametrosDescargaView.vue'), meta: { admin: true, titulo: 'Tempos de descarga' } },
  { path: '/auditoria', name: 'auditoria', component: () => import('./views/AuditoriaView.vue'), meta: { admin: true, titulo: 'Auditoria' } },
  { path: '/:pathMatch(.*)*', redirect: '/inicio' }
]

export const router = createRouter({ history: createWebHistory(), routes })

/** Guarda de rota: telas internas exigem login; telas `admin` exigem perfil ADMIN (o backend também valida). */
export function guarda(to) {
  if (!to.meta.publica && !logado.value) return { name: 'login', query: { voltar: to.fullPath } }
  if (to.name === 'login' && logado.value) return { name: 'inicio' }
  if (to.meta.admin && !isAdmin.value) return { name: 'inicio' }
  return true
}
router.beforeEach(guarda)

router.afterEach((to) => {
  document.title = `${to.meta.titulo || 'Agendamento'} — Rede Krill`
})
