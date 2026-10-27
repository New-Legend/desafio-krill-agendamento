<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api'
import { useEnvio } from '../composables/useEnvio'
import { useNotificar } from '../composables/useNotificar'

const props = defineProps({ agendamentoId: { type: Number, required: true }, admin: { type: Boolean, default: false }, concluido: { type: Boolean, default: false } })
const notificar = useNotificar()
const { enviando, enviar } = useEnvio()
const notas = ref([])
const avisos = ref([])
const aberta = ref(null)
const arquivo = ref(null)
const kg = (v) => (v == null ? '—' : `${Number(v).toLocaleString('pt-BR', { maximumFractionDigits: 1 })} kg`)

async function carregar() {
  try {
    notas.value = await api(`/agendamentos/${props.agendamentoId}/notas`)
  } catch (e) {
    notificar.erro(e)
  }
}
function importar(ev) {
  const f = ev.target.files?.[0]
  if (!f) return
  if (f.size > 2 * 1024 * 1024) {
    notificar.aviso('XML acima de 2 MB')
    ev.target.value = ''
    return
  }
  enviar(async () => {
    try {
      const form = new FormData()
      form.append('arquivo', f)
      const r = await api(`/agendamentos/${props.agendamentoId}/notas`, { method: 'POST', form })
      avisos.value = r.avisos
      notificar.sucesso(`NF-e ${r.nota.numero} importada${r.produtosCriados ? ` · ${r.produtosCriados} produto(s) novo(s)` : ''}`)
      aberta.value = r.nota.id
      await carregar()
    } catch (e) {
      notificar.erro(e)
    } finally {
      ev.target.value = ''
    }
  })
}
function excluir(n) {
  enviar(async () => {
    try {
      await api(`/notas/${n.id}`, { method: 'DELETE' })
      notificar.sucesso('NF-e removida')
      await carregar()
    } catch (e) {
      notificar.erro(e)
    }
  })
}
onMounted(carregar)
defineExpose({ importar, excluir, carregar })
</script>

<template>
  <section class="painel nfe">
    <div class="painel-titulo">
      <h2>Notas fiscais (XML)</h2>
      <template v-if="admin">
        <input ref="arquivo" type="file" accept=".xml,text/xml,application/xml" hidden data-teste="arquivo" @change="importar" />
        <Button label="Importar XML" icon="pi pi-upload" size="small" class="btn-secundario" :loading="enviando" :disabled="enviando" @click="arquivo.click()" />
      </template>
    </div>
    <ul v-if="avisos.length" class="avisos"><li v-for="a in avisos" :key="a"><i class="pi pi-exclamation-triangle" /> {{ a }}</li></ul>
    <p v-if="!notas.length" class="texto-suave">Nenhuma NF-e importada.</p>
    <div v-for="n in notas" :key="n.id" class="nota">
      <button type="button" class="nota-cab" @click="aberta = aberta === n.id ? null : n.id">
        <strong>NF {{ n.numero }}/{{ n.serie }}</strong>
        <span>{{ n.emitenteNome }}</span>
        <span>{{ n.itens.length }} itens · {{ n.volumes ?? '—' }} vol.</span>
        <span>NF {{ kg(n.pesoBrutoKg) }} · calc. {{ kg(n.pesoCalculadoKg) }}</span>
        <Tag v-if="n.pesoDivergente" value="peso divergente" severity="warn" />
        <Tag v-if="n.itensSemPeso" :value="`${n.itensSemPeso} sem peso`" severity="secondary" />
      </button>
      <div v-if="aberta === n.id" class="nota-corpo">
        <small class="texto-suave chave">Chave {{ n.chave }}</small>
        <DataTable :value="n.itens" size="small" striped-rows>
          <Column field="numeroItem" header="#" class="num" />
          <Column field="codigo" header="Código" />
          <Column field="descricao" header="Descrição" />
          <Column header="Qtd." class="num"><template #body="{ data }">{{ Number(data.quantidade).toLocaleString('pt-BR') }} {{ data.unidade }}</template></Column>
          <Column header="Peso" class="num"><template #body="{ data }">{{ kg(data.pesoKg) }}</template></Column>
        </DataTable>
        <Button v-if="admin && !concluido" label="Remover NF-e" icon="pi pi-trash" size="small" text class="btn-excluir-texto" :disabled="enviando" @click="excluir(n)" />
      </div>
    </div>
  </section>
</template>

<style scoped>
.nfe { margin: 12px 0 0; background: var(--cor-fundo); }
.avisos { margin: 0 0 8px; padding-left: 0; list-style: none; font-size: 13px; color: #8a5a00; }
.nota { border: 1px solid var(--cor-linha, #e3e7ec); border-radius: 6px; background: #fff; margin-top: 6px; }
.nota-cab { all: unset; display: flex; flex-wrap: wrap; gap: 10px; align-items: center; padding: 8px 10px; cursor: pointer; font-size: 13px; width: calc(100% - 20px); }
.nota-corpo { padding: 0 10px 8px; }
.chave { display: block; margin-bottom: 6px; word-break: break-all; }
</style>
