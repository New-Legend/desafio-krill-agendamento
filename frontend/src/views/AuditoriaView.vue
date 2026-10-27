<script setup>
import { onMounted, reactive, ref } from 'vue'
import { api } from '../api'
import { deIso, fmtDataHora, hoje, isoData, somarDias } from '../utils'
import { useNotificar } from '../composables/useNotificar'

const notificar = useNotificar()
const filtro = reactive({ inicio: deIso(somarDias(hoje(), -7)), fim: deIso(hoje()), usuario: '', acao: null, entidade: null })
const acoes = ['LOGIN', 'LOGIN_FALHA', 'CRIAR', 'ALTERAR', 'CANCELAR', 'EXPORTAR'].map((v) => ({ label: v, value: v }))
const entidades = ['SESSAO', 'AGENDAMENTO', 'FAIXA_HORARIA', 'TIPO_CAMINHAO', 'TIPO_PALETIZACAO', 'PARAMETRO_CAPACIDADE', 'RELATORIO'].map((v) => ({ label: v, value: v }))
const pagina = ref({ itens: [], total: 0 })
const primeiro = ref(0)
const tamanho = 50
const carregando = ref(false)
const expandidas = ref({})

async function buscar(p = 0) {
  carregando.value = true
  try {
    pagina.value = await api('/auditoria', {
      params: { inicio: isoData(filtro.inicio), fim: isoData(filtro.fim), usuario: filtro.usuario.trim(), acao: filtro.acao, entidade: filtro.entidade, pagina: p, tamanho }
    })
    primeiro.value = p * tamanho
  } catch (e) {
    notificar.erro(e)
  } finally {
    carregando.value = false
  }
}
onMounted(() => buscar())
</script>

<template>
  <div class="cabecalho"><h1>Auditoria e log de acesso</h1></div>
  <form class="painel filtros" @submit.prevent="buscar(0)">
    <div class="campo"><label for="ai">De</label><DatePicker v-model="filtro.inicio" input-id="ai" date-format="dd/mm/yy" show-icon icon-display="input" /></div>
    <div class="campo"><label for="af">Até</label><DatePicker v-model="filtro.fim" input-id="af" date-format="dd/mm/yy" show-icon icon-display="input" /></div>
    <div class="campo"><label for="au">Usuário</label><InputText id="au" v-model="filtro.usuario" /></div>
    <div class="campo"><label for="aa">Ação</label><Select v-model="filtro.acao" input-id="aa" :options="acoes" option-label="label" option-value="value" show-clear /></div>
    <div class="campo"><label for="ae">Entidade</label><Select v-model="filtro.entidade" input-id="ae" :options="entidades" option-label="label" option-value="value" show-clear /></div>
    <Button type="submit" label="Buscar" icon="pi pi-search" class="btn-confirmar" :loading="carregando" />
  </form>
  <section class="painel">
    <div class="painel-titulo"><h2>Registros</h2><span class="texto-suave">{{ pagina.total }} registro(s)</span></div>
    <DataTable v-model:expanded-rows="expandidas" :value="pagina.itens" :loading="carregando" size="small" striped-rows data-key="id">
      <template #empty><div class="vazio">Nenhum registro no período</div></template>
      <Column expander style="width: 36px" />
      <Column header="Data/hora"><template #body="{ data }">{{ fmtDataHora(data.dataHora) }}</template></Column>
      <Column field="usuario" header="Usuário" />
      <Column header="Ação"><template #body="{ data }"><Tag :value="data.acao" :severity="data.sucesso ? (data.acao === 'CANCELAR' ? 'warn' : 'info') : 'danger'" /></template></Column>
      <Column field="entidade" header="Entidade" />
      <Column field="entidadeId" header="ID" />
      <Column field="ip" header="IP" />
      <template #expansion="{ data }"><pre class="json">{{ JSON.stringify(data.detalhes, null, 2) }}</pre></template>
    </DataTable>
    <Paginator :rows="tamanho" :total-records="pagina.total" :first="primeiro" @page="(e) => buscar(e.page)" />
  </section>
</template>
