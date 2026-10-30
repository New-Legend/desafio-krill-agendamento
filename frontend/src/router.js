import { createRouter, createWebHistory } from 'vue-router'
import { logado, pode, sessao } from './sessao'

export const routes = [
  { path: '/', redirect: '/inicio' },
  { path: '/inicio', name: 'inicio', component: () => import('./views/InicioView.vue'), meta: { titulo: 'Início' } },
  { path: '/fornecedor/:token', name: 'fornecedor', component: () => import('./views/FornecedorPublicoView.vue'), meta: { publica: true, livre: true, titulo: 'Seu agendamento' } },
  { path: '/vales-palete', name: 'vales', component: () => import('./views/ValesPaleteView.vue'), meta: { permissao: 1301, titulo: 'Vale-palete' } },
  { path: '/vales-palete/novo', name: 'vale-novo', component: () => import('./views/ValePaleteView.vue'), meta: { permissao: 1302, titulo: 'Novo vale-palete' } },
  { path: '/vales-palete/:id(\\d+)', name: 'vale', component: () => import('./views/ValePaleteView.vue'), meta: { permissao: 1301, titulo: 'Vale-palete' } },
  { path: '/login', name: 'login', component: () => import('./views/LoginView.vue'), meta: { publica: true, titulo: 'Entrar' } },
  { path: '/painel', name: 'painel', component: () => import('./views/PainelView.vue'), meta: { permissao: 1101, titulo: 'Ocupação da semana' } },
  { path: '/agendamentos', name: 'agendamentos', component: () => import('./views/AgendamentosView.vue'), meta: { permissao: 1102, titulo: 'Agendamentos' } },
  { path: '/agendamentos/novo', name: 'novo', component: () => import('./views/NovoAgendamentoView.vue'), meta: { permissao: 1103, titulo: 'Novo agendamento' } },
  { path: '/agendamentos/:id(\\d+)', name: 'detalhe', component: () => import('./views/AgendamentosView.vue'), meta: { permissao: 1102, titulo: 'Agendamentos' } },
  { path: '/relatorio', name: 'relatorio', component: () => import('./views/RelatorioView.vue'), meta: { permissao: 1106, titulo: 'Relatório' } },
  { path: '/cadastros/capacidade', name: 'capacidade', component: () => import('./views/CapacidadeView.vue'), meta: { permissao: 1404, titulo: 'Capacidade diária' } },
  { path: '/cadastros/faixas', name: 'faixas', component: () => import('./views/FaixasView.vue'), meta: { permissao: 1404, titulo: 'Faixas horárias' } },
  { path: '/cadastros/caminhoes', name: 'caminhoes', component: () => import('./views/TiposCaminhaoView.vue'), meta: { permissao: 1407, titulo: 'Tipos de caminhão' } },
  { path: '/cadastros/paletizacao', name: 'paletizacao', component: () => import('./views/TiposPaletizacaoView.vue'), meta: { permissao: 1407, titulo: 'Tipos de paletização' } },
  { path: '/rotina', name: 'rotina', component: () => import('./views/RotinaView.vue'), meta: { permissao: 1201, titulo: 'Rotina do dia' } },
  { path: '/divergencias', name: 'divergencias', component: () => import('./views/DivergenciasView.vue'), meta: { permissao: 1202, titulo: 'Divergências' } },
  { path: '/relatorios/descarga', name: 'descarga', component: () => import('./views/DescargaView.vue'), meta: { permissao: 1203, titulo: 'Tempos de descarga' } },
  { path: '/paletes/saldos', name: 'saldos', component: () => import('./views/ContaCorrenteView.vue'), meta: { permissao: 1304, titulo: 'Conta-corrente de paletes' } },
  { path: '/paletes/cobrancas', name: 'cobrancas', component: () => import('./views/ContaCorrenteView.vue'), meta: { permissao: 1304, titulo: 'Cobranças de paletes' } },
  { path: '/paletes/recibos/:id(\\d+)', name: 'recibo', component: () => import('./views/ReciboPaleteView.vue'), meta: { permissao: 1304, titulo: 'Recibo de paletes' } },
  { path: '/paletes/lojas', name: 'lojas', component: () => import('./views/MovimentosPaleteView.vue'), meta: { permissao: 1307, titulo: 'Paletes nas lojas' } },
  { path: '/cadastros/cds', name: 'cds', component: () => import('./views/CentrosDistribuicaoView.vue'), meta: { permissao: 1401, titulo: 'Centros de distribuição' } },
  { path: '/cadastros/fornecedores', name: 'fornecedores', component: () => import('./views/FornecedoresView.vue'), meta: { permissao: 1402, titulo: 'Fornecedores' } },
  { path: '/cadastros/produtos', name: 'produtos', component: () => import('./views/ProdutosView.vue'), meta: { permissao: 1403, titulo: 'Produtos e pesos' } },
  { path: '/cadastros/descarga', name: 'parametros-descarga', component: () => import('./views/ParametrosDescargaView.vue'), meta: { permissao: 1408, titulo: 'Tempos de descarga' } },
  { path: '/cadastros/tipos-palete', name: 'tipos-palete', component: () => import('./views/TiposPaleteView.vue'), meta: { permissao: 1410, titulo: 'Tipos de palete' } },
  { path: '/cadastros/conferentes', name: 'conferentes', component: () => import('./views/ConferentesView.vue'), meta: { permissao: 1411, titulo: 'Conferentes' } },
  { path: '/patio', name: 'patio', component: () => import('./views/PatioView.vue'), meta: { permissao: 1205, titulo: 'Pátio e docas' } },
  { path: '/cadastros/docas', name: 'docas', component: () => import('./views/DocasView.vue'), meta: { permissao: 1406, titulo: 'Docas' } },
  { path: '/cadastros/capacidade-recebimento', name: 'capacidade-recebimento', component: () => import('./views/CapacidadeRecebimentoView.vue'), meta: { permissao: 1405, titulo: 'Capacidade por CD e loja' } },
  { path: '/cadastros/taxas-descarga', name: 'taxas-descarga', component: () => import('./views/TaxasDescargaView.vue'), meta: { permissao: 1409, titulo: 'Taxa de descarga por modal' } },
  { path: '/auditoria', name: 'auditoria', component: () => import('./views/AuditoriaView.vue'), meta: { permissao: 1005, titulo: 'Auditoria' } },
  { path: '/acessos/usuarios', name: 'usuarios', component: () => import('./views/UsuariosView.vue'), meta: { permissao: [1001, 1002], titulo: 'Usuários' } },
  { path: '/acessos/perfis', name: 'perfis', component: () => import('./views/PerfisView.vue'), meta: { permissao: 1003, titulo: 'Perfis e permissões' } },
  { path: '/acessos/logs', name: 'logs-acesso', component: () => import('./views/LogsAcessoView.vue'), meta: { permissao: 1004, titulo: 'Logs de acesso' } },
  { path: '/trocar-senha', name: 'trocar-senha', component: () => import('./views/TrocarSenhaView.vue'), meta: { semPermissoes: true, titulo: 'Trocar senha' } },
  { path: '/:pathMatch(.*)*', redirect: '/inicio' }
]

export const router = createRouter({ history: createWebHistory(), routes })

/** Telas abertas por quem tem ao menos uma das permissões da rota. */
export function permitido(rota) {
  const p = rota.meta?.permissao
  if (!p) return true
  return (Array.isArray(p) ? p : [p]).some((c) => pode(c))
}

/** Guarda de rota: login, troca de senha provisória e permissão numerada da tela (o backend também valida). */
export function guarda(to) {
  if (!to.meta.publica && !logado.value) return { name: 'login', query: { voltar: to.fullPath } }
  if (to.name === 'login' && logado.value) return { name: 'inicio' }
  if (logado.value && sessao.atual.trocarSenha && !to.meta.livre && to.name !== 'trocar-senha') return { name: 'trocar-senha' }
  if (!permitido(to)) return { name: 'inicio' }
  return true
}
router.beforeEach(guarda)

router.afterEach((to) => {
  document.title = `${to.meta.titulo || 'Agendamento'} — Rede Krill`
})
