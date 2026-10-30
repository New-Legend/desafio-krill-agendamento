<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '../api'
import { pode } from '../sessao'
import { fmtData, isoData } from '../utils'
import { useEnvio } from '../composables/useEnvio'
import { useNotificar } from '../composables/useNotificar'
import { somar, useTiposPalete, zerar } from '../composables/useTiposPalete'

const notificar = useNotificar()
const { enviando, enviar } = useEnvio()
const busca = ref('')
const lojas = ref([])
const movimentos = ref([])
const visivel = ref(false)
const erros = ref({})
const TIPOS = [{ label: 'Envio para a loja', value: 'ENVIO' }, { label: 'Retorno da loja', value: 'RETORNO' }, { label: 'Saldo inicial', value: 'SALDO_INICIAL' }]
const ROTULO = { ENVIO: ['Envio', 'info'], RETORNO: ['Retorno', 'success'], SALDO_INICIAL: ['Saldo inicial', 'secondary'] }
const form = reactive({})
const tiposPalete = useTiposPalete()
const total = computed(() => somar(form.quantidades))
const colunas = computed(() => tiposPalete.colunas(...lojas.value.map((l) => l.porTipo)))

async function carregar() {
  try {
    const l = busca.value.trim() || null
    await tiposPalete.carregar()
    ;[lojas.value, movimentos.value] = await Promise.all([api('/paletes/lojas', { params: { loja: l } }), api('/paletes/movimentos', { params: { loja: l } })])
  } catch (e) {
    notificar.erro(e)
  }
}
function novo(loja = '') {
  Object.assign(form, { data: new Date(), tipo: 'ENVIO', loja, quantidades: zerar(tiposPalete.ativos.value.map((t) => t.codigo)), observacao: '' })
  erros.value = {}
  visivel.value = true
}
function salvar() {
  const e = {}
  if (!form.loja?.trim()) e.loja = 'Informe a loja'
  if (!total.value) e.total = 'Informe ao menos um palete'
  erros.value = e
  if (Object.keys(e).length) return
  enviar(async () => {
    try {
      const r = await api('/paletes/movimentos', { method: 'POST', body: { ...form, data: isoData(form.data), loja: form.loja.trim(), observacao: form.observacao.trim() || null } })
      notificar.sucesso(`Romaneio ${r.romaneio} registrado`)
      visivel.value = false
      carregar()
    } catch (err) {
      notificar.erro(err)
    }
  })
}
onMounted(carregar)
defineExpose({ novo, salvar, form, erros })
</script>

<template>
  <div class="cabecalho">
    <h1>Paletes nas lojas</h1>
    <div class="acoes">
      <form @submit.prevent="carregar" class="acoes"><InputText v-model="busca" placeholder="Loja" aria-label="Loja" /><Button type="submit" icon="pi pi-search" class="btn-secundario" aria-label="Buscar" /></form>
      <Button v-if="pode(1308)" label="Novo romaneio" icon="pi pi-plus" class="btn-confirmar" @click="novo()" />
    </div>
  </div>
  <section class="painel">
    <h2>Visão por loja (saldo em poder da loja)</h2>
    <DataTable :value="lojas" size="small" striped-rows>
      <template #empty><div class="vazio">Nenhum movimento com lojas</div></template>
      <Column field="loja" header="Loja" sortable />
      <Column v-for="c in colunas" :key="c" :header="tiposPalete.rotulo(c)" class="num"><template #body="{ data }">{{ data.porTipo[c] || 0 }}</template></Column>
      <Column header="Total" class="num" sortable sort-field="total"><template #body="{ data }"><strong>{{ data.total }}</strong></template></Column>
      <Column header="Último movimento"><template #body="{ data }">{{ fmtData(data.ultimoMovimento) }}</template></Column>
      <Column v-if="pode(1308)" header=""><template #body="{ data }"><Button label="Movimentar" icon="pi pi-arrow-right-arrow-left" size="small" class="btn-secundario" @click="novo(data.loja)" /></template></Column>
    </DataTable>
  </section>
  <section class="painel">
    <h2>Romaneios</h2>
    <DataTable :value="movimentos" size="small" striped-rows paginator :rows="25">
      <template #empty><div class="vazio">Nenhum romaneio</div></template>
      <Column field="romaneio" header="Romaneio" />
      <Column header="Data"><template #body="{ data }">{{ fmtData(data.data) }}</template></Column>
      <Column header="Tipo"><template #body="{ data }"><Tag :value="ROTULO[data.tipo][0]" :severity="ROTULO[data.tipo][1]" /></template></Column>
      <Column field="loja" header="Loja" />
      <Column header="Paletes por tipo"><template #body="{ data }">{{ Object.entries(data.quantidades).map(([k, n]) => `${tiposPalete.rotulo(k)} ${n}`).join(' · ') }}</template></Column>
      <Column field="total" header="Total" class="num" />
      <Column field="criadoPor" header="Por" />
    </DataTable>
  </section>

  <Dialog v-model:visible="visivel" modal header="Novo romaneio de paletes" :style="{ width: '560px' }" :breakpoints="{ '600px': '96vw' }">
    <form class="form-grade" @submit.prevent="salvar">
      <div class="campo c6"><label for="mt">Tipo</label><Select v-model="form.tipo" input-id="mt" :options="TIPOS" option-label="label" option-value="value" /></div>
      <div class="campo c6"><label for="md">Data</label><DatePicker v-model="form.data" input-id="md" date-format="dd/mm/yy" /></div>
      <div class="campo c12"><label for="ml">Loja <span class="obrig">*</span></label><InputText id="ml" v-model="form.loja" maxlength="80" :invalid="!!erros.loja" /><span v-if="erros.loja" class="erro">{{ erros.loja }}</span></div>
      <div v-for="t in tiposPalete.ativos.value" :key="t.codigo" class="campo c3"><label :for="`m${t.codigo}`">{{ t.nome }}</label><InputNumber v-model="form.quantidades[t.codigo]" :input-id="`m${t.codigo}`" :min="0" :max="10000" /></div>
      <span v-if="erros.total" class="erro c12">{{ erros.total }}</span>
      <div class="campo c12"><label for="mo">Observação</label><InputText id="mo" v-model="form.observacao" maxlength="255" /></div>
      <button type="submit" hidden />
    </form>
    <template #footer>
      <span class="texto-suave">{{ total }} palete(s)</span>
      <Button label="Cancelar" class="btn-secundario" :disabled="enviando" @click="visivel = false" />
      <Button label="Registrar" icon="pi pi-check" class="btn-confirmar" :loading="enviando" :disabled="enviando" @click="salvar" />
    </template>
  </Dialog>
</template>
