<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api'
import { diaSemana, fmtData } from '../utils'
import { useEnvio } from '../composables/useEnvio'
import { useNotificar } from '../composables/useNotificar'
import StatusAgendamento from '../components/StatusAgendamento.vue'
import EscolhaHorario from '../components/EscolhaHorario.vue'

const route = useRoute()
const notificar = useNotificar()
const { enviando, enviar } = useEnvio()
const ag = ref(null)
const erro = ref('')
const veiculo = reactive({ placa: '', motorista: '' })
const base = `/publico/fornecedor/${route.params.token}`

async function carregar() {
  try {
    ag.value = await api(base)
    veiculo.placa = ag.value.placa || ''
    veiculo.motorista = ag.value.motorista || ''
  } catch (e) {
    erro.value = e.status === 404 ? 'Link inválido ou expirado. Solicite um novo link ao recebimento.' : e.message
  }
}

function confirmar() {
  enviar(async () => {
    try {
      ag.value = await api(`${base}/confirmacao`, { method: 'POST' })
      notificar.sucesso('Presença confirmada. Obrigado!')
    } catch (e) {
      notificar.erro(e)
    }
  })
}

function salvarVeiculo() {
  enviar(async () => {
    try {
      ag.value = await api(`${base}/veiculo`, { method: 'PUT', body: { placa: veiculo.placa.trim() || null, motorista: veiculo.motorista.trim() || null } })
      veiculo.placa = ag.value.placa || ''
      notificar.sucesso('Dados do veículo enviados ao recebimento')
    } catch (e) {
      notificar.erro(e)
    }
  })
}

const remarcando = ref(false)
const dias = ref([])
const escolha = ref(null)

async function abrirRemarcar() {
  escolha.value = null
  try {
    dias.value = await api(`${base}/disponibilidade`)
    remarcando.value = true
  } catch (e) {
    notificar.erro(e)
  }
}

function remarcar() {
  if (!escolha.value) return notificar.aviso('Escolha o novo dia e faixa')
  enviar(async () => {
    try {
      ag.value = await api(`${base}/reagendamento`, { method: 'PUT', body: { data: escolha.value.data, faixaHorariaId: escolha.value.faixaHorariaId } })
      remarcando.value = false
      notificar.sucesso('Entrega remarcada. O recebimento foi avisado.')
    } catch (e) {
      notificar.erro(e)
      if (e.status === 422) abrirRemarcar()
    }
  })
}

onMounted(carregar)
defineExpose({ abrirRemarcar, remarcar, escolha })
</script>

<template>
  <div class="publico">
    <header class="publico-topo">
      <img src="/img/rede-krill-logo.png" alt="Rede Krill" width="570" height="528" />
      <div><strong>Rede Krill</strong><small>Agendamento de entrega</small></div>
    </header>

    <div v-if="erro" class="painel vazio">{{ erro }}</div>
    <template v-else-if="ag">
      <section class="painel">
        <div class="painel-titulo"><h2>Pedido {{ ag.idPedido }}</h2><StatusAgendamento :status="ag.status" /></div>
        <div class="destaque">
          <span>{{ diaSemana(ag.data) }}, {{ fmtData(ag.data) }}</span>
          <strong>{{ ag.faixaHoraria }}</strong>
        </div>
        <dl class="detalhes">
          <dt>Fornecedor</dt><dd>{{ ag.fornecedor }}</dd>
          <dt>Caminhão</dt><dd>{{ ag.tipoCaminhao }}</dd>
          <dt>Carga</dt><dd>{{ ag.quantidadePaletes }} paletes · {{ ag.tipoPaletizacao }}</dd>
        </dl>
        <Button v-if="ag.status === 'AGENDADO'" label="Confirmar presença" icon="pi pi-check" class="btn-confirmar largo" :loading="enviando" :disabled="enviando" @click="confirmar" />
      </section>

      <section v-if="ag.editavel" class="painel">
        <div class="painel-titulo"><h2>Remarcar entrega</h2><Button v-if="!remarcando" label="Escolher outra data" icon="pi pi-calendar" class="btn-secundario" size="small" @click="abrirRemarcar" /></div>
        <template v-if="remarcando">
          <p class="texto-suave">Somente faixas com vaga para {{ ag.paletesEquivalentes }} palete(s) podem ser escolhidas.</p>
          <EscolhaHorario v-model="escolha" :dias="dias" :paletes="ag.paletesEquivalentes" :atual="{ data: ag.data, faixaHorariaId: ag.faixaHorariaId }" somente-com-vaga />
          <p v-if="escolha" class="destaque-escolha">Nova entrega: <strong>{{ diaSemana(escolha.data) }}, {{ fmtData(escolha.data) }} · {{ escolha.descricao }}</strong></p>
          <div class="linha-botoes">
            <Button label="Voltar" class="btn-secundario" :disabled="enviando" @click="remarcando = false" />
            <Button label="Confirmar nova data" icon="pi pi-check" class="btn-confirmar" :loading="enviando" :disabled="enviando || !escolha" @click="remarcar" />
          </div>
        </template>
        <p v-else class="texto-suave">Não vai conseguir vir em {{ fmtData(ag.data) }}? Escolha outro dia e faixa com vaga.</p>
      </section>

      <form v-if="ag.editavel" class="painel" @submit.prevent="salvarVeiculo">
        <h2>Veículo</h2>
        <div class="form-grade">
          <div class="campo c4"><label for="pl">Placa</label><InputText id="pl" v-model="veiculo.placa" maxlength="8" autocomplete="off" /></div>
          <div class="campo c8"><label for="mo">Motorista</label><InputText id="mo" v-model="veiculo.motorista" maxlength="80" /></div>
        </div>
        <Button type="submit" label="Enviar dados do veículo" icon="pi pi-send" class="btn-confirmar largo" :loading="enviando" :disabled="enviando" />
      </form>
      <p class="texto-suave rodape">Chegue no início da faixa com a nota fiscal. Para cancelar, fale com o recebimento.</p>
    </template>
  </div>
</template>

<style scoped>
.publico { max-width: 560px; margin: 0 auto; padding: 16px 12px 32px; }
.publico-topo { display: flex; gap: 10px; align-items: center; margin-bottom: 14px; }
.publico-topo img { height: 56px; width: auto; aspect-ratio: 570 / 528; object-fit: contain; }
.publico-topo div { display: flex; flex-direction: column; }
.publico-topo small { color: var(--cor-texto-suave); }
.destaque { display: flex; flex-direction: column; padding: 10px 12px; background: var(--cor-fundo); border-radius: var(--raio); margin-bottom: 10px; }
.destaque strong { font-size: 20px; }
.largo { width: 100%; margin-top: 12px; }
.linha-botoes { display: flex; justify-content: flex-end; gap: 8px; margin-top: 10px; }
.destaque-escolha { margin: 10px 0 0; }
.rodape { font-size: 12px; text-align: center; }
</style>
