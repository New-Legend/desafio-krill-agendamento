<script setup>
import { computed } from 'vue'

const props = defineProps({ status: { type: String, required: true } })
const ROTULOS = {
  AGENDADO: 'Agendado', CONFIRMADO: 'Confirmado', CHEGOU: 'Chegou', EM_RECEBIMENTO: 'Em recebimento',
  RECEBIDO: 'Recebido', CANCELADO: 'Cancelado', NAO_COMPARECEU: 'Não compareceu'
}
const rotulo = computed(() => ROTULOS[props.status] || props.status)
const classe = computed(() => (ROTULOS[props.status] ? props.status.toLowerCase() : 'outro'))
</script>

<template>
  <span class="status" :class="classe">{{ rotulo }}</span>
</template>
