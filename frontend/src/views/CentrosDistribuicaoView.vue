<script setup>
import CadastroCrud from '../components/CadastroCrud.vue'

const campos = [
  { campo: 'codigo', rotulo: 'Código', tipo: 'texto', obrigatorio: true, max: 10 },
  { campo: 'nome', rotulo: 'Nome', tipo: 'texto', obrigatorio: true, max: 80 },
  { campo: 'cidade', rotulo: 'Cidade', tipo: 'texto', max: 80 },
  { campo: 'capacidadeDiariaPaletes', rotulo: 'Capacidade/dia', tipo: 'numero', min: 1, maxNum: 100000, sufixo: ' pal.', dica: 'Vazio = parâmetro geral (480)' },
  { campo: 'capacidadeFaixaPaletes', rotulo: 'Capacidade/faixa', tipo: 'numero', min: 1, maxNum: 100000, sufixo: ' pal.', dica: 'Vazio = capacidade de cada faixa' },
  { campo: 'ativo', rotulo: 'Situação', tipo: 'ativo' }
]
const novo = () => ({ codigo: '', nome: '', cidade: '', capacidadeDiariaPaletes: null, capacidadeFaixaPaletes: null, ativo: true })
</script>

<template>
  <CadastroCrud titulo="Centros de distribuição" recurso="/cds" :campos="campos" :novo-padrao="novo" rotulo-novo="Novo CD">
    <template #rodape><p class="texto-suave">O CD padrão (ID 1) segue as regras do desafio: 480 paletes/dia e 120 por faixa. Ele não pode ser inativado.</p></template>
  </CadastroCrud>
</template>
