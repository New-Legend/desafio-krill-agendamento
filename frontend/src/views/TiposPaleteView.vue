<script setup>
import { onMounted, reactive, ref } from 'vue'
import { api } from '../api'
import { isAdmin } from '../sessao'
import CadastroCrud from '../components/CadastroCrud.vue'
import { fmtMoeda } from '../utils'
import { useTiposPalete } from '../composables/useTiposPalete'
import { useEnvio } from '../composables/useEnvio'
import { useNotificar } from '../composables/useNotificar'

const tiposPalete = useTiposPalete()
const notificar = useNotificar()
const { enviando, enviar } = useEnvio()
const cds = ref([])
const campos = ref([
  { campo: 'codigo', rotulo: 'Código', tipo: 'texto', obrigatorio: true, max: 20, dica: 'Letras, números ou _ (ex.: PBR). Não muda depois de criado.' },
  { campo: 'nome', rotulo: 'Nome', tipo: 'texto', obrigatorio: true, max: 40 },
  { campo: 'cdId', rotulo: 'CD', tipo: 'select', opcoes: [], dica: 'Vazio = aceito em todos os CDs', formatar: (r) => (r.cdId ? cds.value.find((c) => c.id === r.cdId)?.nome || r.cdId : 'Todos os CDs') },
  { campo: 'valorReposicao', rotulo: 'Valor de reposição', tipo: 'numero', min: 0, maxNum: 99999, decimais: 2, formatar: (r) => fmtMoeda(r.valorReposicao), dica: 'Sugestão de valor por palete na cobrança' },
  { campo: 'ordem', rotulo: 'Ordem', tipo: 'numero', min: 0, maxNum: 999 },
  { campo: 'ativo', rotulo: 'Situação', tipo: 'ativo', dica: 'Inativo some dos novos vales e romaneios; os antigos continuam mostrando o tipo' }
])
const novo = () => ({ codigo: '', nome: '', cdId: null, valorReposicao: 0, ordem: 10, ativo: true })
const validar = (f) => (/^[A-Za-z0-9_ ]{2,20}$/.test((f.codigo || '').trim()) ? null : { codigo: 'Use de 2 a 20 letras, números ou _' })

const config = reactive({ exigirDuasAssinaturas: false, agrupamento: 'fornecedor' })
const AGRUPAMENTOS = [{ label: 'Fornecedor (empresa)', value: 'fornecedor' }, { label: 'Transportadora', value: 'transportadora' }]

function salvarConfig() {
  enviar(async () => {
    try {
      Object.assign(config, await api('/vales-palete/configuracao', { method: 'PUT', body: { ...config } }))
      notificar.sucesso(config.exigirDuasAssinaturas ? 'As duas assinaturas passam a ser obrigatórias para registrar a devolução' : 'Configuração salva')
    } catch (e) {
      notificar.erro(e)
    }
  })
}

onMounted(async () => {
  try {
    cds.value = await api('/cds')
    campos.value[2].opcoes = cds.value.map((c) => ({ label: `${c.codigo} · ${c.nome}`, value: c.id }))
  } catch {
    campos.value[2].opcoes = []
  }
  try {
    Object.assign(config, await api('/vales-palete/configuracao'))
  } catch (e) {
    notificar.erro(e)
  }
})
</script>

<template>
  <CadastroCrud titulo="Tipos de palete" recurso="/tipos-palete" :campos="campos" :novo-padrao="novo" :validar-extra="validar" rotulo-novo="Novo tipo de palete" @salvo="tiposPalete.carregar(true)" />

  <section class="painel config-vale">
    <h2>Configurações do vale-palete</h2>
    <form class="grade" novalidate @submit.prevent="salvarConfig">
      <div class="campo c6 linha-switch">
        <ToggleSwitch v-model="config.exigirDuasAssinaturas" input-id="cfg-ass" :disabled="!isAdmin" />
        <label for="cfg-ass">Exigir as duas assinaturas (motorista e conferente) antes de registrar a devolução</label>
      </div>
      <div class="campo c3">
        <label for="cfg-agr">Agrupar a conta-corrente por</label>
        <Select v-model="config.agrupamento" input-id="cfg-agr" :options="AGRUPAMENTOS" option-label="label" option-value="value" :disabled="!isAdmin" />
      </div>
      <div class="campo c3 fim"><Button type="submit" label="Salvar configurações" icon="pi pi-check" class="btn-confirmar" :loading="enviando" :disabled="!isAdmin" /></div>
    </form>
  </section>
</template>

<style scoped>
.config-vale { margin-top: 12px; }
.linha-switch { flex-direction: row; align-items: center; gap: 10px; }
.fim { justify-content: flex-end; }
</style>
