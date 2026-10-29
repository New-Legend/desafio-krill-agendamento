<script setup>
import CadastroCrud from '../components/CadastroCrud.vue'
import { fmtMoeda } from '../utils'
import { useTiposPalete } from '../composables/useTiposPalete'

const tiposPalete = useTiposPalete()
const campos = [
  { campo: 'codigo', rotulo: 'Código', tipo: 'texto', obrigatorio: true, max: 20, dica: 'Letras, números ou _ (ex.: PBR). Não muda depois de criado.' },
  { campo: 'nome', rotulo: 'Nome', tipo: 'texto', obrigatorio: true, max: 40 },
  { campo: 'valorReposicao', rotulo: 'Valor de reposição', tipo: 'numero', min: 0, maxNum: 99999, decimais: 2, formatar: (r) => fmtMoeda(r.valorReposicao), dica: 'Sugestão de valor por palete na cobrança' },
  { campo: 'ordem', rotulo: 'Ordem', tipo: 'numero', min: 0, maxNum: 999 },
  { campo: 'ativo', rotulo: 'Situação', tipo: 'ativo', dica: 'Inativo some dos novos vales e romaneios; os antigos continuam mostrando o tipo' }
]
const novo = () => ({ codigo: '', nome: '', valorReposicao: 0, ordem: 10, ativo: true })
const validar = (f) => (/^[A-Za-z0-9_ ]{2,20}$/.test((f.codigo || '').trim()) ? null : { codigo: 'Use de 2 a 20 letras, números ou _' })
</script>

<template>
  <CadastroCrud titulo="Tipos de palete" recurso="/tipos-palete" :campos="campos" :novo-padrao="novo" :validar-extra="validar" rotulo-novo="Novo tipo de palete" @salvo="tiposPalete.carregar(true)" />
</template>
