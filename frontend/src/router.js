import { createRouter, createWebHistory } from 'vue-router'
import { logado, isAdmin } from './sessao'

const routes = [
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
  { path: '/auditoria', name: 'auditoria', component: () => import('./views/AuditoriaView.vue'), meta: { admin: true, titulo: 'Auditoria' } },
  { path: '/:pathMatch(.*)*', redirect: '/inicio' }
]

export const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  if (!to.meta.publica && !logado.value) return { name: 'login', query: { voltar: to.fullPath } }
  if (to.name === 'login' && logado.value) return { name: 'inicio' }
  if (to.meta.admin && !isAdmin.value) return { name: 'inicio' }
  return true
})

router.afterEach((to) => {
  document.title = `${to.meta.titulo || 'Agendamento'} — Rede Krill`
})
