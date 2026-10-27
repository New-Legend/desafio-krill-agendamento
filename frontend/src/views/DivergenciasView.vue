<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { baixarArquivo, deIso, fmtData, hoje, isoData, somarDias } from '../utils'
import { useEnvio } from '../composables/useEnvio'
import { useNotificar } from '../composables/useNotificar'
import SeletorCd from '../components/SeletorCd.vue'

const router = useRouter()
const route = useRoute()
const notificar = useNotificar()
const { enviando, enviar } = useEnvio()
const filtro = reactive({ inicio: deIso(route.query.inicio || somarDias(hoje(), -30)), fim: deIso(route.query.fim || hoje()), fornecedor: '', cdId: null })
const lista = ref([])
const buscou = ref(false)
const carregando = ref(false)

function params() {
  return { inicio: isoData(filtro.inicio), fim: isoData(filtro.fim), fornecedor: filtro.fornecedor.trim(), cdId: filtro.cdId }
}
function valido() {
  if (!filtro.inicio || !filtro.fim) return notificar.aviso('Informe o período'), false
  if (filtro.fim < filtro.inicio) return notificar.aviso('Data final anterior à data inicial'), false
  return true
}
async function buscar() {
  if (!valido()) return
  carregando.value = true
  try {
    lista.value = await api('/divergencias', { params: params() })
    buscou.value = true
  } catch (e) {
    notificar.erro(e)
  } finally {
    carregando.value = false
  }
}
function exportar() {
  if (!valido()) return
  enviar(async () => {
    try {
      const p = params()
      baixarArquivo(await api('/divergencias.csv', { params: p, blob: true }), `divergencias_${p.inicio}_${p.fim}.csv`)
    } catch (e) {
      notificar.erro(e)
    }
  })
}
onMounted(() => { if (route.query.inicio) buscar() })
defineExpose({ buscar, exportar, filtro })
</script>

<template>
  <div class="cabecalho"><h1>Divergências de recebimento</h1></div>
  <form class="painel filtros" @submit.prevent="buscar">
    <div class="campo"><label for="di">De <span class="obrig">*</span></label><DatePicker v-model="filtro.inicio" input-id="di" date-format="dd/mm/yy" show-icon icon-display="input" /></div>
    <div class="campo"><label for="df">Até <span class="obrig">*</span></label><DatePicker v-model="filtro.fim" input-id="df" date-format="dd/mm/yy" show-icon icon-display="input" /></div>
    <div class="campo"><label for="dfo">Fornecedor</label><InputText id="dfo" v-model="filtro.fornecedor" /></div>
    <SeletorCd v-model="filtro.cdId" input-id="dcd" />
    <Button type="submit" label="Buscar" icon="pi pi-search" class="btn-confirmar" :loading="carregando" />
    <Button type="button" label="Exportar CSV" icon="pi pi-download" class="btn-secundario" :loading="enviando" :disabled="enviando" @click="exportar" />
  </form>
  <section v-if="buscou" class="painel">
    <div class="painel-titulo"><h2>Recebimentos com divergência</h2><span class="texto-suave">{{ lista.length }} registro(s)</span></div>
    <DataTable :value="lista" size="small" striped-rows row-hover paginator :rows="50" @row-click="(e) => router.push(`/agendamentos/${e.data.agendamento.id}`)">
      <template #empty><div class="vazio">Nenhuma divergência no período</div></template>
      <Column header="#" class="num"><template #body="{ data }">{{ data.agendamento.id }}</template></Column>
      <Column header="Data"><template #body="{ data }">{{ fmtData(data.agendamento.data) }}</template></Column>
      <Column header="Pedido"><template #body="{ data }">{{ data.agendamento.idPedido }}</template></Column>
      <Column header="Fornecedor"><template #body="{ data }">{{ data.agendamento.fornecedor }}</template></Column>
      <Column header="Agendado" class="num"><template #body="{ data }">{{ data.agendamento.quantidadePaletes }}</template></Column>
      <Column header="Recebido" class="num"><template #body="{ data }">{{ data.agendamento.paletesRecebidos ?? '—' }}</template></Column>
      <Column header="Diferença" class="num"><template #body="{ data }"><span :class="data.diferencaPaletes < 0 ? 'erro' : ''">{{ data.diferencaPaletes > 0 ? '+' : '' }}{{ data.diferencaPaletes }}</span></template></Column>
      <Column header="NF-e" class="num"><template #body="{ data }">{{ data.notas }}<Tag v-if="data.pesoNfDivergente" value="peso" severity="warn" class="ml" /></template></Column>
      <Column header="Divergência"><template #body="{ data }">{{ data.agendamento.divergencia }}</template></Column>
    </DataTable>
  </section>
</template>

<style scoped>
.ml { margin-left: 6px; }
</style>
