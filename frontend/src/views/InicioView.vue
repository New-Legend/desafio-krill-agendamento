<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'
import { isAdmin } from '../sessao'
import { diaSemana, fmtData, fmtPct, hoje, nivelOcupacao } from '../utils'
import { useNotificar } from '../composables/useNotificar'
import BarraOcupacao from '../components/BarraOcupacao.vue'
import StatusAgendamento from '../components/StatusAgendamento.vue'

const router = useRouter()
const notificar = useNotificar()
const d = ref(null)
const carregando = ref(false)
let timer

const STATUS = [
  ['AGENDADO', 'Agendados'], ['CONFIRMADO', 'Confirmados'], ['CHEGOU', 'Na portaria'], ['EM_RECEBIMENTO', 'Em conferência'],
  ['RECEBIDO', 'Recebidos'], ['CANCELADO', 'Cancelados'], ['NAO_COMPARECEU', 'Não compareceram']
]
const saldo = computed(() => d.value ? d.value.ocupacao.capacidade - d.value.ocupacao.paletesOcupados : 0)

async function carregar() {
  carregando.value = true
  try {
    d.value = await api('/dashboard', { params: { data: hoje() } })
  } catch (e) {
    notificar.erro(e)
  } finally {
    carregando.value = false
  }
}
onMounted(() => {
  carregar()
  timer = setInterval(carregar, 60000)
})
onUnmounted(() => clearInterval(timer))
</script>

<template>
  <div class="cabecalho">
    <h1>Recebimento · {{ fmtData(hoje()) }}</h1>
    <div class="acoes">
      <Button icon="pi pi-refresh" class="btn-secundario" aria-label="Atualizar" :loading="carregando" @click="carregar" />
      <Button v-if="isAdmin" label="Novo agendamento" icon="pi pi-plus" class="btn-confirmar" @click="router.push('/agendamentos/novo')" />
    </div>
  </div>

  <template v-if="d">
    <div class="kpis">
      <div class="kpi kpi-destaque">
        <span>Ocupação de hoje</span>
        <strong class="pct" :class="nivelOcupacao(d.ocupacao.percentual)">{{ fmtPct(d.ocupacao.percentual) }}</strong>
        <BarraOcupacao :percentual="d.ocupacao.percentual" :mostrar-texto="false" />
        <small class="texto-suave">{{ d.ocupacao.paletesOcupados }} / {{ d.ocupacao.capacidade }} paletes</small>
      </div>
      <div class="kpi"><span>Agendamentos hoje</span><strong>{{ d.ocupacao.quantidadeAgendamentos }}</strong></div>
      <div class="kpi"><span>{{ saldo >= 0 ? 'Saldo de paletes' : 'Excedente' }}</span><strong :class="{ 'pct excedido': saldo < 0 }">{{ Math.abs(saldo) }}</strong></div>
      <div class="kpi"><span>Alertas</span><strong :class="{ 'pct excedido': d.alertas.some((a) => a.nivel === 'erro') }">{{ d.alertas.length }}</strong></div>
    </div>

    <div class="grade-home">
      <section class="painel">
        <div class="painel-titulo"><h2>Ocupação por faixa horária</h2><RouterLink to="/painel" class="texto-suave">semana</RouterLink></div>
        <div class="lista-faixas">
          <div v-for="f in d.ocupacao.faixas" :key="f.faixaHorariaId" class="linha-faixa">
            <strong>{{ f.descricao }}</strong>
            <BarraOcupacao :percentual="f.percentual" :mostrar-texto="false" />
            <span class="pct" :class="nivelOcupacao(f.percentual)">{{ fmtPct(f.percentual) }}</span>
            <span class="texto-suave num">{{ f.paletesOcupados }}/{{ f.capacidade }}</span>
          </div>
        </div>
      </section>

      <section class="painel">
        <h2>Status de hoje</h2>
        <div class="status-grade">
          <RouterLink v-for="[k, rotulo] in STATUS" :key="k" class="status-item" :to="{ path: '/agendamentos', query: { status: k } }">
            <strong>{{ d.porStatus[k] || 0 }}</strong><span>{{ rotulo }}</span>
          </RouterLink>
        </div>
      </section>

      <section class="painel">
        <h2>Próximos caminhões</h2>
        <ol v-if="d.proximos.length" class="timeline">
          <li v-for="a in d.proximos" :key="a.id" @click="router.push(`/agendamentos/${a.id}`)">
            <span class="hora">{{ a.data === d.data ? a.faixaHoraria.slice(0, 5) : `${diaSemana(a.data)} ${fmtData(a.data).slice(0, 5)}` }}</span>
            <div>
              <strong>{{ a.fornecedor }}</strong>
              <small class="texto-suave">Pedido {{ a.idPedido }} · {{ a.tipoCaminhao }} · {{ a.paletesEquivalentes }} paletes<template v-if="a.placa"> · {{ a.placa }}</template></small>
            </div>
            <StatusAgendamento :status="a.status" />
          </li>
        </ol>
        <div v-else class="vazio">Nenhum caminhão previsto</div>
      </section>

      <section class="painel">
        <h2>Alertas</h2>
        <ul v-if="d.alertas.length" class="alertas">
          <li v-for="(a, i) in d.alertas" :key="i" :class="a.nivel" @click="a.agendamentoId ? router.push(`/agendamentos/${a.agendamentoId}`) : router.push({ path: '/painel', query: { data: a.data } })">
            <i :class="a.nivel === 'info' ? 'pi pi-info-circle' : 'pi pi-exclamation-triangle'" />
            <span><b>{{ fmtData(a.data).slice(0, 5) }}</b> {{ a.mensagem }}</span>
          </li>
        </ul>
        <div v-else class="vazio">Sem alertas nos próximos 7 dias</div>
      </section>
    </div>

    <section class="painel">
      <h2>Próximos 7 dias</h2>
      <div class="dias">
        <button v-for="dia in d.proximosDias" :key="dia.data" class="dia" @click="router.push({ path: '/painel', query: { data: dia.data } })">
          <div class="cab"><span>{{ diaSemana(dia.data) }}</span><span>{{ fmtData(dia.data).slice(0, 5) }}</span></div>
          <span class="pct" :class="nivelOcupacao(dia.percentual)">{{ fmtPct(dia.percentual) }}</span>
          <BarraOcupacao :percentual="dia.percentual" :mostrar-texto="false" />
        </button>
      </div>
    </section>
  </template>
