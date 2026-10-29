<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api'
import CadastroCrud from '../components/CadastroCrud.vue'

const cds = ref([])
const campos = ref([
  { campo: 'nome', rotulo: 'Nome completo', tipo: 'texto', obrigatorio: true, max: 80, dica: 'Aparece no campo Conferente ao iniciar a conferência deste CD' },
  { campo: 'cdId', rotulo: 'CD', tipo: 'select', obrigatorio: true, opcoes: [], formatar: (r) => cds.value.find((c) => c.id === r.cdId)?.nome || r.cdId },
  { campo: 'ativo', rotulo: 'Situação', tipo: 'ativo', dica: 'Pausado não aparece ao iniciar a conferência' }
])
const novo = () => ({ nome: '', cdId: cds.value[0]?.id ?? 1, ativo: true })
const validar = (f) => ((f.nome || '').trim().includes(' ') ? null : { nome: 'Informe nome e sobrenome' })

onMounted(async () => {
  try {
    cds.value = await api('/cds')
    campos.value[1].opcoes = cds.value.map((c) => ({ label: `${c.codigo} · ${c.nome}`, value: c.id }))
  } catch {
    campos.value[1].opcoes = [{ label: 'CD 1', value: 1 }]
  }
})
</script>

<template>
  <CadastroCrud titulo="Conferentes" recurso="/conferentes" :campos="campos" :novo-padrao="novo" :validar-extra="validar" rotulo-novo="Novo conferente" />
</template>
