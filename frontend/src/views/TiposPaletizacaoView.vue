<script setup>
import CadastroCrud from '../components/CadastroCrud.vue'

const campos = [
  { campo: 'nome', rotulo: 'Nome', tipo: 'texto', obrigatorio: true, max: 40 },
  { campo: 'fatorMultiplicador', rotulo: 'Fator na ocupação', tipo: 'numero', obrigatorio: true, min: 1, maxNum: 10, sufixo: '×', dica: 'Não paletizado = 2 (a quantidade de paletes conta em dobro)' },
  { campo: 'ativo', rotulo: 'Situação', tipo: 'ativo' }
]
const novo = () => ({ nome: '', fatorMultiplicador: 1, ativo: true })
</script>

<template>
  <CadastroCrud titulo="Tipos de paletização" recurso="/tipos-paletizacao" :campos="campos" :novo-padrao="novo" rotulo-novo="Novo tipo de paletização" />
</template>
