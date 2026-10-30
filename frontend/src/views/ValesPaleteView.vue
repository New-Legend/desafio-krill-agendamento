<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'
import { pode } from '../sessao'
import { deIso, fmtData, hoje, isoData, somarDias } from '../utils'
import { useNotificar } from '../composables/useNotificar'

const router = useRouter()
const notificar = useNotificar()
const filtro = reactive({ inicio: deIso(somarDias(hoje(), -30)), fim: deIso(hoje()), status: null, fornecedor: '' })
const codigo = ref('')
const lista = ref([])
const carregando = ref(false)
const STATUS = [
  { label: 'Todos', value: null }, { label: 'Pendente de assinatura', value: 'PENDENTE' }, { label: 'Assinado', value: 'ASSINADO' },
  { label: 'Quitado', value: 'QUITADO' }, { label: 'Cancelado', value: 'CANCELADO' }
]
const SEV = { PENDENTE: 'warn', ASSINADO: 'info', QUITADO: 'success', CANCELADO: 'danger' }

async function buscar() {
  carregando.value = true
  try {
    lista.value = await api('/vales-palete', { params: { inicio: isoData(filtro.inicio), fim: isoData(filtro.fim), status: filtro.status, fornecedor: filtro.fornecedor.trim() } })
  } catch (e) {
    notificar.erro(e)
  } finally {
    carregando.value = false
  }
}

async function porCodigo() {
  if (!codigo.value.trim()) return
  try {
    const v = await api(`/vales-palete/codigo/${encodeURIComponent(codigo.value.trim().toUpperCase())}`)
    router.push(`/vales-palete/${v.id}`)
  } catch (e) {
    notificar.erro(e)
  }
}
onMounted(buscar)
</script>

<template>
  <div class="cabecalho">
    <h1>Vale-palete</h1>
    <div class="acoes">
      <form class="busca-codigo" @submit.prevent="porCodigo"><InputText v-model="codigo" placeholder="VP-AAAAMMDD-0001" aria-label="Código do vale" /><Button type="submit" icon="pi pi-search" class="btn-secundario" aria-label="Abrir vale" /></form>
      <Button v-if="pode(1302)" label="Novo vale" icon="pi pi-plus" class="btn-confirmar" @click="router.push('/vales-palete/novo')" />
    </div>
  </div>
  <form class="painel filtros" @submit.prevent="buscar">
    <div class="campo"><label for="vi">De</label><DatePicker v-model="filtro.inicio" input-id="vi" date-format="dd/mm/yy" show-icon icon-display="input" /></div>
    <div class="campo"><label for="vf">Até</label><DatePicker v-model="filtro.fim" input-id="vf" date-format="dd/mm/yy" show-icon icon-display="input" /></div>
    <div class="campo"><label for="vs">Status</label><Select v-model="filtro.status" input-id="vs" :options="STATUS" option-label="label" option-value="value" /></div>
    <div class="campo"><label for="vfo">Fornecedor</label><InputText id="vfo" v-model="filtro.fornecedor" /></div>
    <Button type="submit" label="Buscar" icon="pi pi-search" class="btn-confirmar" :loading="carregando" />
  </form>
  <section class="painel">
    <div class="painel-titulo"><h2>Vales</h2><span class="texto-suave">{{ lista.length }} registro(s)</span></div>
    <DataTable :value="lista" :loading="carregando" size="small" striped-rows row-hover data-key="id" paginator :rows="25" @row-click="(e) => router.push(`/vales-palete/${e.data.id}`)">
      <template #empty><div class="vazio">Nenhum vale no período</div></template>
      <Column field="codigo" header="Código" />
      <Column header="Data"><template #body="{ data }">{{ fmtData(data.data) }}</template></Column>
      <Column field="fornecedor" header="Fornecedor" />
      <Column field="placa" header="Placa" />
      <Column header="Entregues" class="num"><template #body="{ data }">{{ data.totalEntregues }}</template></Column>
      <Column header="Saldo" class="num"><template #body="{ data }"><b>{{ data.totalSaldo }}</b></template></Column>
      <Column header="Status"><template #body="{ data }"><Tag :value="STATUS.find((s) => s.value === data.status)?.label" :severity="SEV[data.status]" /></template></Column>
    </DataTable>
  </section>
</template>

<style scoped>
.busca-codigo { display: flex; gap: 4px; }
</style>