</template>

<style scoped>
.grade-home { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.grade-home .painel { margin-bottom: 0; }
.grade-home + .painel { margin-top: 12px; }
.kpi-destaque { display: flex; flex-direction: column; gap: 4px; }
.lista-faixas { display: flex; flex-direction: column; gap: 10px; }
.linha-faixa { display: grid; grid-template-columns: 110px 1fr 56px 64px; gap: 10px; align-items: center; font-size: 13px; }
.status-grade { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 8px; }
.status-item { display: flex; flex-direction: column; padding: 8px; border: 1px solid var(--cor-linha); border-radius: var(--raio); text-decoration: none; color: var(--cor-texto); }
.status-item strong { font-size: 18px; }
.status-item span { font-size: 12px; color: var(--cor-texto-suave); }
.timeline { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; }
.timeline li { display: grid; grid-template-columns: 76px 1fr auto; gap: 10px; align-items: center; padding: 7px 0; border-bottom: 1px solid var(--cor-linha); cursor: pointer; }
.timeline li:last-child { border-bottom: 0; }
.timeline .hora { font-weight: 700; font-variant-numeric: tabular-nums; color: var(--cor-primaria); font-size: 13px; }
.timeline div { display: flex; flex-direction: column; min-width: 0; }
.timeline strong { font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.alertas { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 6px; }
.alertas li { display: flex; gap: 8px; padding: 7px 9px; border-radius: var(--raio); font-size: 13px; cursor: pointer; }
.alertas li.erro { background: var(--gc-toast-error-bg); color: var(--gc-toast-error-text); border-left: 4px solid var(--gc-toast-error-accent); }
.alertas li.aviso { background: var(--gc-toast-warn-bg); color: var(--gc-toast-warn-text); border-left: 4px solid var(--gc-toast-warn-accent); }
.alertas li.info { background: var(--gc-toast-info-bg); color: var(--gc-toast-info-text); border-left: 4px solid var(--gc-toast-info-accent); }
@media (max-width: 900px) {
  .grade-home { grid-template-columns: 1fr; }
}
@media (max-width: 640px) {
  .status-grade { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .linha-faixa { grid-template-columns: 96px 1fr 50px; }
  .linha-faixa .num { display: none; }
}
</style>
