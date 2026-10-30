import { pode } from './sessao'

/** Menu do sistema, agrupado como as centrais do Gestão CD (usado no topo, no menu lateral e na home). */
export const CENTRAIS = {
  'Operação': { sigla: 'OPE', titulo: 'Operação do dia', icone: 'pi pi-th-large' },
  Recebimento: { sigla: 'REC', titulo: 'Recebimento', icone: 'pi pi-clipboard' },
  Paletes: { sigla: 'PAL', titulo: 'Paletes', icone: 'pi pi-box' },
  Cadastros: { sigla: 'CAD', titulo: 'Cadastros', icone: 'pi pi-database' },
  'Administração': { sigla: 'ADM', titulo: 'Acessos e auditoria', icone: 'pi pi-cog' }
}

/** p = permissão numerada exigida (lista: basta uma). */
const GRUPOS = [
  {
    grupo: 'Operação',
    itens: [
      { to: '/inicio', label: 'Início', icon: 'pi pi-home' },
      { to: '/painel', label: 'Ocupação', icon: 'pi pi-chart-bar', p: 1101 },
      { to: '/agendamentos/novo', label: 'Novo agendamento', icon: 'pi pi-plus', p: 1103 },
      { to: '/agendamentos', label: 'Agendamentos', icon: 'pi pi-calendar', p: 1102 },
      { to: '/vales-palete', label: 'Vale-palete', icon: 'pi pi-ticket', p: 1301 },
      { to: '/relatorio', label: 'Relatório', icon: 'pi pi-file', p: 1106 }
    ]
  },
  {
    grupo: 'Recebimento',
    itens: [
      { to: '/rotina', label: 'Rotina do dia', icon: 'pi pi-list-check', p: 1201 },
      { to: '/patio', label: 'Pátio e docas', icon: 'pi pi-truck', p: 1205 },
      { to: '/divergencias', label: 'Divergências', icon: 'pi pi-exclamation-triangle', p: 1202 },
      { to: '/relatorios/descarga', label: 'Tempos de descarga', icon: 'pi pi-stopwatch', p: 1203 }
    ]
  },
  {
    grupo: 'Paletes',
    itens: [
      { to: '/vales-palete', label: 'Vale-palete', icon: 'pi pi-ticket', p: 1301 },
      { to: '/paletes/saldos', label: 'Conta-corrente', icon: 'pi pi-wallet', p: 1304 },
      { to: '/paletes/cobrancas', label: 'Cobranças e recibos', icon: 'pi pi-receipt', p: 1304 },
      { to: '/paletes/lojas', label: 'Paletes nas lojas', icon: 'pi pi-shop', p: 1307 }
    ]
  },
  {
    grupo: 'Cadastros',
    itens: [
      { to: '/cadastros/cds', label: 'Centros de distribuição', icon: 'pi pi-building', p: 1401 },
      { to: '/cadastros/fornecedores', label: 'Fornecedores', icon: 'pi pi-users', p: 1402 },
      { to: '/cadastros/produtos', label: 'Produtos e pesos', icon: 'pi pi-barcode', p: 1403 },
      { to: '/cadastros/capacidade', label: 'Capacidade e faixas', icon: 'pi pi-sliders-h', p: 1404 },
      { to: '/cadastros/capacidade-recebimento', label: 'Capacidade por CD e loja', icon: 'pi pi-chart-pie', p: 1405 },
      { to: '/cadastros/docas', label: 'Docas', icon: 'pi pi-sign-in', p: 1406 },
      { to: '/cadastros/faixas', label: 'Faixas horárias', icon: 'pi pi-clock', p: 1404 },
      { to: '/cadastros/caminhoes', label: 'Caminhões', icon: 'pi pi-truck', p: 1407 },
      { to: '/cadastros/descarga', label: 'Tempos de descarga', icon: 'pi pi-stopwatch', p: 1408 },
      { to: '/cadastros/taxas-descarga', label: 'Taxa de descarga', icon: 'pi pi-dollar', p: 1409 },
      { to: '/cadastros/paletizacao', label: 'Paletização', icon: 'pi pi-box', p: 1407 },
      { to: '/cadastros/tipos-palete', label: 'Tipos de palete', icon: 'pi pi-th-large', p: 1410 },
      { to: '/cadastros/conferentes', label: 'Conferentes', icon: 'pi pi-id-card', p: 1411 }
    ]
  },
  {
    grupo: 'Administração',
    itens: [
      { to: '/acessos/usuarios', label: 'Usuários', icon: 'pi pi-user', p: [1001, 1002] },
      { to: '/acessos/perfis', label: 'Perfis e permissões', icon: 'pi pi-key', p: 1003 },
      { to: '/acessos/logs', label: 'Logs de acesso', icon: 'pi pi-history', p: 1004 },
      { to: '/auditoria', label: 'Auditoria', icon: 'pi pi-shield', p: 1005 }
    ]
  }
]

export function permiteItem(item, podeFn = pode) {
  if (!item.p) return true
  return (Array.isArray(item.p) ? item.p : [item.p]).some((c) => podeFn(c))
}

/** Grupos e itens que o perfil logado pode abrir; grupos vazios somem. */
export function menuItens(podeFn = pode) {
  return GRUPOS.map((g) => ({ grupo: g.grupo, itens: g.itens.filter((i) => permiteItem(i, podeFn)) })).filter((g) => g.itens.length)
}
