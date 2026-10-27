<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { isAdmin } from '../sessao'
import { deIso, fmtData, fmtDataHora, hoje, isoData, somarDias } from '../utils'
import { useEnvio } from '../composables/useEnvio'
import { useNotificar } from '../composables/useNotificar'
import StatusAgendamento from '../components/StatusAgendamento.vue'
import EscolhaHorario from '../components/EscolhaHorario.vue'
import NotasFiscais from '../components/NotasFiscais.vue'
import SeletorCd from '../components/SeletorCd.vue'

const route = useRoute()
const router = useRouter()
const notificar = useNotificar()
const { enviando, enviar } = useEnvio()

const filtro = reactive({ inicio: deIso(hoje()), fim: deIso(somarDias(hoje(), 30)), status: route.query.status || null, fornecedor: '', idPedido: null, cdId: null })
const status = [
  { label: 'Todos', value: null }, { label: 'Agendado', value: 'AGENDADO' }, { label: 'Confirmado', value: 'CONFIRMADO' },
  { label: 'Chegou', value: 'CHEGOU' }, { label: 'Em recebimento', value: 'EM_RECEBIMENTO' }, { label: 'Recebido', value: 'RECEBIDO' },
  { label: 'Cancelado', value: 'CANCELADO' }, { label: 'Não compareceu', value: 'NAO_COMPARECEU' }
]
const acao = ref(null)
const remarcando = ref(false)
const diasRemarcar = ref([])
const novoHorario = ref(null)
const podeRemarcar = computed(() => ['AGENDADO', 'CONFIRMADO'].includes(detalhe.value?.status))

async function abrirRemarcar() {
  novoHorario.value = null
  try {
    diasRemarcar.value = await api('/ocupacao/periodo', { params: { inicio: hoje(), fim: somarDias(hoje(), 13), cdId: detalhe.value.cdId } })
    remarcando.value = true
  } catch (e) {
    notificar.erro(e)
  }
}

function confirmarRemarcacao() {
  if (!novoHorario.value) return notificar.aviso('Escolha a nova data e faixa')
  enviar(async () => {
    try {
      const r = await api(`/agendamentos/${detalhe.value.id}/data`, { method: 'PATCH', body: { data: novoHorario.value.data, faixaHorariaId: novoHorario.value.faixaHorariaId } })
      detalhe.value = r.agendamento
      ;(r.alertas || []).forEach((a) => notificar.aviso(a.mensagem))
      notificar.sucesso('Agendamento remarcado e fornecedor avisado')
      remarcando.value = false
      buscar()
    } catch (e) {
      notificar.erro(e)
    }
  })
}
const op = reactive({ placa: '', motorista: '', conferente: '', paletesRecebidos: null, divergencia: '' })
const link = ref('')
const podeCancelar = computed(() => ['AGENDADO', 'CONFIRMADO'].includes(detalhe.value?.status))
const acoes = computed(() => {
  const s = detalhe.value?.status
  const r = []
  if (s === 'AGENDADO') r.push({ k: 'CONFIRMAR', label: 'Confirmar', icon: 'pi pi-check' })
  if (s === 'AGENDADO' || s === 'CONFIRMADO') r.push({ k: 'CHEGADA', label: 'Registrar chegada', icon: 'pi pi-truck' }, { k: 'NAO_COMPARECEU', label: 'Não compareceu', icon: 'pi pi-ban', excluir: true })
  if (s === 'CHEGOU') r.push({ k: 'INICIAR', label: 'Iniciar conferência', icon: 'pi pi-play' })
  if (s === 'EM_RECEBIMENTO') r.push({ k: 'CONCLUIR', label: 'Concluir recebimento', icon: 'pi pi-check-square' })
  return r
})
const TITULOS = { CONFIRMAR: 'Confirmar agendamento', CHEGADA: 'Chegada na portaria', INICIAR: 'Início da conferência', CONCLUIR: 'Conclusão do recebimento', NAO_COMPARECEU: 'Registrar não comparecimento' }

