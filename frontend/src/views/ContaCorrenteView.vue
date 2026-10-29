<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { isAdmin } from '../sessao'
import { deIso, fmtData, fmtDataHora, fmtMoeda, hoje, isoData, somarDias } from '../utils'
import { useEnvio } from '../composables/useEnvio'
import { useNotificar } from '../composables/useNotificar'
import { useTiposPalete } from '../composables/useTiposPalete'
import EmailCobranca from '../components/EmailCobranca.vue'
import WhatsAppAviso from '../components/WhatsAppAviso.vue'

const route = useRoute()
const router = useRouter()
const notificar = useNotificar()
const { enviando, enviar } = useEnvio()
const aba = ref(route.path.endsWith('cobrancas') ? 'cobrancas' : 'saldos')
const busca = ref('')
const saldos = ref([])
const porTransportadora = ref([])
const cobrancas = ref([])
const statusCob = ref('ABERTA')
const FORMAS = [
  { label: 'PIX', value: 'PIX' }, { label: 'Boleto', value: 'BOLETO' }, { label: 'Dinheiro', value: 'DINHEIRO' },
  { label: 'Desconto na NF', value: 'DESCONTO_NF' }, { label: 'Devolução física', value: 'DEVOLUCAO_FISICA' }
]
const STATUS = { ABERTA: ['Aberta', 'warn'], PAGA: ['Paga', 'success'], CANCELADA: ['Cancelada', 'secondary'] }
const dlg = reactive({ tipo: null, alvo: null, quantidade: null, valorUnitario: 45, vencimento: null, observacao: '', forma: 'PIX', motivo: '' })
const erro = ref('')
const tiposPalete = useTiposPalete()
/** Valor sugerido: reposição do primeiro tipo ativo com valor (PBR na carga inicial). */
const valorSugerido = computed(() => Number(tiposPalete.ativos.value.find((t) => Number(t.valorReposicao) > 0)?.valorReposicao ?? 45))
const totalAberto = computed(() => saldos.value.reduce((s, x) => s + x.saldoEmAberto, 0))

async function carregar() {
  try {
    const f = busca.value.trim() || null
    tiposPalete.carregar().catch(() => {})
    ;[saldos.value, cobrancas.value, porTransportadora.value] = await Promise.all([
      api('/paletes/saldos', { params: { fornecedor: f } }),
      api('/paletes/cobrancas', { params: { fornecedor: f, status: statusCob.value } }),
      api('/paletes/saldos-transportadora').catch(() => [])
    ])
  } catch (e) {
    notificar.erro(e)
  }
}

/** Agrupamento padrão vem da configuração do vale-palete (fornecedor ou transportadora), como no Gestão CD. */
onMounted(async () => {
  try {
    const cfg = await api('/vales-palete/configuracao')
    if (cfg.agrupamento === 'transportadora' && aba.value === 'saldos') aba.value = 'transportadoras'
  } catch {
    /* mantém fornecedor */
  }
})

function abrir(tipo, alvo) {
  Object.assign(dlg, { tipo, alvo, quantidade: alvo?.saldoEmAberto ?? null, valorUnitario: valorSugerido.value, vencimento: deIso(somarDias(hoje(), 10)), observacao: '', forma: 'PIX', motivo: '' })
  erro.value = ''
}

function confirmar() {
  erro.value = ''
  if (dlg.tipo === 'cobrar' && (!dlg.quantidade || dlg.quantidade < 1 || dlg.quantidade > dlg.alvo.saldoEmAberto)) erro.value = `Quantidade entre 1 e ${dlg.alvo.saldoEmAberto}`
  else if (dlg.tipo === 'cobrar' && !dlg.vencimento) erro.value = 'Informe o vencimento'
  else if (dlg.tipo === 'cancelar' && !dlg.motivo.trim()) erro.value = 'Informe o motivo'
  if (erro.value) return
  enviar(async () => {
    try {
      if (dlg.tipo === 'cobrar') {
        await api('/paletes/cobrancas', { method: 'POST', body: { fornecedor: dlg.alvo.fornecedor, quantidade: dlg.quantidade, valorUnitario: dlg.valorUnitario, vencimento: isoData(dlg.vencimento), observacao: dlg.observacao.trim() || null } })
        notificar.sucesso('Cobrança registrada')
      } else if (dlg.tipo === 'pagar') {
        const r = await api(`/paletes/cobrancas/${dlg.alvo.id}/pagamento`, { method: 'POST', body: { forma: dlg.forma } })
        notificar.sucesso(`Pagamento registrado · recibo ${r.reciboNumero}`)
        router.push(`/paletes/recibos/${r.id}`)
      } else {
        await api(`/paletes/cobrancas/${dlg.alvo.id}/cancelamento`, { method: 'PATCH', body: { motivo: dlg.motivo.trim() } })
        notificar.sucesso('Cobrança cancelada')
      }
      dlg.tipo = null
      carregar()
    } catch (e) {
      notificar.erro(e)
    }
  })
}
onMounted(carregar)
defineExpose({ abrir, confirmar, dlg, erro })
</script>

