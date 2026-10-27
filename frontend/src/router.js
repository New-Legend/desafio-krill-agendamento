import { createRouter, createWebHistory } from 'vue-router'
import { logado, isAdmin } from './sessao'

const routes = [
  { path: '/', redirect: '/painel' },
  { path: '/login', name: 'login', component: () => import('./views/LoginView.vue'), meta: { publica: true, titulo: 'Entrar' } },
  { path: '/painel', name: 'painel', component: () => import('./views/PainelView.vue'), meta: { titulo: 'Ocupação' } },
  { path: '/agendamentos', name: 'agendamentos', component: () => import('./views/AgendamentosView.vue'), meta: { titulo: 'Agendamentos' } },
  { path: '/agendamentos/novo', name: 'novo', component: () => import('./views/NovoAgendamentoView.vue'), meta: { admin: true, titulo: 'Novo agendamento' } },
  { path: '/agendamentos/:id(\\d+)', name: 'detalhe', component: () => import('./views/AgendamentosView.vue'), meta: { titulo: 'Agendamentos' } },
  { path: '/relatorio', name: 'relatorio', component: () => import('./views/RelatorioView.vue'), meta: { titulo: 'Relatório' } },
  { path: '/cadastros/capacidade', name: 'capacidade', component: () => import('./views/CapacidadeView.vue'), meta: { admin: true, titulo: 'Capacidade diária' } },
  { path: '/cadastros/faixas', name: 'faixas', component: () => import('./views/FaixasView.vue'), meta: { admin: true, titulo: 'Faixas horárias' } },
  { path: '/cadastros/caminhoes', name: 'caminhoes', component: () => import('./views/TiposCaminhaoView.vue'), meta: { admin: true, titulo: 'Tipos de caminhão' } },
  { path: '/cadastros/paletizacao', name: 'paletizacao', component: () => import('./views/TiposPaletizacaoView.vue'), meta: { admin: true, titulo: 'Tipos de paletização' } },
  { path: '/auditoria', name: 'auditoria', component: () => import('./views/AuditoriaView.vue'), meta: { admin: true, titulo: 'Auditoria' } },
  { path: '/:pathMatch(.*)*', redirect: '/painel' }
]

export const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  if (!to.meta.publica && !logado.value) return { name: 'login', query: { voltar: to.fullPath } }
  if (to.name === 'login' && logado.value) return { name: 'painel' }
  if (to.meta.admin && !isAdmin.value) return { name: 'painel' }
  return true
})

router.afterEach((to) => {
  document.title = `${to.meta.titulo || 'Agendamento'} — Rede Krill`
})
