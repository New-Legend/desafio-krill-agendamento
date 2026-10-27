<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api'
import { baixarArquivo, deIso, fmtData, fmtMin, fmtMoeda, hoje, isoData, somarDias } from '../utils'
import { useEnvio } from '../composables/useEnvio'
import { useNotificar } from '../composables/useNotificar'
import SeletorCd from '../components/SeletorCd.vue'

const route = useRoute()
const notificar = useNotificar()
const { enviando, enviar } = useEnvio()
const filtro = reactive({ inicio: deIso(route.query.inicio || somarDias(hoje(), -7)), fim: deIso(route.query.fim || hoje()), cdId: null })
const dados = ref(null)
const carregando = ref(false)
const SITUACAO = { AGUARDANDO: ['Aguardando', 'info'], EM_DESCARGA: ['Em descarga', 'warn'], NO_PRAZO: ['No prazo', 'success'], ACIMA_DO_PREVISTO: ['Acima do previsto', 'danger'] }

function params() {
  return { inicio: isoData(filtro.inicio), fim: isoData(filtro.fim), cdId: filtro.cdId }
}
async function buscar() {
  if (!filtro.inicio || !filtro.fim) return notificar.aviso('Informe o período')
  carregando.value = true
  try {
    dados.value = await api('/relatorios/descarga', { params: params() })
  } catch (e) {
    notificar.erro(e)
  } finally {
    carregando.value = false
  }
}
function exportar() {
  enviar(async () => {
    try {
      const p = params()
      baixarArquivo(await api('/relatorios/descarga.csv', { params: p, blob: true }), `descarga_${p.inicio}_${p.fim}.csv`)
    } catch (e) {
      notificar.erro(e)
    }
  })
}
onMounted(() => { if (route.query.inicio) buscar() })
</script>

<template>
  <div class="cabecalho"><h1>Tempos de espera e descarga</h1></div>
  <form class="painel filtros" @submit.prevent="buscar">
    <div class="campo"><label for="ti">De</label><DatePicker v-model="filtro.inicio" input-id="ti" date-format="dd/mm/yy" show-icon icon-display="input" /></div>
    <div class="campo"><label for="tf">Até</label><DatePicker v-model="filtro.fim" input-id="tf" date-format="dd/mm/yy" show-icon icon-display="input" /></div>
    <SeletorCd v-model="filtro.cdId" input-id="tcd" />
    <Button type="submit" label="Gerar" icon="pi pi-search" class="btn-confirmar" :loading="carregando" />
    <Button type="button" label="Exportar CSV" icon="pi pi-download" class="btn-secundario" :loading="enviando" :disabled="enviando" @click="exportar" />
  </form>
  <template v-if="dados">
    <section class="painel">
      <h2>Resumo por tipo de caminhão</h2>
      <DataTable :value="dados.resumo" size="small" striped-rows>
        <template #empty><div class="vazio">Sem chegadas no período</div></template>
        <Column field="tipoCaminhao" header="Caminhão" />
        <Column field="recebimentos" header="Recebimentos" class="num" />
        <Column header="Espera média" class="num"><template #body="{ data }">{{ fmtMin(Math.round(data.mediaEsperaMinutos)) }}</template></Column>
        <Column header="Descarga média" class="num"><template #body="{ data }">{{ fmtMin(Math.round(data.mediaDescargaMinutos)) }}</template></Column>
        <Column header="Previsto" class="num"><template #body="{ data }">{{ fmtMin(data.previstoMinutos) }}</template></Column>
        <Column field="acimaDoPrevisto" header="Acima do previsto" class="num" />
        <Column header="Custo de descarga" class="num"><template #body="{ data }">{{ fmtMoeda(data.custoTotal) }}</template></Column>
      </DataTable>
    </section>
    <section class="painel">
      <h2>Recebimentos</h2>
      <DataTable :value="dados.linhas" size="small" striped-rows paginator :rows="50">
        <template #empty><div class="vazio">Sem chegadas no período</div></template>
        <Column header="Data"><template #body="{ data }">{{ fmtData(data.agendamento.data) }} · {{ data.agendamento.faixaHoraria }}</template></Column>
        <Column header="Fornecedor"><template #body="{ data }">{{ data.agendamento.fornecedor }}</template></Column>
        <Column header="Caminhão"><template #body="{ data }">{{ data.agendamento.tipoCaminhao }} {{ data.agendamento.placa || '' }}</template></Column>
        <Column header="Espera" class="num"><template #body="{ data }"><span :class="data.indicador.esperaAcimaTolerancia ? 'erro' : ''">{{ fmtMin(data.indicador.esperaMinutos) }}</span></template></Column>
        <Column header="Descarga" class="num"><template #body="{ data }">{{ fmtMin(data.indicador.descargaMinutos) }}</template></Column>
        <Column header="Excesso" class="num"><template #body="{ data }">{{ data.indicador.excessoMinutos ? fmtMin(data.indicador.excessoMinutos) : '—' }}</template></Column>
        <Column header="Custo" class="num"><template #body="{ data }">{{ fmtMoeda(data.indicador.custo) }}</template></Column>
        <Column header="Situação"><template #body="{ data }"><Tag :value="SITUACAO[data.indicador.situacao][0]" :severity="SITUACAO[data.indicador.situacao][1]" /></template></Column>
      </DataTable>
    </section>
  </template>
</template>