function prepararAcao(k) {
  acao.value = k
  op.placa = detalhe.value.placa || ''
  op.motorista = detalhe.value.motorista || ''
  op.conferente = detalhe.value.conferente || ''
  op.paletesRecebidos = detalhe.value.quantidadePaletes
  op.divergencia = ''
}

function executarAcao() {
  if (acao.value === 'CHEGADA' && !op.placa.trim()) return notificar.aviso('Informe a placa do veículo')
  if (acao.value === 'INICIAR' && !op.conferente.trim()) return notificar.aviso('Informe o conferente')
  if (acao.value === 'CONCLUIR' && (op.paletesRecebidos === null || op.paletesRecebidos < 0)) return notificar.aviso('Informe os paletes recebidos')
  enviar(async () => {
    try {
      const r = await api(`/agendamentos/${detalhe.value.id}/status`, {
        method: 'PATCH',
        body: { acao: acao.value, placa: op.placa.trim() || null, motorista: op.motorista.trim() || null, conferente: op.conferente.trim() || null,
          paletesRecebidos: op.paletesRecebidos, divergencia: op.divergencia.trim() || null }
      })
      detalhe.value = r
      acao.value = null
      notificar.sucesso(`${r.statusDescricao}: agendamento #${r.id}`)
      if (r.divergencia) notificar.aviso(r.divergencia, 'Divergência registrada')
      buscar()
    } catch (e) {
      notificar.erro(e)
    }
  })
}

