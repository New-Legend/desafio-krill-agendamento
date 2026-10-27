<script setup>
import CadastroCrud from '../components/CadastroCrud.vue'
import { cnpjValido, fmtCnpj } from '../utils'

const campos = [
  { campo: 'cnpj', rotulo: 'CNPJ', tipo: 'texto', obrigatorio: true, max: 18, formatar: (r) => fmtCnpj(r.cnpj) },
  { campo: 'razaoSocial', rotulo: 'Razão social', tipo: 'texto', obrigatorio: true, max: 80 },
  { campo: 'nomeFantasia', rotulo: 'Nome fantasia', tipo: 'texto', max: 80, coluna: false },
  { campo: 'email', rotulo: 'E-mail de agendamento', tipo: 'texto', obrigatorio: true, max: 80 },
  { campo: 'telefone', rotulo: 'Telefone', tipo: 'texto', max: 20 },
  { campo: 'contato', rotulo: 'Contato', tipo: 'texto', max: 80, coluna: false },
  { campo: 'ativo', rotulo: 'Situação', tipo: 'ativo' }
]
const novo = () => ({ cnpj: '', razaoSocial: '', nomeFantasia: '', email: '', telefone: '', contato: '', ativo: true })
function validarExtra(f) {
  const e = {}
  if (f.cnpj && !cnpjValido(f.cnpj)) e.cnpj = 'CNPJ inválido'
  if (f.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(f.email.trim())) e.email = 'E-mail inválido'
  return Object.keys(e).length ? e : null
}
</script>

<template>
  <CadastroCrud titulo="Fornecedores" recurso="/fornecedores" :campos="campos" :novo-padrao="novo" :validar-extra="validarExtra" rotulo-novo="Novo fornecedor" />
</template>
