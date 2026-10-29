<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { api } from '../api'
import { deIso, diaSemana, fmtData, fmtMoeda, hoje, isoData } from '../utils'
import { isAdmin } from '../sessao'
import { useNotificar } from '../composables/useNotificar'
import { useEnvio } from '../composables/useEnvio'
import SeletorCd from '../components/SeletorCd.vue'
import BarraOcupacao from '../components/BarraOcupacao.vue'

const notificar = useNotificar()
const { enviando, enviar } = useEnvio()
const data = ref(deIso(hoje()))
const cdId = ref(null)
const quadro = ref(null)
let timer = null

const COLUNAS = [
  { chave: 'PREVISTO', titulo: 'Previstos', icone: 'pi pi-calendar' },
  { chave: 'AGUARDANDO', titulo: 'Aguardando doca', icone: 'pi pi-hourglass' },
  { chave: 'NA_DOCA', titulo: 'Na doca', icone: 'pi pi-sign-in' },
  { chave: 'DESCARREGANDO', titulo: 'Descarregando', icone: 'pi pi-box' },
  { chave: 'LIBERADO', titulo: 'Liberados', icone: 'pi pi-check-circle' }
]

const ind = computed(() => quadro.value?.indicador)
const docasLivres = computed(() => (quadro.value?.docas || []).filter((d) => !d.agendamentoId).map((d) => ({ label: d.doca.nome, value: d.doca.id })))
const hora = (s) => (s ? new Date(s).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' }) : '')

async function carregar() {
  try {
    quadro.value = await api('/patio', { params: { data: isoData(data.value), cdId: cdId.value } })
  } catch (e) {
    notificar.erro(e)
  }
}

function atribuir(cartao, docaId) {
  enviar(async () => {
    try {
      await api(`/agendamentos/${cartao.agendamentoId}/doca`, { method: 'PUT', body: { docaId } })
      notificar.sucesso(docaId ? 'Doca atribuída' : 'Doca liberada')
      await carregar()
    } catch (e) {
      notificar.erro(e)
    }
  })
}

watch([data, cdId], carregar)
onMounted(() => {
  carregar()
  timer = setInterval(carregar, 30000)
})
onUnmounted(() => clearInterval(timer))
</script>

<template>
  <div class="cabecalho">
    <h1>Pátio e docas</h1>
    <div class="acoes filtros-inline">
      <SeletorCd v-model="cdId" input-id="pcd" :todos="false" />
      <div class="campo"><label for="pd">Data</label><DatePicker v-model="data" input-id="pd" date-format="dd/mm/yy" show-icon icon-display="input" /></div>
      <Button label="Atualizar" icon="pi pi-refresh" class="btn-secundario" @click="carregar" />
    </div>
  </div>

  <template v-if="quadro">
    <p class="texto-suave">{{ diaSemana(quadro.data) }}, {{ fmtData(quadro.data) }} — atualiza sozinho a cada 30 segundos.</p>

    <div class="patio-indicadores">
      <section class="painel indicador" :class="ind.lotadoAgora ? 'cheio' : 'livre'">
        <span class="rotulo">Agora</span>
        <strong>{{ ind.lotadoAgora ? 'CD lotado' : 'Há docas livres' }}</strong>
        <span>{{ ind.docasOcupadas }} de {{ ind.docasAtivas }} docas ocupadas · {{ ind.caminhoesNoPatio }} caminhão(ões) no pátio</span>
      </section>
      <section class="painel indicador" :class="ind.diaCheio ? 'cheio' : 'livre'">
        <span class="rotulo">No dia</span>
        <strong>{{ ind.diaCheio ? 'Capacidade do dia atingida' : 'Ainda há capacidade no dia' }}</strong>
        <BarraOcupacao :percentual="Math.max(ind.dia.capacidadePaletes ? (ind.dia.paletes * 100) / ind.dia.capacidadePaletes : 0, ind.dia.capacidadeCaminhoes ? (ind.dia.caminhoes * 100) / ind.dia.capacidadeCaminhoes : 0)"
                       :ocupado="ind.dia.paletes" :capacidade="ind.dia.capacidadePaletes" />
        <span v-if="ind.dia.capacidadeCaminhoes">{{ ind.dia.caminhoes }} de {{ ind.dia.capacidadeCaminhoes }} caminhões</span>
      </section>
    </div>

    <section class="painel">
      <div class="painel-titulo"><h2>Docas</h2><span class="texto-suave">{{ ind.docasOcupadas }}/{{ ind.docasAtivas }} ocupada(s)</span></div>
      <div v-if="quadro.docas.length" class="docas">
        <div v-for="d in quadro.docas" :key="d.doca.id" class="doca" :class="{ ocupada: d.agendamentoId }">
          <strong>{{ d.doca.nome }}</strong>
          <span>{{ d.placa || (d.agendamentoId ? `#${d.agendamentoId}` : 'livre') }}</span>
          <small v-if="d.fornecedor">{{ d.fornecedor }}</small>
        </div>
      </div>
      <p v-else class="texto-suave">Nenhuma doca ativa neste CD. Cadastre em Cadastros → Docas.</p>
    </section>

    <div class="patio-colunas">
      <section v-for="c in COLUNAS" :key="c.chave" class="painel coluna">
        <div class="painel-titulo"><h2><i :class="c.icone" /> {{ c.titulo }}</h2><Tag :value="String(quadro.colunas[c.chave].length)" severity="secondary" /></div>
        <p v-if="!quadro.colunas[c.chave].length" class="texto-suave">Nenhum caminhão</p>
        <article v-for="k in quadro.colunas[c.chave]" :key="k.agendamentoId" class="cartao">
          <header>
            <router-link :to="`/agendamentos/${k.agendamentoId}`" class="placa">{{ k.placa || `#${k.agendamentoId}` }}</router-link>
            <span class="faixa">{{ k.faixa }}</span>
          </header>
          <div class="linha">{{ k.fornecedor }}</div>
          <div v-if="k.transportadora" class="linha suave"><i class="pi pi-truck" /> {{ k.transportadora }}</div>
          <div v-if="k.loja" class="linha suave"><i class="pi pi-shop" /> Entrega na {{ k.loja }}</div>
          <div class="linha suave">{{ k.tipoCaminhao }} · {{ k.paletes }} paletes<span v-if="k.chegadaEm"> · chegou {{ hora(k.chegadaEm) }}</span></div>
          <div v-if="k.taxaDescarga != null" class="linha suave">Descarga {{ k.modal }}: {{ fmtMoeda(k.taxaDescarga) }}</div>
          <div v-if="k.doca" class="linha"><Tag :value="k.doca" /></div>
          <div v-if="isAdmin && ['AGUARDANDO', 'NA_DOCA', 'DESCARREGANDO'].includes(c.chave)" class="acoes-cartao">
            <Select v-if="!k.docaId" :options="docasLivres" option-label="label" option-value="value" placeholder="Atribuir doca"
                    :disabled="enviando || !docasLivres.length" :aria-label="`Atribuir doca ao agendamento ${k.agendamentoId}`" @change="(e) => atribuir(k, e.value)" />
            <Button v-else label="Liberar doca" icon="pi pi-sign-out" class="btn-secundario" size="small" :disabled="enviando" @click="atribuir(k, null)" />
          </div>
        </article>
      </section>
    </div>
  </template>
</template>

<style scoped>
.patio-indicadores { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 12px; margin-bottom: 12px; }
.indicador { display: flex; flex-direction: column; gap: 6px; border-left: 4px solid var(--cor-ok); }
.indicador.cheio { border-left-color: var(--cor-excedido); }
.indicador .rotulo { font-size: 11px; text-transform: uppercase; letter-spacing: .06em; color: var(--gc-eyebrow); font-weight: 700; }
.indicador strong { font-size: 16px; color: var(--cor-titulo); }
.indicador.cheio strong { color: var(--cor-excedido); }
.docas { display: flex; flex-wrap: wrap; gap: 8px; }
.doca { display: flex; flex-direction: column; min-width: 120px; padding: 8px 10px; border: 1px solid var(--cor-linha); border-radius: var(--raio); background: var(--gc-navy-50); }
.doca.ocupada { background: var(--cor-topo); color: var(--gc-topbar-text); border-color: var(--cor-topo); }
.doca small { opacity: .85; }
.patio-colunas { display: grid; grid-template-columns: repeat(5, minmax(220px, 1fr)); gap: 12px; margin-top: 12px; overflow-x: auto; }
.coluna h2 i { color: var(--cor-primaria); }
.cartao { border: 1px solid var(--cor-linha); border-radius: var(--raio); padding: 8px 10px; margin-bottom: 8px; background: var(--cor-painel); display: flex; flex-direction: column; gap: 3px; font-size: 13px; }
.cartao header { display: flex; justify-content: space-between; align-items: center; gap: 6px; }
.cartao .placa { font-weight: 700; color: var(--cor-primaria); text-decoration: none; letter-spacing: .03em; }
.cartao .faixa { font-size: 12px; color: var(--cor-texto-suave); }
.cartao .suave { color: var(--cor-texto-suave); font-size: 12px; }
.acoes-cartao { margin-top: 4px; }
.acoes-cartao .p-select { width: 100%; }
@media (max-width: 900px) {
  .patio-colunas { grid-template-columns: 1fr; }
}
</style>
