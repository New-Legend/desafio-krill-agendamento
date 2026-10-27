<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { isAdmin } from '../sessao'
import { deIso, fmtData, fmtDataHora, hoje, isoData, somarDias } from '../utils'
import { useEnvio } from '../composables/useEnvio'
import { useNotificar } from '../composables/useNotificar'
import StatusAgendamento from '../components/StatusAgendamento.vue'

const route = useRoute()
const router = useRouter()
const notificar = useNotificar()
const { enviando, enviar } = useEnvio()

const filtro = reactive({ inicio: deIso(hoje()), fim: deIso(somarDias(hoje(), 30)), status: 'AGENDADO', fornecedor: '', idPedido: null })
const status = [{ label: 'Agendados', value: 'AGENDADO' }, { label: 'Cancelados', value: 'CANCELADO' }, { label: 'Todos', value: null }]
const lista = ref([])
const carregando = ref(false)
const detalhe = ref(null)
const motivo = ref('')
const cancelando = ref(false)
const erroMotivo = ref('')

const dialogo = computed({
  get: () => !!detalhe.value,
  set: (v) => { if (!v) fechar() }
})

async function buscar() {
  carregando.value = true
  try {
    lista.value = await api('/agendamentos', {
      params: { inicio: isoData(filtro.inicio), fim: isoData(filtro.fim), status: filtro.status, fornecedor: filtro.fornecedor.trim(), idPedido: filtro.idPedido }
    })
  } catch (e) {
    notificar.erro(e)
  } finally {
    carregando.value = false
  }
}

async function abrir(id) {
  try {
    detalhe.value = await api(`/agendamentos/${id}`)
  } catch (e) {
    notificar.erro(e)
    router.replace('/agendamentos')
  }
}

function fechar() {
  detalhe.value = null
  cancelando.value = false
  motivo.value = ''
  erroMotivo.value = ''
  if (route.params.id) router.replace('/agendamentos')
}

function confirmarCancelamento() {
  if (!motivo.value.trim()) {
    erroMotivo.value = 'Informe o motivo do cancelamento'
    return
  }
  enviar(async () => {
    try {
      const r = await api(`/agendamentos/${detalhe.value.id}/cancelamento`, { method: 'PATCH', body: { motivo: motivo.value.trim() } })
      notificar.sucesso(`Agendamento #${r.id} cancelado. Fornecedor avisado por e-mail.`)
      fechar()
      buscar()
    } catch (e) {
      notificar.erro(e)
    }
  })
}

watch(() => route.params.id, (id) => { if (id) abrir(id) })
onMounted(() => {
  buscar()
  if (route.params.id) abrir(route.params.id)
})
</script>