function gerarLink() {
  enviar(async () => {
    try {
      const r = await api(`/agendamentos/${detalhe.value.id}/link-fornecedor`, { method: 'POST' })
      link.value = r.linkFornecedor
      await navigator.clipboard?.writeText(r.linkFornecedor).catch(() => {})
      notificar.sucesso('Novo link gerado e copiado. O link anterior deixou de valer.')
    } catch (e) {
      notificar.erro(e)
    }
  })
}
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
      params: { inicio: isoData(filtro.inicio), fim: isoData(filtro.fim), status: filtro.status, fornecedor: filtro.fornecedor.trim(), idPedido: filtro.idPedido, cdId: filtro.cdId }
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
  remarcando.value = false
  acao.value = null
  link.value = ''
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
    <SeletorCd v-model="filtro.cdId" input-id="fcd" />
    <div class="campo"><label for="fp">ID do pedido</label><InputNumber v-model="filtro.idPedido" input-id="fp" :use-grouping="false" /></div>
    <Button type="submit" label="Buscar" icon="pi pi-search" class="btn-confirmar" :loading="carregando" />
  </form>

  <section class="painel">
    <div class="painel-titulo"><h2>Resultado</h2><span class="texto-suave">{{ lista.length }} registro(s)</span></div>
    <DataTable :value="lista" :loading="carregando" size="small" striped-rows row-hover data-key="id" paginator :rows="25"
               :row-class="(r) => (['CANCELADO', 'NAO_COMPARECEU'].includes(r.status) ? 'linha-cancelada' : '')" @row-click="(e) => router.push(`/agendamentos/${e.data.id}`)">
      <template #empty><div class="vazio">Nenhum agendamento encontrado para o filtro</div></template>
      <Column field="id" header="#" class="num" />
      <Column header="Data" sortable sort-field="data"><template #body="{ data }">{{ fmtData(data.data) }}</template></Column>
      <Column field="faixaHoraria" header="Faixa" />
      <Column field="idPedido" header="Pedido" />
      <Column field="fornecedor" header="Fornecedor" sortable />
      <Column field="tipoCaminhao" header="Caminhão" />
      <Column field="tipoPaletizacao" header="Paletização" />
      <Column header="Paletes" class="num"><template #body="{ data }">{{ data.quantidadePaletes }}<span v-if="data.paletesEquivalentes !== data.quantidadePaletes" class="texto-suave"> ({{ data.paletesEquivalentes }})</span></template></Column>
      <Column field="placa" header="Placa" />
      <Column header="Status"><template #body="{ data }"><StatusAgendamento :status="data.status" /></template></Column>
    </DataTable>
  </section>

  <Dialog v-model:visible="dialogo" modal :header="detalhe ? `Agendamento #${detalhe.id}` : ''" :style="{ width: '720px' }" :breakpoints="{ '680px': '96vw' }">
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
        <template v-if="detalhe.placa || detalhe.motorista">
          <dt>Veículo</dt><dd>{{ detalhe.placa || '—' }} · {{ detalhe.motorista || 'motorista não informado' }}</dd>
        </template>
        <template v-if="detalhe.chegadaEm"><dt>Chegada</dt><dd>{{ fmtDataHora(detalhe.chegadaEm) }}</dd></template>
        <template v-if="detalhe.inicioRecebimentoEm"><dt>Conferência</dt><dd>{{ fmtDataHora(detalhe.inicioRecebimentoEm) }} · {{ detalhe.conferente }}</dd></template>
        <template v-if="detalhe.fimRecebimentoEm"><dt>Recebido</dt><dd>{{ fmtDataHora(detalhe.fimRecebimentoEm) }} · {{ detalhe.paletesRecebidos }} paletes</dd></template>
        <template v-if="detalhe.divergencia"><dt>Divergência</dt><dd class="erro">{{ detalhe.divergencia }}</dd></template>
        <template v-if="detalhe.status === 'CANCELADO'">
          <dt>Cancelado</dt><dd>{{ fmtDataHora(detalhe.canceladoEm) }} por {{ detalhe.canceladoPor }}</dd>
          <dt>Motivo</dt><dd>{{ detalhe.motivoCancelamento }}</dd>
        </template>
      </dl>
      <div v-if="remarcando" class="painel acao-op">
        <h2>Remarcar entrega</h2>
        <p class="texto-suave">Escolha o novo dia e faixa. Faixas sem vaga podem ser usadas pelo CD (apenas alerta), o fornecedor só vê as que têm vaga.</p>
        <EscolhaHorario v-model="novoHorario" :dias="diasRemarcar" :paletes="detalhe.paletesEquivalentes" :atual="{ data: detalhe.data, faixaHorariaId: detalhe.faixaHorariaId }" />
        <div class="acoes">
          <Button label="Voltar" class="btn-secundario" :disabled="enviando" @click="remarcando = false" />
          <Button label="Remarcar" icon="pi pi-calendar" class="btn-confirmar" :loading="enviando" :disabled="enviando || !novoHorario" @click="confirmarRemarcacao" />
        </div>
      </div>
      <NotasFiscais v-if="['CHEGOU', 'EM_RECEBIMENTO', 'RECEBIDO'].includes(detalhe.status)" :key="detalhe.id" :agendamento-id="detalhe.id" :admin="isAdmin" :concluido="detalhe.status === 'RECEBIDO'" />
      <div v-if="link" class="link-fornecedor">
        <label for="lk">Link do fornecedor</label>
        <InputText id="lk" :model-value="link" readonly @focus="$event.target.select()" />
      </div>
      <form v-if="acao" class="painel acao-op" @submit.prevent="executarAcao">
        <h2>{{ TITULOS[acao] }}</h2>
        <div class="form-grade">
          <template v-if="acao === 'CHEGADA'">
            <div class="campo c4"><label for="op-placa">Placa <span class="obrig">*</span></label><InputText id="op-placa" v-model="op.placa" maxlength="8" autofocus /></div>
            <div class="campo c8"><label for="op-mot">Motorista</label><InputText id="op-mot" v-model="op.motorista" maxlength="80" /></div>
          </template>
          <div v-if="acao === 'INICIAR'" class="campo c12"><label for="op-conf">Conferente <span class="obrig">*</span></label><InputText id="op-conf" v-model="op.conferente" maxlength="80" autofocus /></div>
          <template v-if="acao === 'CONCLUIR'">
            <div class="campo c4"><label for="op-pal">Paletes recebidos <span class="obrig">*</span></label><InputNumber v-model="op.paletesRecebidos" input-id="op-pal" :min="0" :max="10000" /></div>
            <div class="campo c8"><label for="op-div">Divergência</label><InputText id="op-div" v-model="op.divergencia" maxlength="255" /><small class="texto-suave">Se a contagem diferir do agendado, a divergência é registrada automaticamente.</small></div>
          </template>
          <p v-if="acao === 'CONFIRMAR' || acao === 'NAO_COMPARECEU'" class="c12 texto-suave">{{ acao === 'CONFIRMAR' ? 'Confirma que o fornecedor virá na data e faixa agendadas.' : 'O agendamento deixa de ocupar capacidade.' }}</p>
        </div>
        <div class="acoes">
          <Button label="Voltar" type="button" class="btn-secundario" :disabled="enviando" @click="acao = null" />
          <Button label="Salvar" type="submit" icon="pi pi-check" :class="acao === 'NAO_COMPARECEU' ? 'btn-excluir' : 'btn-confirmar'" :loading="enviando" :disabled="enviando" />
        </div>
      </form>
      <div v-if="cancelando" class="campo c12" style="margin-top: 12px">
        <label for="motivo">Motivo do cancelamento <span class="obrig">*</span></label>
        <Textarea id="motivo" v-model="motivo" rows="2" maxlength="255" :invalid="!!erroMotivo" autofocus />
        <span v-if="erroMotivo" class="erro">{{ erroMotivo }}</span>
      </div>
    </template>
    <template #footer>
      <template v-if="detalhe && isAdmin && !acao && !cancelando && !remarcando">
        <Button v-for="a in acoes" :key="a.k" :label="a.label" :icon="a.icon" :class="a.excluir ? 'btn-excluir' : 'btn-confirmar'" @click="prepararAcao(a.k)" />
        <Button v-if="podeRemarcar" label="Remarcar" icon="pi pi-calendar" class="btn-secundario" @click="abrirRemarcar" />
        <Button v-if="['AGENDADO', 'CONFIRMADO'].includes(detalhe.status)" label="Link do fornecedor" icon="pi pi-link" class="btn-secundario" :disabled="enviando" @click="gerarLink" />
        <Button v-if="['CHEGOU', 'EM_RECEBIMENTO', 'RECEBIDO'].includes(detalhe.status)" label="Emitir vale-palete" icon="pi pi-ticket" class="btn-secundario" @click="router.push({ path: '/vales-palete/novo', query: { agendamento: detalhe.id } })" />
      </template>
      <template v-if="detalhe && isAdmin && podeCancelar && !acao && !remarcando">
        <template v-if="cancelando">
          <Button label="Voltar" class="btn-secundario" :disabled="enviando" @click="cancelando = false" />
          <Button label="Confirmar cancelamento" icon="pi pi-times" class="btn-excluir" :loading="enviando" :disabled="enviando" @click="confirmarCancelamento" />
        </template>
        <Button v-else label="Cancelar agendamento" icon="pi pi-times" class="btn-excluir" @click="cancelando = true" />
      </template>
      <Button v-if="!cancelando && !acao && !remarcando" label="Fechar" class="btn-secundario" @click="fechar" />
    </template>
  </Dialog>
</template>

<style scoped>
.link-fornecedor { display: flex; flex-direction: column; gap: 4px; margin-top: 12px; }
.acao-op { margin: 12px 0 0; background: var(--cor-fundo); }
.acao-op .acoes { display: flex; justify-content: flex-end; gap: 8px; margin-top: 10px; }
</style>
