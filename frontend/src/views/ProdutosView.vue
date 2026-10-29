<script setup>
import { ref } from 'vue'
import { api } from '../api'
import { baixarArquivo } from '../utils'
import { useNotificar } from '../composables/useNotificar'
import CadastroCrud from '../components/CadastroCrud.vue'

const notificar = useNotificar()
async function exportar() {
  try {
    baixarArquivo(await api('/produtos/exportar.csv', { blob: true }), 'catalogo_produtos.csv')
  } catch (e) {
    notificar.erro(e)
  }
}

const semPeso = ref(false)
const crud = ref(null)
const campos = [
  { campo: 'codigo', rotulo: 'Código', tipo: 'texto', obrigatorio: true, max: 60 },
  { campo: 'ean', rotulo: 'EAN', tipo: 'texto', max: 14 },
  { campo: 'descricao', rotulo: 'Descrição', tipo: 'texto', obrigatorio: true, max: 120 },
  { campo: 'unidade', rotulo: 'Unid.', tipo: 'texto', max: 6 },
  { campo: 'pesoBrutoKg', rotulo: 'Peso bruto (kg)', tipo: 'numero', min: 0, maxNum: 100000, formatar: (r) => (r.pesoBrutoKg == null ? 'sem peso' : `${Number(r.pesoBrutoKg).toLocaleString('pt-BR')} kg`) },
  { campo: 'itensPorPalete', rotulo: 'Itens/palete', tipo: 'numero', min: 1, maxNum: 100000 },
  { campo: 'ativo', rotulo: 'Situação', tipo: 'ativo' }
]
const novo = () => ({ codigo: '', ean: '', descricao: '', unidade: 'UN', pesoBrutoKg: null, itensPorPalete: null, ativo: true })
function alternar() {
  semPeso.value = !semPeso.value
  setTimeout(() => crud.value?.carregar())
}
</script>

<template>
  <CadastroCrud ref="crud" titulo="Produtos e pesos" recurso="/produtos" :params="{ semPeso: semPeso || null }" :campos="campos" :novo-padrao="novo" rotulo-novo="Novo produto">
    <template #rodape>
      <div class="acoes-rodape">
        <Button :label="semPeso ? 'Mostrar todos' : 'Somente sem peso'" icon="pi pi-filter" class="btn-secundario" @click="alternar" />
        <Button label="Exportar catálogo (CSV)" icon="pi pi-download" class="btn-secundario" @click="exportar" />
        <span class="texto-suave">Produtos novos entram automaticamente ao importar o XML da NF-e. Informe o peso para o cálculo de carga.</span>
      </div>
    </template>
  </CadastroCrud>
</template>

<style scoped>
.acoes-rodape { display: flex; gap: 12px; align-items: center; margin-top: 10px; flex-wrap: wrap; }
</style>
