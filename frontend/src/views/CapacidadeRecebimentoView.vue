<script setup>
import { onMounted, ref, watch } from 'vue'
import { api } from '../api'
import { deIso, hoje, isoData } from '../utils'
import { useNotificar } from '../composables/useNotificar'
import CadastroCrud from '../components/CadastroCrud.vue'
import BarraOcupacao from '../components/BarraOcupacao.vue'

const DIAS = ['Segunda', 'Terça', 'Quarta', 'Quinta', 'Sexta', 'Sábado', 'Domingo']
const notificar = useNotificar()
const cds = ref([])
const campos = ref([
  { campo: 'cdId', rotulo: 'CD', tipo: 'select', opcoes: [], dica: 'Deixe vazio e informe a loja para entrega direta', formatar: (r) => (r.cdId ? cds.value.find((c) => c.id === r.cdId)?.nome || r.cdId : '—') },
  { campo: 'loja', rotulo: 'Loja (entrega direta)', tipo: 'texto', max: 80, formatar: (r) => r.loja || '—' },
  { campo: 'diaSemana', rotulo: 'Dia da semana', tipo: 'select', opcoes: DIAS.map((d, i) => ({ label: d, value: i + 1 })), dica: 'Vazio = todos os dias', formatar: (r) => (r.diaSemana ? DIAS[r.diaSemana - 1] : 'Todos os dias') },
  { campo: 'paletes', rotulo: 'Paletes por dia', tipo: 'numero', min: 1, maxNum: 100000 },
  { campo: 'caminhoes', rotulo: 'Caminhões por dia', tipo: 'numero', min: 1, maxNum: 10000 }
])
const novo = () => ({ cdId: cds.value[0]?.id ?? 1, loja: '', diaSemana: null, paletes: null, caminhoes: null })
const validar = (f) => {
  const e = {}
  if (!f.cdId && !(f.loja || '').trim()) e.loja = 'Informe o CD ou a loja'
  if (!f.paletes && !f.caminhoes) e.paletes = 'Informe paletes e/ou caminhões'
  return Object.keys(e).length ? e : null
}
const paraBody = (f) => ({ ...f, cdId: (f.loja || '').trim() ? null : f.cdId, loja: (f.loja || '').trim() || null })

const consulta = ref({ data: deIso(hoje()), cdId: null, loja: '' })
const ocupacao = ref(null)
async function consultar() {
  try {
    ocupacao.value = await api('/capacidades-recebimento/ocupacao', {
      params: { data: isoData(consulta.value.data), cdId: consulta.value.cdId, loja: consulta.value.loja.trim() }
    })
  } catch (e) {
    notificar.erro(e)
  }
}
watch(consulta, consultar, { deep: true })

onMounted(async () => {
  try {
    cds.value = await api('/cds')
    campos.value[0].opcoes = cds.value.map((c) => ({ label: `${c.codigo} · ${c.nome}`, value: c.id }))
  } catch {
    campos.value[0].opcoes = [{ label: 'CD 1', value: 1 }]
  }
  consultar()
})
</script>

<template>
  <CadastroCrud titulo="Capacidade de recebimento por dia" recurso="/capacidades-recebimento" :campos="campos" :novo-padrao="novo"
                :validar-extra="validar" :para-body="paraBody" rotulo-novo="Nova capacidade" @salvo="consultar">
    <template #rodape>
      <p class="texto-suave">
        Cada CD e cada loja (entregas diretas) podem ter limite de paletes e/ou caminhões por dia. A regra do dia da semana
        vale sobre a regra geral. CD sem regra usa a capacidade diária do cadastro de CDs; loja sem regra não tem limite.
      </p>
    </template>
  </CadastroCrud>

  <section class="painel consulta">
    <h2>Está cheio?</h2>
    <div class="filtros">
      <div class="campo"><label for="cq-data">Data</label><DatePicker v-model="consulta.data" input-id="cq-data" date-format="dd/mm/yy" show-icon icon-display="input" /></div>
      <div class="campo">
        <label for="cq-cd">CD</label>
        <Select v-model="consulta.cdId" input-id="cq-cd" :options="cds" :option-label="(c) => `${c.codigo} · ${c.nome}`" option-value="id" show-clear placeholder="CD padrão" />
      </div>
      <div class="campo"><label for="cq-loja">Loja</label><InputText id="cq-loja" v-model="consulta.loja" maxlength="80" /></div>
    </div>
    <div v-if="ocupacao" class="resultado">
      <strong :class="ocupacao.cheio ? 'texto-excedido' : 'texto-ok'">
        {{ ocupacao.cheio ? `Sim, ${ocupacao.destino} está cheio` : ocupacao.limitado ? `Não, ${ocupacao.destino} ainda tem vaga` : `Sem limite cadastrado para ${ocupacao.destino}` }}
      </strong>
      <BarraOcupacao v-if="ocupacao.capacidadePaletes" :percentual="ocupacao.percentualPaletes" :ocupado="ocupacao.paletes" :capacidade="ocupacao.capacidadePaletes" />
      <span v-if="ocupacao.capacidadeCaminhoes">{{ ocupacao.caminhoes }} de {{ ocupacao.capacidadeCaminhoes }} caminhões ({{ Math.round(ocupacao.percentualCaminhoes) }}%)</span>
      <span v-else>{{ ocupacao.caminhoes }} caminhão(ões) agendado(s)</span>
    </div>
  </section>
</template>

<style scoped>
.consulta { margin-top: 12px; }
.resultado { display: flex; flex-direction: column; gap: 6px; margin-top: 10px; max-width: 520px; }
.texto-excedido { color: var(--cor-excedido); }
.texto-ok { color: var(--cor-ok); }
</style>