<template>
  <div class="cabecalho">
    <h1>Conta-corrente de paletes</h1>
    <form class="acoes" @submit.prevent="carregar"><InputText v-model="busca" placeholder="Fornecedor" aria-label="Fornecedor" /><Button type="submit" icon="pi pi-search" class="btn-secundario" aria-label="Buscar" /></form>
  </div>
  <div class="abas">
    <button :class="{ ativa: aba === 'saldos' }" @click="aba = 'saldos'">Saldo por fornecedor</button>
    <button :class="{ ativa: aba === 'transportadoras' }" @click="aba = 'transportadoras'">Por transportadora</button>
    <button :class="{ ativa: aba === 'cobrancas' }" @click="aba = 'cobrancas'">Cobranças e recibos</button>
  </div>

  <section v-if="aba === 'saldos'" class="painel">
    <div class="painel-titulo"><h2>Paletes em poder dos fornecedores</h2><span class="texto-suave">{{ totalAberto }} paletes em aberto</span></div>
    <DataTable :value="saldos" size="small" striped-rows>
      <template #empty><div class="vazio">Nenhum vale-palete emitido</div></template>
      <Column field="fornecedor" header="Fornecedor" sortable />
      <Column field="vales" header="Vales" class="num" />
      <Column field="entregues" header="Entregues" class="num" />
      <Column field="devolvidos" header="Devolvidos" class="num" />
      <Column field="cobrados" header="Cobrados" class="num" />
      <Column field="pagos" header="Pagos" class="num" />
      <Column header="Saldo em aberto" class="num" sortable sort-field="saldoEmAberto"><template #body="{ data }"><strong :class="data.saldoEmAberto > 0 ? 'erro' : ''">{{ data.saldoEmAberto }}</strong></template></Column>
      <Column header="A receber" class="num"><template #body="{ data }">{{ fmtMoeda(data.valorEmAberto) }}</template></Column>
      <Column v-if="isAdmin" header=""><template #body="{ data }"><Button v-if="data.saldoEmAberto > 0" label="Cobrar" icon="pi pi-wallet" class="btn-confirmar" size="small" @click="abrir('cobrar', data)" /></template></Column>
    </DataTable>
  </section>

  <section v-else-if="aba === 'transportadoras'" class="painel">
    <div class="painel-titulo"><h2>Vales agrupados por transportadora</h2><span class="texto-suave">Sem transportadora no agendamento, entra o fornecedor</span></div>
    <DataTable :value="porTransportadora" size="small" striped-rows>
      <template #empty><div class="vazio">Nenhum vale-palete emitido</div></template>
      <Column field="transportadora" header="Transportadora / fornecedor" sortable />
      <Column field="vales" header="Vales" class="num" />
      <Column field="entregues" header="Entregues" class="num" />
      <Column field="devolvidos" header="Devolvidos" class="num" />
      <Column header="Saldo" class="num" sortable sort-field="saldo"><template #body="{ data }"><strong :class="data.saldo > 0 ? 'erro' : ''">{{ data.saldo }}</strong></template></Column>
    </DataTable>
  </section>

  <section v-else class="painel">
    <div class="painel-titulo">
      <h2>Cobranças</h2>
      <Select v-model="statusCob" :options="[{ label: 'Abertas', value: 'ABERTA' }, { label: 'Pagas', value: 'PAGA' }, { label: 'Canceladas', value: 'CANCELADA' }, { label: 'Todas', value: null }]" option-label="label" option-value="value" aria-label="Status" @change="carregar" />
    </div>
    <DataTable :value="cobrancas" size="small" striped-rows>
      <template #empty><div class="vazio">Nenhuma cobrança</div></template>
      <Column field="id" header="#" class="num" />
      <Column field="fornecedor" header="Fornecedor" />
      <Column field="quantidade" header="Paletes" class="num" />
      <Column header="Valor" class="num"><template #body="{ data }">{{ fmtMoeda(data.valorTotal) }}</template></Column>
      <Column header="Vencimento"><template #body="{ data }"><span :class="data.vencida ? 'erro' : ''">{{ fmtData(data.vencimento) }}</span></template></Column>
      <Column header="Status"><template #body="{ data }"><Tag :value="STATUS[data.status][0]" :severity="STATUS[data.status][1]" /></template></Column>
      <Column header="Recibo"><template #body="{ data }"><a v-if="data.reciboNumero" href="#" @click.prevent="router.push(`/paletes/recibos/${data.id}`)">{{ data.reciboNumero }}</a><span v-else-if="data.pagoEm">{{ fmtDataHora(data.pagoEm) }}</span></template></Column>
      <Column header="Avisar"><template #body="{ data }">
        <div v-if="data.status !== 'CANCELADA'" class="botoes-aviso"><EmailCobranca :cobranca-id="data.id" :admin="isAdmin" /><WhatsAppAviso documento="cobrancas" :id="data.id" :admin="isAdmin" /></div>
      </template></Column>
      <Column v-if="isAdmin" header=""><template #body="{ data }">
        <template v-if="data.status === 'ABERTA'">
          <Button label="Receber" icon="pi pi-check" class="btn-confirmar" size="small" @click="abrir('pagar', data)" />
          <Button icon="pi pi-times" class="btn-excluir" size="small" aria-label="Cancelar cobrança" @click="abrir('cancelar', data)" />
        </template>
      </template></Column>
    </DataTable>
  </section>

  <Dialog :visible="!!dlg.tipo" modal :header="dlg.tipo === 'cobrar' ? `Cobrar paletes · ${dlg.alvo?.fornecedor}` : dlg.tipo === 'pagar' ? `Receber cobrança #${dlg.alvo?.id}` : `Cancelar cobrança #${dlg.alvo?.id}`" :style="{ width: '460px' }" @update:visible="(v) => !v && (dlg.tipo = null)">
    <form class="form-grade" @submit.prevent="confirmar">
      <template v-if="dlg.tipo === 'cobrar'">
        <div class="campo c6"><label for="cq">Paletes <span class="obrig">*</span></label><InputNumber v-model="dlg.quantidade" input-id="cq" :min="1" :max="dlg.alvo?.saldoEmAberto" /></div>
        <div class="campo c6"><label for="cv">Valor por palete</label><InputNumber v-model="dlg.valorUnitario" input-id="cv" mode="currency" currency="BRL" locale="pt-BR" :min="0" /></div>
        <div class="campo c6"><label for="cvc">Vencimento <span class="obrig">*</span></label><DatePicker v-model="dlg.vencimento" input-id="cvc" date-format="dd/mm/yy" /></div>
        <div class="campo c12"><label for="co">Observação</label><InputText id="co" v-model="dlg.observacao" maxlength="255" /></div>
        <p class="c12 texto-suave">Total: {{ fmtMoeda((dlg.quantidade || 0) * (dlg.valorUnitario || 0)) }}</p>
      </template>
      <div v-else-if="dlg.tipo === 'pagar'" class="campo c12"><label for="cf">Forma de pagamento</label><Select v-model="dlg.forma" input-id="cf" :options="FORMAS" option-label="label" option-value="value" /></div>
      <div v-else class="campo c12"><label for="cm">Motivo <span class="obrig">*</span></label><Textarea id="cm" v-model="dlg.motivo" rows="2" maxlength="255" /></div>
      <span v-if="erro" class="erro c12">{{ erro }}</span>
      <button type="submit" hidden />
    </form>
    <template #footer>
      <Button label="Voltar" class="btn-secundario" :disabled="enviando" @click="dlg.tipo = null" />
      <Button :label="dlg.tipo === 'cancelar' ? 'Cancelar cobrança' : 'Confirmar'" :class="dlg.tipo === 'cancelar' ? 'btn-excluir' : 'btn-confirmar'" icon="pi pi-check" :loading="enviando" :disabled="enviando" @click="confirmar" />
    </template>
  </Dialog>
</template>

<style scoped>
.botoes-aviso { display: flex; gap: 4px; }
.abas { display: flex; gap: 4px; margin-bottom: 10px; }
.abas button { border: 1px solid var(--gc-border); background: var(--gc-panel); color: var(--gc-text); padding: 7px 14px; border-radius: var(--raio, 6px); cursor: pointer; font: inherit; }
.abas button.ativa { background: var(--gc-topbar-bg); color: var(--gc-panel); border-color: transparent; }
</style>
