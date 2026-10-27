<script setup>
import { computed, reactive, ref } from 'vue'
import { api } from '../api'
import { baixarArquivo, deIso, diaSemana, fmtData, fmtPct, hoje, isoData, nivelOcupacao, somarDias } from '../utils'
import { useEnvio } from '../composables/useEnvio'
import { useNotificar } from '../composables/useNotificar'
import StatusAgendamento from '../components/StatusAgendamento.vue'

const notificar = useNotificar()
const { enviando, enviar } = useEnvio()
const filtro = reactive({ inicio: deIso(somarDias(hoje(), -7)), fim: deIso(somarDias(hoje(), 7)), status: null })
const status = [{ label: 'Todos', value: null }, { label: 'Agendados', value: 'AGENDADO' }, { label: 'Cancelados', value: 'CANCELADO' }]
const lista = ref([])
const dias = ref([])
const buscou = ref(false)
const carregando = ref(false)

const totais = computed(() => {
  const ativos = lista.value.filter((a) => a.status === 'AGENDADO')
  return {
    total: lista.value.length,
    ativos: ativos.length,
    cancelados: lista.value.length - ativos.length,
    paletes: ativos.reduce((s, a) => s + a.quantidadePaletes, 0),
    equivalentes: ativos.reduce((s, a) => s + a.paletesEquivalentes, 0)
  }
})
const diasComMovimento = computed(() => dias.value.filter((d) => d.quantidadeAgendamentos > 0))

function params() {
  return { inicio: isoData(filtro.inicio), fim: isoData(filtro.fim), status: filtro.status }
}

function validar() {
  if (!filtro.inicio || !filtro.fim) {
    notificar.aviso('Informe o período')
    return false
  }
  if (filtro.fim < filtro.inicio) {
    notificar.aviso('Data final anterior à data inicial')
    return false
  }
  return true
}

async function buscar() {
  if (!validar()) return
  carregando.value = true
  try {
    const p = params()
    ;[lista.value, dias.value] = await Promise.all([api('/agendamentos', { params: p }), api('/ocupacao/periodo', { params: { inicio: p.inicio, fim: p.fim } })])
    buscou.value = true
  } catch (e) {
    notificar.erro(e)
  } finally {
    carregando.value = false
  }
}

function exportar() {
  if (!validar()) return
  enviar(async () => {
    try {
      const p = params()
      baixarArquivo(await api('/relatorios/agendamentos.csv', { params: p, blob: true }), `agendamentos_${p.inicio}_${p.fim}.csv`)
    } catch (e) {
      notificar.erro(e)
    }
  })
}
</script>

<template>
  <div class="cabecalho"><h1>Relatório de agendamentos</h1></div>

  <form class="painel filtros" @submit.prevent="buscar">
    <div class="campo"><label for="ri">De <span class="obrig">*</span></label><DatePicker v-model="filtro.inicio" input-id="ri" date-format="dd/mm/yy" show-icon icon-display="input" /></div>
    <div class="campo"><label for="rf">Até <span class="obrig">*</span></label><DatePicker v-model="filtro.fim" input-id="rf" date-format="dd/mm/yy" show-icon icon-display="input" /></div>
    <div class="campo"><label for="rs">Status</label><Select v-model="filtro.status" input-id="rs" :options="status" option-label="label" option-value="value" /></div>
    <Button type="submit" label="Gerar" icon="pi pi-search" class="btn-confirmar" :loading="carregando" />
    <Button type="button" label="Exportar CSV" icon="pi pi-download" class="btn-secundario" :loading="enviando" :disabled="enviando" @click="exportar" />
  </form>

  <template v-if="buscou">
    <div class="kpis">
      <div class="kpi"><span>Agendamentos</span><strong>{{ totais.total }}</strong></div>
      <div class="kpi"><span>Ativos / cancelados</span><strong>{{ totais.ativos }} / {{ totais.cancelados }}</strong></div>
      <div class="kpi"><span>Paletes informados (ativos)</span><strong>{{ totais.paletes }}</strong></div>
      <div class="kpi"><span>Paletes na ocupação (ativos)</span><strong>{{ totais.equivalentes }}</strong></div>
    </div>

    <section class="painel">
      <h2>Ocupação por dia</h2>
      <DataTable :value="diasComMovimento" size="small" striped-rows>
        <template #empty><div class="vazio">Sem agendamentos ativos no período</div></template>
        <Column header="Data"><template #body="{ data }">{{ fmtData(data.data) }} <span class="texto-suave">{{ diaSemana(data.data) }}</span></template></Column>
        <Column field="quantidadeAgendamentos" header="Agendamentos" class="num" />
        <Column header="Paletes" class="num"><template #body="{ data }">{{ data.paletesOcupados }} / {{ data.capacidade }}</template></Column>
        <Column header="Ocupação" class="num"><template #body="{ data }"><span class="pct" :class="nivelOcupacao(data.percentual)">{{ fmtPct(data.percentual) }}</span></template></Column>
        <Column v-for="(f, i) in (dias[0]?.faixas || [])" :key="f.faixaHorariaId" :header="f.descricao" class="num">
          <template #body="{ data }"><span class="pct" :class="nivelOcupacao(data.faixas[i]?.percentual)">{{ fmtPct(data.faixas[i]?.percentual) }}</span></template>
        </Column>
      </DataTable>
    </section>

    <section class="painel">
      <div class="painel-titulo"><h2>Agendamentos do período</h2><span class="texto-suave">{{ lista.length }} registro(s)</span></div>
      <DataTable :value="lista" size="small" striped-rows paginator :rows="50" :row-class="(r) => (r.status === 'CANCELADO' ? 'linha-cancelada' : '')">
        <template #empty><div class="vazio">Nenhum agendamento no período</div></template>
        <Column field="id" header="#" class="num" />
        <Column header="Data" sortable sort-field="data"><template #body="{ data }">{{ fmtData(data.data) }}</template></Column>
        <Column field="faixaHoraria" header="Faixa" />
        <Column field="idPedido" header="Pedido" />
        <Column field="fornecedor" header="Fornecedor" sortable />
        <Column field="emailFornecedor" header="E-mail" />
        <Column field="tipoCaminhao" header="Caminhão" />
        <Column field="tipoPaletizacao" header="Paletização" />
        <Column field="quantidadePaletes" header="Paletes" class="num" />
        <Column field="paletesEquivalentes" header="Ocupação" class="num" />
        <Column header="Status"><template #body="{ data }"><StatusAgendamento :status="data.status" /></template></Column>
      </DataTable>
    </section>
  </template>
</template>
