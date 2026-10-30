<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { pode } from '../sessao'
import { deIso, diaSemana, fmtData, fmtPct, hoje, isoData, nivelOcupacao, somarDias } from '../utils'
import { useNotificar } from '../composables/useNotificar'
import BarraOcupacao from '../components/BarraOcupacao.vue'
import StatusAgendamento from '../components/StatusAgendamento.vue'

const route = useRoute()
const router = useRouter()
const notificar = useNotificar()
const dataSel = ref(route.query.data || hoje())
const inicioSemana = ref(dataSel.value)
const semana = ref([])
const agendamentos = ref([])
const carregando = ref(false)

const dia = computed(() => semana.value.find((d) => d.data === dataSel.value))
const datePicker = computed({
  get: () => deIso(dataSel.value),
  set: (v) => { if (v) selecionar(isoData(v), true) }
})

async function carregarSemana() {
  semana.value = await api('/ocupacao/periodo', { params: { inicio: inicioSemana.value, fim: somarDias(inicioSemana.value, 6) } })
}
async function carregarDia() {
  agendamentos.value = await api('/agendamentos', { params: { inicio: dataSel.value, fim: dataSel.value, status: 'AGENDADO' } })
}
async function carregar() {
  carregando.value = true
  try {
    await Promise.all([carregarSemana(), carregarDia()])
  } catch (e) {
    notificar.erro(e)
  } finally {
    carregando.value = false
  }
}

function selecionar(d, recentrar = false) {
  dataSel.value = d
  if (recentrar || d < inicioSemana.value || d > somarDias(inicioSemana.value, 6)) inicioSemana.value = d
  router.replace({ query: { data: d } })
}
function navegar(n) {
  inicioSemana.value = somarDias(inicioSemana.value, n)
  selecionar(inicioSemana.value)
}

watch(dataSel, carregar)
onMounted(carregar)
</script>

<template>
  <div class="cabecalho">
    <h1>Ocupação do recebimento</h1>
    <div class="acoes">
      <Button icon="pi pi-angle-left" class="btn-secundario" aria-label="Semana anterior" @click="navegar(-7)" />
      <DatePicker v-model="datePicker" date-format="dd/mm/yy" show-icon icon-display="input" :manual-input="false" />
      <Button icon="pi pi-angle-right" class="btn-secundario" aria-label="Próxima semana" @click="navegar(7)" />
      <Button label="Hoje" class="btn-secundario" @click="selecionar(hoje(), true)" />
      <Button v-if="pode(1103)" label="Novo agendamento" icon="pi pi-plus" class="btn-confirmar" @click="router.push({ name: 'novo', query: { data: dataSel } })" />
    </div>
  </div>

  <section class="painel">
    <div class="dias">
      <button v-for="d in semana" :key="d.data" class="dia" :class="{ selecionado: d.data === dataSel }" @click="selecionar(d.data)">
        <div class="cab"><span>{{ diaSemana(d.data) }}</span><span>{{ fmtData(d.data).slice(0, 5) }}</span></div>
        <span class="pct" :class="nivelOcupacao(d.percentual)">{{ fmtPct(d.percentual) }}</span>
        <BarraOcupacao :percentual="d.percentual" :mostrar-texto="false" />
        <span class="val">{{ d.paletesOcupados }}/{{ d.capacidade }} · {{ d.quantidadeAgendamentos }} ag.</span>
      </button>
    </div>
  </section>

  <template v-if="dia">
    <div class="kpis">
      <div class="kpi"><span>Ocupação do dia {{ fmtData(dia.data) }}</span><strong class="pct" :class="nivelOcupacao(dia.percentual)">{{ fmtPct(dia.percentual) }}</strong></div>
      <div class="kpi"><span>Paletes ocupados</span><strong>{{ dia.paletesOcupados }}</strong></div>
      <div class="kpi"><span>Capacidade diária</span><strong>{{ dia.capacidade }}</strong></div>
      <div class="kpi"><span>{{ dia.excedente > 0 ? 'Excedente' : 'Saldo disponível' }}</span>
        <strong :class="{ 'pct excedido': dia.excedente > 0 }">{{ dia.excedente > 0 ? dia.excedente : dia.capacidade - dia.paletesOcupados }}</strong></div>
    </div>

    <section class="painel">
      <h2>Faixas horárias</h2>
      <div class="faixas">
        <div v-for="f in dia.faixas" :key="f.faixaHorariaId" class="faixa">
          <div class="cab"><strong>{{ f.descricao }}</strong><span class="pct" :class="nivelOcupacao(f.percentual)">{{ fmtPct(f.percentual) }}</span></div>
          <BarraOcupacao :percentual="f.percentual" :mostrar-texto="false" />
          <span class="texto-suave">{{ f.paletesOcupados }} / {{ f.capacidade }} paletes<template v-if="f.excedente"> · excedido em {{ f.excedente }}</template></span>
        </div>
      </div>
    </section>
  </template>

  <section class="painel">
    <div class="painel-titulo"><h2>Agendamentos do dia</h2><span class="texto-suave">{{ agendamentos.length }} registro(s)</span></div>
    <DataTable :value="agendamentos" :loading="carregando" size="small" striped-rows data-key="id"
               row-hover @row-click="(e) => router.push(`/agendamentos/${e.data.id}`)">
      <template #empty><div class="vazio">Nenhum agendamento ativo nesta data</div></template>
      <Column field="faixaHoraria" header="Faixa" />
      <Column field="idPedido" header="Pedido" />
      <Column field="fornecedor" header="Fornecedor" />
      <Column field="tipoCaminhao" header="Caminhão" />
      <Column field="tipoPaletizacao" header="Paletização" />
      <Column header="Paletes" class="num"><template #body="{ data }">{{ data.quantidadePaletes }}<span v-if="data.paletesEquivalentes !== data.quantidadePaletes" class="texto-suave"> ({{ data.paletesEquivalentes }})</span></template></Column>
      <Column header="Status"><template #body="{ data }"><StatusAgendamento :status="data.status" /></template></Column>
    </DataTable>
  </section>
</template>
