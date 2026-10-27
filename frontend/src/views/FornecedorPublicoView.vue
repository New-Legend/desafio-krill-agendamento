<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api'
import { diaSemana, fmtData } from '../utils'
import { useEnvio } from '../composables/useEnvio'
import { useNotificar } from '../composables/useNotificar'
import StatusAgendamento from '../components/StatusAgendamento.vue'

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

onMounted(carregar)
</script>

<template>
  <div class="publico">
    <header class="publico-topo">
      <img src="/icons/icon-192.png" alt="" />
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

      <form v-if="ag.editavel" class="painel" @submit.prevent="salvarVeiculo">
        <h2>Veículo</h2>
        <div class="form-grade">
          <div class="campo c4"><label for="pl">Placa</label><InputText id="pl" v-model="veiculo.placa" maxlength="8" autocomplete="off" /></div>
          <div class="campo c8"><label for="mo">Motorista</label><InputText id="mo" v-model="veiculo.motorista" maxlength="80" /></div>
        </div>
        <Button type="submit" label="Enviar dados do veículo" icon="pi pi-send" class="btn-confirmar largo" :loading="enviando" :disabled="enviando" />
      </form>
      <p class="texto-suave rodape">Chegue no início da faixa com a nota fiscal. Para cancelar ou remarcar, fale com o recebimento.</p>
    </template>
  </div>
</template>

<style scoped>
.publico { max-width: 560px; margin: 0 auto; padding: 16px 12px 32px; }
.publico-topo { display: flex; gap: 10px; align-items: center; margin-bottom: 14px; }
.publico-topo img { width: 40px; height: 40px; border-radius: 8px; }
.publico-topo div { display: flex; flex-direction: column; }
.publico-topo small { color: var(--cor-texto-suave); }
.destaque { display: flex; flex-direction: column; padding: 10px 12px; background: var(--cor-fundo); border-radius: var(--raio); margin-bottom: 10px; }
.destaque strong { font-size: 20px; }
.largo { width: 100%; margin-top: 12px; }
.rodape { font-size: 12px; text-align: center; }
</style>
