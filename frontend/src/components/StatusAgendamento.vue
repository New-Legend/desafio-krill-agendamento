<script setup>
import { computed } from 'vue'

const props = defineProps({ status: { type: String, required: true } })
const MAPA = {
  AGENDADO: ['Agendado', 'info'],
  CONFIRMADO: ['Confirmado', 'success'],
  CHEGOU: ['Chegou', 'warn'],
  EM_RECEBIMENTO: ['Em recebimento', 'warn'],
  RECEBIDO: ['Recebido', 'success'],
  CANCELADO: ['Cancelado', 'danger'],
  NAO_COMPARECEU: ['Não compareceu', 'secondary']
}
const item = computed(() => MAPA[props.status] || [props.status, 'secondary'])
</script>

<template>
  <Tag :value="item[0]" :severity="item[1]" />
</template>
