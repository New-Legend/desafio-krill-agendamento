<script setup>
import { computed } from 'vue'
import { diaSemana, fmtData } from '../utils'

/**
 * Grade de dias × faixas para remarcar (mesmo visual do painel de ocupação).
 * somenteComVaga: fornecedor só escolhe faixa com vaga; o CD vê tudo (excesso só alerta).
 */
const props = defineProps({
  dias: { type: Array, default: () => [] },
  paletes: { type: Number, required: true },
  atual: { type: Object, default: null },
  somenteComVaga: { type: Boolean, default: false }
})
const modelo = defineModel({ type: Object, default: null })

const linhas = computed(() =>
  props.dias.map((d) => ({
    data: d.data,
    faixas: d.faixas.map((f) => {
      const livreFaixa = f.capacidade - f.paletesOcupados
      const livreDia = d.capacidade - d.paletesOcupados
      const cabe = props.paletes <= livreFaixa && props.paletes <= livreDia
      const atual = props.atual && props.atual.data === d.data && props.atual.faixaHorariaId === f.faixaHorariaId
      return { ...f, livre: Math.max(0, Math.min(livreFaixa, livreDia)), cabe, atual, bloqueada: atual || (props.somenteComVaga && !cabe) }
    })
  }))
)
function escolher(d, f) {
  if (!f.bloqueada) modelo.value = { data: d.data, faixaHorariaId: f.faixaHorariaId, descricao: f.descricao, cabe: f.cabe }
}
const selecionada = (d, f) => modelo.value?.data === d.data && modelo.value?.faixaHorariaId === f.faixaHorariaId
</script>

<template>
  <div class="escolha">
    <div v-for="d in linhas" :key="d.data" class="dia">
      <span class="rotulo">{{ diaSemana(d.data) }} <strong>{{ fmtData(d.data).slice(0, 5) }}</strong></span>
      <button v-for="f in d.faixas" :key="f.faixaHorariaId" type="button" class="faixa"
              :class="{ cheia: !f.cabe, atual: f.atual, sel: selecionada(d, f) }" :disabled="f.bloqueada"
              :aria-label="`${fmtData(d.data)} ${f.descricao}`" @click="escolher(d, f)">
        <span>{{ f.descricao }}</span>
        <small>{{ f.atual ? 'atual' : f.cabe ? `${f.livre} livres` : 'sem vaga' }}</small>
      </button>
    </div>
  </div>
</template>

<style scoped>
.escolha { display: flex; flex-direction: column; gap: 6px; max-height: 360px; overflow: auto; }
.dia { display: grid; grid-template-columns: 90px repeat(4, 1fr); gap: 6px; align-items: stretch; }
.rotulo { font-size: 12px; display: flex; flex-direction: column; justify-content: center; text-transform: capitalize; }
.faixa { border: 1px solid #b9dcc6; background: #effaf3; border-radius: 6px; padding: 5px 4px; cursor: pointer; display: flex; flex-direction: column; font: inherit; font-size: 12px; }
.faixa small { color: var(--cor-texto-suave); }
.faixa.cheia { border-color: #f0c2c2; background: #fdf1f1; }
.faixa.atual { border-style: dashed; background: #f3f5f8; }
.faixa.sel { outline: 2px solid var(--cor-primaria, #0b4f8a); background: #e5effa; }
.faixa:disabled { cursor: not-allowed; opacity: 0.55; }
@media (max-width: 560px) { .dia { grid-template-columns: 1fr 1fr; } .rotulo { grid-column: span 2; flex-direction: row; gap: 4px; } }
</style>
