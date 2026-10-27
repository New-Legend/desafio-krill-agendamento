<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { deIso, fmtData, fmtPct, hoje, isoData, nivelOcupacao, validarAgendamento } from '../utils'
import { useEnvio } from '../composables/useEnvio'
import { useNotificar } from '../composables/useNotificar'
import BarraOcupacao from '../components/BarraOcupacao.vue'
import SeletorCd from '../components/SeletorCd.vue'
import { fmtCnpj } from '../utils'

const route = useRoute()
const router = useRouter()
const notificar = useNotificar()
const { enviando, enviar } = useEnvio()

const vazio = () => ({
  data: deIso(route.query.data && route.query.data >= hoje() ? route.query.data : hoje()),
  faixaHorariaId: null, idPedido: null, fornecedor: '', emailFornecedor: '',
  tipoCaminhaoId: null, tipoPaletizacaoId: null, quantidadePaletes: null, observacao: '', cdId: null, fornecedorId: null
})
const form = reactive(vazio())
const erros = ref({})
const tentouEnviar = ref(false)
const caminhoes = ref([])
const paletizacoes = ref([])
const simulacao = ref(null)
const fornecedores = ref([])
function escolherFornecedor(id) {
  const f = fornecedores.value.find((x) => x.id === id)
  if (f) {
    form.fornecedor = (f.nomeFantasia || f.razaoSocial).slice(0, 80)
    form.emailFornecedor = f.email.slice(0, 80)
  }
}
const minData = deIso(hoje())

const ocupacao = computed(() => simulacao.value?.ocupacaoAtual)
const opcoesFaixa = computed(() => (ocupacao.value?.faixas || []).map((f) => ({
  value: f.faixaHorariaId,
  label: f.descricao,
  percentual: f.percentual,
  detalhe: `${f.paletesOcupados}/${f.capacidade}`
})))
const faixaSel = computed(() => ocupacao.value?.faixas.find((f) => f.faixaHorariaId === form.faixaHorariaId))
const paletizacaoSel = computed(() => paletizacoes.value.find((p) => p.id === form.tipoPaletizacaoId))
const equivalentes = computed(() => simulacao.value?.paletesEquivalentes || 0)
const posDia = computed(() => ocupacao.value ? pct(ocupacao.value.paletesOcupados + equivalentes.value, ocupacao.value.capacidade) : 0)
const posFaixa = computed(() => faixaSel.value ? pct(faixaSel.value.paletesOcupados + equivalentes.value, faixaSel.value.capacidade) : 0)
const contador = (s, max = 80) => `${(s || '').length}/${max}`

function pct(v, total) {
  return total ? Math.round((v * 1000) / total) / 10 : 0
}

let seq = 0
async function simular() {
  if (!form.data) return
  const atual = ++seq
  try {
    const r = await api('/agendamentos/simulacao', {
      method: 'POST',
      body: {
        data: isoData(form.data), faixaHorariaId: form.faixaHorariaId,
        tipoPaletizacaoId: form.tipoPaletizacaoId, quantidadePaletes: form.quantidadePaletes || 0, cdId: form.cdId
      }
    })
    if (atual === seq) simulacao.value = r
  } catch (e) {
    notificar.erro(e)
  }
}

watch(() => [form.cdId, form.data, form.faixaHorariaId, form.tipoPaletizacaoId, form.quantidadePaletes], simular)
watch(form, () => { if (tentouEnviar.value) erros.value = validarAgendamento(payload()) })

onMounted(async () => {
  try {
    const [c, p] = await Promise.all([api('/tipos-caminhao'), api('/tipos-paletizacao')])
    caminhoes.value = c.filter((x) => x.ativo)
    paletizacoes.value = p.filter((x) => x.ativo)
    api('/fornecedores', { params: { ativo: true } }).then((f) => (fornecedores.value = f)).catch(() => {})
    await simular()
  } catch (e) {
    notificar.erro(e)
  }
})

