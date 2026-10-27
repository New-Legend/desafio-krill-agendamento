<script setup>
import CadastroCrud from '../components/CadastroCrud.vue'

const campos = [
  { campo: 'nome', rotulo: 'Nome', tipo: 'texto', obrigatorio: true, max: 40 },
  { campo: 'descricao', rotulo: 'Descrição', tipo: 'texto', max: 120 },
  { campo: 'capacidadePaletes', rotulo: 'Capacidade (paletes)', tipo: 'numero', min: 1, maxNum: 1000 },
  { campo: 'ativo', rotulo: 'Situação', tipo: 'ativo' }
]
const novo = () => ({ nome: '', descricao: '', capacidadePaletes: null, ativo: true })
</script>

<template>
  <CadastroCrud titulo="Tipos de caminhão" recurso="/tipos-caminhao" :campos="campos" :novo-padrao="novo" rotulo-novo="Novo tipo de caminhão" />
</template>
