<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api'
import CadastroCrud from '../components/CadastroCrud.vue'

const cds = ref([])
const campos = ref([
  { campo: 'codigo', rotulo: 'Código', tipo: 'texto', obrigatorio: true, max: 10, dica: 'Ex.: D01. Único dentro do CD' },
  { campo: 'nome', rotulo: 'Nome', tipo: 'texto', obrigatorio: true, max: 40 },
  { campo: 'cdId', rotulo: 'CD', tipo: 'select', obrigatorio: true, opcoes: [], formatar: (r) => cds.value.find((c) => c.id === r.cdId)?.nome || r.cdId },
  { campo: 'ativa', rotulo: 'Situação', tipo: 'ativo', dica: 'Inativa não aparece no pátio nem na lotação do CD' }
])
const novo = () => ({ codigo: '', nome: '', cdId: cds.value[0]?.id ?? 1, ativa: true })

onMounted(async () => {
  try {
    cds.value = await api('/cds')
    campos.value[2].opcoes = cds.value.map((c) => ({ label: `${c.codigo} · ${c.nome}`, value: c.id }))
  } catch {
    campos.value[2].opcoes = [{ label: 'CD 1', value: 1 }]
  }
})
</script>

<template>
  <CadastroCrud titulo="Docas" recurso="/docas" :campos="campos" :novo-padrao="novo" rotulo-novo="Nova doca" />
</template>
