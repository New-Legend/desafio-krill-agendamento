<script setup>
import { onMounted, reactive, ref } from 'vue'
import { api } from '../api'
import { fmtDataHora } from '../utils'
import { useNotificar } from '../composables/useNotificar'

const notificar = useNotificar()
const filtro = reactive({ texto: '', evento: null, dias: 7 })
const eventos = [
  { label: 'Entrou no sistema', value: 'ENTRADA' },
  { label: 'Saiu do sistema', value: 'SAIDA' },
  { label: 'Senha recusada', value: 'SENHA_RECUSADA' },
  { label: 'Trocou a senha', value: 'SENHA_TROCADA' }
]
const periodos = [1, 7, 30, 90].map((d) => ({ label: d === 1 ? 'Últimas 24 horas' : `Últimos ${d} dias`, value: d }))
const classe = { ENTRADA: 'ativo', SAIDA: 'inativo', SENHA_RECUSADA: 'erro', SENHA_TROCADA: 'aviso' }
const logs = ref([])
const limite = ref(200)
const carregando = ref(false)

async function buscar() {
  carregando.value = true
  try {
    logs.value = await api('/logs-acesso', { params: { texto: filtro.texto.trim(), evento: filtro.evento, dias: filtro.dias, limite: limite.value } })
  } catch (e) {
    notificar.erro(e)
  } finally {
    carregando.value = false
  }
}

function mais() {
  limite.value += 200
  buscar()
}

onMounted(buscar)
</script>

<template>
  <div class="cabecalho"><h1>Logs de acesso</h1></div>
  <form class="painel filtros" @submit.prevent="buscar">
    <div class="campo"><label for="lg-texto">Login ou IP</label><InputText id="lg-texto" v-model="filtro.texto" maxlength="80" /></div>
    <div class="campo"><label for="lg-ev">Evento</label><Select v-model="filtro.evento" input-id="lg-ev" :options="eventos" option-label="label" option-value="value" show-clear /></div>
    <div class="campo"><label for="lg-per">Período</label><Select v-model="filtro.dias" input-id="lg-per" :options="periodos" option-label="label" option-value="value" /></div>
    <Button type="submit" label="Filtrar" icon="pi pi-filter" class="btn-confirmar" :loading="carregando" />
  </form>
  <section class="painel">
    <div class="painel-titulo"><h2>Entradas, saídas e senhas recusadas</h2><span class="texto-suave">{{ logs.length }} registro(s)</span></div>
    <DataTable class="tabela-acesso" :value="logs" :loading="carregando" size="small" striped-rows data-key="id">
      <template #empty><div class="vazio">Nenhum acesso no período</div></template>
      <Column header="Data/hora"><template #body="{ data }">{{ fmtDataHora(data.criadoEm) }}</template></Column>
      <Column field="usuario" header="Login" />
      <Column header="Evento"><template #body="{ data }"><span class="pilula" :class="classe[data.evento]">{{ data.rotulo }}</span></template></Column>
      <Column field="ip" header="IP" />
      <Column header="Navegador"><template #body="{ data }"><span :title="data.userAgent">{{ data.navegador }}</span></template></Column>
    </DataTable>
    <div class="cartoes-acesso">
      <div v-if="!logs.length && !carregando" class="vazio">Nenhum acesso no período</div>
      <article v-for="l in logs" :key="l.id" class="cartao-acesso">
        <div class="topo-cartao"><strong>{{ l.usuario }}</strong><span class="pilula" :class="classe[l.evento]">{{ l.rotulo }}</span></div>
        <div class="linha-cartao"><i class="pi pi-clock" />{{ fmtDataHora(l.criadoEm) }}</div>
        <div class="linha-cartao"><i class="pi pi-globe" />{{ l.ip || '—' }} · {{ l.navegador }}</div>
      </article>
    </div>
    <div v-if="logs.length >= limite" class="rodape-form"><Button label="Carregar mais" class="btn-secundario" @click="mais" /></div>
  </section>
</template>