const erp = ref(null)
let ultimoPedido = null
async function buscarPedido() {
  const id = form.idPedido
  if (!id || id === ultimoPedido) return
  ultimoPedido = id
  try {
    const r = await api(`/erp/pedidos/${id}`)
    erp.value = r
    const p = r.pedido
    if (p.fornecedorNome) form.fornecedor = p.fornecedorNome.slice(0, 80)
    if (p.fornecedorEmail) form.emailFornecedor = p.fornecedorEmail.slice(0, 80)
    if (p.quantidadePaletesPrevista) form.quantidadePaletes = p.quantidadePaletesPrevista
    if (r.tipoPaletizacaoId && paletizacoes.value.some((x) => x.id === r.tipoPaletizacaoId)) form.tipoPaletizacaoId = r.tipoPaletizacaoId
    if (p.dataPrevistaEntrega && p.dataPrevistaEntrega >= hoje()) form.data = deIso(p.dataPrevistaEntrega)
    notificar.sucesso(`Dados do pedido ${id} preenchidos (${r.provedor === 'bluesoft' ? 'Bluesoft' : 'base local'})`, 'Pedido encontrado')
  } catch (e) {
    erp.value = null
    if (e.status !== 404) notificar.erro(e)
  }
}

function payload() {
  return {
    ...form,
    data: isoData(form.data),
    fornecedor: form.fornecedor.trim(),
    emailFornecedor: form.emailFornecedor.trim(),
    observacao: form.observacao?.trim() || null
  }
}

function salvar() {
  tentouEnviar.value = true
  const body = payload()
  erros.value = validarAgendamento(body)
  if (Object.keys(erros.value).length) {
    notificar.aviso('Revise os campos destacados', 'Campos obrigatórios')
    return
  }
  enviar(async () => {
    try {
      const r = await api('/agendamentos', { method: 'POST', body })
      notificar.sucesso(`Agendamento #${r.agendamento.id} criado. E-mail enviado para ${r.agendamento.emailFornecedor}.`, 'Agendamento confirmado')
      r.alertas.forEach((a) => notificar.aviso(a.mensagem, 'Capacidade excedida'))
      router.push(`/agendamentos/${r.agendamento.id}`)
    } catch (e) {
      notificar.erro(e)
    }
  })
}

function limpar() {
  Object.assign(form, vazio())
  erros.value = {}
  tentouEnviar.value = false
}
</script>

