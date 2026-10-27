<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api'

/** Seletor de centro de distribuição (multi-CD). Fica oculto quando só existe o CD padrão. */
const modelo = defineModel({ type: Number, default: null })
const props = defineProps({ inputId: { type: String, default: 'cd' }, todos: { type: Boolean, default: true } })
const cds = ref([])
onMounted(async () => {
  try {
    cds.value = (await api('/cds')).filter((c) => c.ativo)
  } catch {
    cds.value = []
  }
})
</script>

<template>
  <div v-if="cds.length > 1" class="campo">
    <label :for="props.inputId">CD</label>
    <Select v-model="modelo" style="min-width: 210px" :input-id="props.inputId" :options="[...(props.todos ? [{ id: null, codigo: '', nome: 'Todos os CDs' }] : []), ...cds]"
            :option-label="(c) => (c.codigo ? `${c.codigo} · ${c.nome}` : c.nome)" option-value="id" :placeholder="props.todos ? 'Todos os CDs' : 'CD padrão'" />
  </div>
</template>
