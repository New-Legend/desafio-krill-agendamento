<script setup>
import { computed } from 'vue'
import { fmtPct, nivelOcupacao } from '../utils'

const props = defineProps({
  percentual: { type: Number, default: 0 },
  ocupado: { type: Number, default: null },
  capacidade: { type: Number, default: null },
  mostrarTexto: { type: Boolean, default: true }
})
const nivel = computed(() => nivelOcupacao(props.percentual))
const largura = computed(() => `${Math.min(100, Math.max(0, props.percentual))}%`)
</script>

<template>
  <div class="barra-ocupacao">
    <div v-if="mostrarTexto" class="ocupacao-inline">
      <span class="pct" :class="nivel">{{ fmtPct(percentual) }}</span>
      <span v-if="capacidade !== null">{{ ocupado }} / {{ capacidade }} paletes</span>
    </div>
    <div class="barra" :class="nivel" role="progressbar" :aria-valuenow="percentual" aria-valuemin="0" aria-valuemax="100">
      <span :style="{ width: largura }" />
    </div>
  </div>
</template>

<style scoped>
.barra-ocupacao { display: flex; flex-direction: column; gap: 4px; }
</style>