<template>
  <div class="cabecalho">
    <h1>Agendamentos</h1>
    <div class="acoes">
      <Button v-if="isAdmin" label="Novo agendamento" icon="pi pi-plus" class="btn-confirmar" @click="router.push('/agendamentos/novo')" />
    </div>
  </div>

  <form class="painel filtros" @submit.prevent="buscar">
    <div class="campo"><label for="fi">De</label><DatePicker v-model="filtro.inicio" input-id="fi" date-format="dd/mm/yy" show-icon icon-display="input" /></div>
    <div class="campo"><label for="ff">Até</label><DatePicker v-model="filtro.fim" input-id="ff" date-format="dd/mm/yy" show-icon icon-display="input" /></div>
    <div class="campo"><label for="fs">Status</label><Select v-model="filtro.status" input-id="fs" :options="status" option-label="label" option-value="value" /></div>
    <div class="campo"><label for="ffor">Fornecedor</label><InputText id="ffor" v-model="filtro.fornecedor" /></div>
    <div class="campo"><label for="fp">ID do pedido</label><InputNumber v-model="filtro.idPedido" input-id="fp" :use-grouping="false" /></div>
    <Button type="submit" label="Buscar" icon="pi pi-search" class="btn-confirmar" :loading="carregando" />
  </form>

  <section class="painel">
    <div class="painel-titulo"><h2>Resultado</h2><span class="texto-suave">{{ lista.length }} registro(s)</span></div>
    <DataTable :value="lista" :loading="carregando" size="small" striped-rows row-hover data-key="id" paginator :rows="25"
               :row-class="(r) => (r.status === 'CANCELADO' ? 'linha-cancelada' : '')" @row-click="(e) => router.push(`/agendamentos/${e.data.id}`)">
      <template #empty><div class="vazio">Nenhum agendamento encontrado para o filtro</div></template>
      <Column field="id" header="#" class="num" />
      <Column header="Data" sortable sort-field="data"><template #body="{ data }">{{ fmtData(data.data) }}</template></Column>
      <Column field="faixaHoraria" header="Faixa" />
      <Column field="idPedido" header="Pedido" />
      <Column field="fornecedor" header="Fornecedor" sortable />
      <Column field="tipoCaminhao" header="Caminhão" />
      <Column field="tipoPaletizacao" header="Paletização" />
      <Column header="Paletes" class="num"><template #body="{ data }">{{ data.quantidadePaletes }}<span v-if="data.paletesEquivalentes !== data.quantidadePaletes" class="texto-suave"> ({{ data.paletesEquivalentes }})</span></template></Column>
      <Column header="Status"><template #body="{ data }"><StatusAgendamento :status="data.status" /></template></Column>
    </DataTable>
  </section>

  <Dialog v-model:visible="dialogo" modal :header="detalhe ? `Agendamento #${detalhe.id}` : ''" :style="{ width: '640px' }" :breakpoints="{ '680px': '96vw' }">
    <template v-if="detalhe">
      <dl class="detalhes">
        <dt>Status</dt><dd><StatusAgendamento :status="detalhe.status" /></dd>
        <dt>Data / faixa</dt><dd>{{ fmtData(detalhe.data) }} · {{ detalhe.faixaHoraria }}</dd>
        <dt>ID do pedido</dt><dd>{{ detalhe.idPedido }}</dd>
        <dt>Fornecedor</dt><dd>{{ detalhe.fornecedor }}</dd>
        <dt>E-mail</dt><dd>{{ detalhe.emailFornecedor }}</dd>
        <dt>Caminhão</dt><dd>{{ detalhe.tipoCaminhao }}</dd>
        <dt>Paletização</dt><dd>{{ detalhe.tipoPaletizacao }}</dd>
        <dt>Paletes</dt><dd>{{ detalhe.quantidadePaletes }} informados · {{ detalhe.paletesEquivalentes }} na ocupação</dd>
        <dt>Observação</dt><dd>{{ detalhe.observacao || '—' }}</dd>
        <dt>Criado</dt><dd>{{ fmtDataHora(detalhe.criadoEm) }} por {{ detalhe.criadoPor }}</dd>
        <template v-if="detalhe.status === 'CANCELADO'">
          <dt>Cancelado</dt><dd>{{ fmtDataHora(detalhe.canceladoEm) }} por {{ detalhe.canceladoPor }}</dd>
          <dt>Motivo</dt><dd>{{ detalhe.motivoCancelamento }}</dd>
        </template>
      </dl>
      <div v-if="cancelando" class="campo c12" style="margin-top: 12px">
        <label for="motivo">Motivo do cancelamento <span class="obrig">*</span></label>
        <Textarea id="motivo" v-model="motivo" rows="2" maxlength="255" :invalid="!!erroMotivo" autofocus />
        <span v-if="erroMotivo" class="erro">{{ erroMotivo }}</span>
      </div>
    </template>
    <template #footer>
      <template v-if="detalhe && isAdmin && detalhe.status === 'AGENDADO'">
        <template v-if="cancelando">
          <Button label="Voltar" class="btn-secundario" :disabled="enviando" @click="cancelando = false" />
          <Button label="Confirmar cancelamento" icon="pi pi-times" class="btn-excluir" :loading="enviando" :disabled="enviando" @click="confirmarCancelamento" />
        </template>
        <Button v-else label="Cancelar agendamento" icon="pi pi-times" class="btn-excluir" @click="cancelando = true" />
      </template>
      <Button v-if="!cancelando" label="Fechar" class="btn-secundario" @click="fechar" />
    </template>
  </Dialog>
</template>