<template>
  <div class="cabecalho">
    <h1>Novo agendamento</h1>
    <div class="acoes"><Button label="Voltar" icon="pi pi-arrow-left" class="btn-secundario" @click="router.back()" /></div>
  </div>

  <form class="painel" novalidate @submit.prevent="salvar">
    <div class="grade">
      <SeletorCd v-model="form.cdId" input-id="cd" :todos="false" />
      <div class="campo">
        <label for="data">Data <span class="obrig">*</span></label>
        <DatePicker v-model="form.data" input-id="data" date-format="dd/mm/yy" :min-date="minData" show-icon icon-display="input" :invalid="!!erros.data" />
        <BarraOcupacao v-if="ocupacao" :percentual="ocupacao.percentual" :ocupado="ocupacao.paletesOcupados" :capacidade="ocupacao.capacidade" />
        <span v-if="erros.data" class="erro">{{ erros.data }}</span>
      </div>
      <div class="campo">
        <label for="faixa">Faixa horária <span class="obrig">*</span></label>
        <Select v-model="form.faixaHorariaId" input-id="faixa" :options="opcoesFaixa" option-label="label" option-value="value" :invalid="!!erros.faixaHorariaId">
          <template #option="{ option }">
            <div class="opcao-faixa">
              <span>{{ option.label }}</span>
              <span class="pct" :class="nivelOcupacao(option.percentual)">{{ fmtPct(option.percentual) }}</span>
              <span class="texto-suave">{{ option.detalhe }}</span>
            </div>
          </template>
        </Select>
        <BarraOcupacao v-if="faixaSel" :percentual="faixaSel.percentual" :ocupado="faixaSel.paletesOcupados" :capacidade="faixaSel.capacidade" />
        <span v-if="erros.faixaHorariaId" class="erro">{{ erros.faixaHorariaId }}</span>
      </div>
      <div class="campo">
        <label for="pedido">ID do pedido <span class="obrig">*</span></label>
        <InputNumber v-model="form.idPedido" input-id="pedido" :use-grouping="false" :min="1" :max="999999999999" :invalid="!!erros.idPedido" @blur="buscarPedido" @keydown.enter.prevent="buscarPedido" />
        <small v-if="erp" class="texto-suave">{{ erp.pedido.situacao || "Pedido" }} · {{ erp.provedor === "bluesoft" ? "Bluesoft" : "base local" }}<template v-if="erp.pedido.lojaDestino"> · {{ erp.pedido.lojaDestino }}</template></small>
        <span v-if="erros.idPedido" class="erro">{{ erros.idPedido }}</span>
      </div>
      <div class="campo">
        <label for="qtd">Quantidade de paletes <span class="obrig">*</span></label>
        <InputNumber v-model="form.quantidadePaletes" input-id="qtd" :min="1" :max="10000" :invalid="!!erros.quantidadePaletes" />
        <span v-if="paletizacaoSel && paletizacaoSel.fatorMultiplicador > 1 && form.quantidadePaletes" class="dica">
          {{ paletizacaoSel.nome }}: conta como {{ equivalentes }} paletes (×{{ paletizacaoSel.fatorMultiplicador }})
        </span>
        <span v-if="erros.quantidadePaletes" class="erro">{{ erros.quantidadePaletes }}</span>
      </div>
      <div v-if="fornecedores.length" class="campo c12">
        <label for="fcad">Fornecedor cadastrado</label>
        <Select v-model="form.fornecedorId" input-id="fcad" :options="fornecedores" :option-label="(f) => `${f.nomeFantasia || f.razaoSocial} · ${fmtCnpj(f.cnpj)}`" option-value="id"
                filter show-clear placeholder="Opcional: preenche nome e e-mail" @update:model-value="escolherFornecedor" />
      </div>
      <div class="campo c6">
        <label for="fornecedor">Fornecedor <span class="obrig">*</span></label>
        <InputText id="fornecedor" v-model="form.fornecedor" maxlength="80" :invalid="!!erros.fornecedor" />
        <span class="dica">{{ contador(form.fornecedor) }}</span>
        <span v-if="erros.fornecedor" class="erro">{{ erros.fornecedor }}</span>
      </div>
      <div class="campo c6">
        <label for="email">E-mail do fornecedor <span class="obrig">*</span></label>
        <InputText id="email" v-model="form.emailFornecedor" type="email" maxlength="80" autocomplete="off" :invalid="!!erros.emailFornecedor" />
        <span class="dica">{{ contador(form.emailFornecedor) }}</span>
        <span v-if="erros.emailFornecedor" class="erro">{{ erros.emailFornecedor }}</span>
      </div>
      <div class="campo">
        <label for="caminhao">Tipo de caminhão <span class="obrig">*</span></label>
        <Select v-model="form.tipoCaminhaoId" input-id="caminhao" :options="caminhoes" option-label="nome" option-value="id" :invalid="!!erros.tipoCaminhaoId" />
        <span v-if="erros.tipoCaminhaoId" class="erro">{{ erros.tipoCaminhaoId }}</span>
      </div>
      <div class="campo">
        <label for="paletizacao">Tipo de paletização <span class="obrig">*</span></label>
        <Select v-model="form.tipoPaletizacaoId" input-id="paletizacao" :options="paletizacoes" option-label="nome" option-value="id" :invalid="!!erros.tipoPaletizacaoId" />
        <span v-if="erros.tipoPaletizacaoId" class="erro">{{ erros.tipoPaletizacaoId }}</span>
      </div>
      <div class="campo c6">
        <label for="obs">Observação</label>
        <Textarea id="obs" v-model="form.observacao" rows="2" maxlength="500" auto-resize />
      </div>
    </div>

    <div v-if="simulacao && equivalentes && form.faixaHorariaId">
      <div v-if="simulacao.alertas.length" class="alerta aviso">
        <i class="pi pi-exclamation-triangle" />
        <div>
          <div v-for="a in simulacao.alertas" :key="a.tipo">{{ a.mensagem }}</div>
          <div>O agendamento pode ser confirmado mesmo assim.</div>
        </div>
      </div>
      <div v-else class="alerta ok">
        <i class="pi pi-check-circle" />
        <span>Após confirmar: dia {{ fmtData(isoData(form.data)) }} com {{ fmtPct(posDia) }} e faixa {{ faixaSel?.descricao }} com {{ fmtPct(posFaixa) }}.</span>
      </div>
    </div>

    <div class="rodape-form">
      <Button type="button" label="Limpar" class="btn-secundario" :disabled="enviando" @click="limpar" />
      <Button type="submit" label="Confirmar agendamento" icon="pi pi-check" class="btn-confirmar" :loading="enviando" :disabled="enviando" />
    </div>
  </form>
</template>

<style scoped>
.opcao-faixa { display: grid; grid-template-columns: 1fr auto auto; gap: 10px; width: 100%; font-size: 13px; }
</style>
