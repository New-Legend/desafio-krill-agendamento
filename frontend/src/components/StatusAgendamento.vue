<script setup>
defineProps({ status: { type: String, required: true } })
</script>

<template>
  <Tag :value="status === 'AGENDADO' ? 'Agendado' : 'Cancelado'" :severity="status === 'AGENDADO' ? 'success' : 'danger'" />
</template>
