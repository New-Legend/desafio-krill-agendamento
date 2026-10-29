/** Menu do sistema, agrupado como as centrais do Gestão CD (usado no topo, no menu lateral e na home). */
export const CENTRAIS = {
  'Operação': { sigla: 'OPE', titulo: 'Operação do dia', icone: 'pi pi-th-large' },
  Recebimento: { sigla: 'REC', titulo: 'Recebimento', icone: 'pi pi-clipboard' },
  Paletes: { sigla: 'PAL', titulo: 'Paletes', icone: 'pi pi-box' },
  Cadastros: { sigla: 'CAD', titulo: 'Cadastros', icone: 'pi pi-database' },
  'Administração': { sigla: 'Administração', titulo: 'Acessos e auditoria', icone: 'pi pi-cog' }
}

export function menuItens(admin) {
  const base = [
    { to: '/inicio', label: 'Início', icon: 'pi pi-home' },
    { to: '/painel', label: 'Ocupação', icon: 'pi pi-chart-bar' },
    { to: '/agendamentos', label: 'Agendamentos', icon: 'pi pi-calendar' },
    { to: '/vales-palete', label: 'Vale-palete', icon: 'pi pi-ticket' },
    { to: '/relatorio', label: 'Relatório', icon: 'pi pi-file' }
  ]
  const recebimento = {
    grupo: 'Recebimento',
    itens: [
      { to: '/rotina', label: 'Rotina do dia', icon: 'pi pi-list-check' },
      { to: '/patio', label: 'Pátio e docas', icon: 'pi pi-truck' },
      { to: '/divergencias', label: 'Divergências', icon: 'pi pi-exclamation-triangle' },
      { to: '/relatorios/descarga', label: 'Tempos de descarga', icon: 'pi pi-stopwatch' }
    ]
  }
  const paletes = {
    grupo: 'Paletes',
    itens: [
      base[3],
      { to: '/paletes/saldos', label: 'Conta-corrente', icon: 'pi pi-wallet' },
      { to: '/paletes/cobrancas', label: 'Cobranças e recibos', icon: 'pi pi-receipt' },
      { to: '/paletes/lojas', label: 'Paletes nas lojas', icon: 'pi pi-shop' }
    ]
  }
  if (!admin) return [{ grupo: 'Operação', itens: base }, recebimento, paletes]
  return [
    { grupo: 'Operação', itens: [base[0], base[1], { to: '/agendamentos/novo', label: 'Novo agendamento', icon: 'pi pi-plus' }, base[2], base[3], base[4]] },
    recebimento,
    paletes,
    {
      grupo: 'Cadastros',
      itens: [
        { to: '/cadastros/cds', label: 'Centros de distribuição', icon: 'pi pi-building' },
        { to: '/cadastros/fornecedores', label: 'Fornecedores', icon: 'pi pi-users' },
        { to: '/cadastros/produtos', label: 'Produtos e pesos', icon: 'pi pi-barcode' },
        { to: '/cadastros/capacidade', label: 'Capacidade e faixas', icon: 'pi pi-sliders-h' },
        { to: '/cadastros/capacidade-recebimento', label: 'Capacidade por CD e loja', icon: 'pi pi-chart-pie' },
        { to: '/cadastros/docas', label: 'Docas', icon: 'pi pi-sign-in' },
        { to: '/cadastros/faixas', label: 'Faixas horárias', icon: 'pi pi-clock' },
        { to: '/cadastros/caminhoes', label: 'Caminhões', icon: 'pi pi-truck' },
        { to: '/cadastros/descarga', label: 'Tempos de descarga', icon: 'pi pi-stopwatch' },
        { to: '/cadastros/taxas-descarga', label: 'Taxa de descarga', icon: 'pi pi-dollar' },
        { to: '/cadastros/paletizacao', label: 'Paletização', icon: 'pi pi-box' },
        { to: '/cadastros/tipos-palete', label: 'Tipos de palete', icon: 'pi pi-th-large' },
        { to: '/cadastros/conferentes', label: 'Conferentes', icon: 'pi pi-id-card' }
      ]
    },
    { grupo: 'Administração', itens: [{ to: '/auditoria', label: 'Auditoria', icon: 'pi pi-shield' }] }
  ]
}
