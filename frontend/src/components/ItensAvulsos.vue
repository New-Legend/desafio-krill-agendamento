<script setup>
import { onMounted, reactive, ref } from 'vue'
import { api } from '../api'
import { fmtDataHora } from '../utils'
import { useEnvio } from '../composables/useEnvio'
import { useNotificar } from '../composables/useNotificar'

/** Produto avulso na conferência: item que chegou fora da NF-e (com opção de cadastrar no catálogo). */
const props = defineProps({ agendamentoId: { type: Number, required: true }, admin: { type: Boolean, default: false } })
const notificar = useNotificar()
const { enviando, enviar } = useEnvio()
const itens = ref([])
const aberto = ref(false)
const form = reactive({ codigo: '', descricao: '', quantidade: 1, unidade: 'UN', observacao: '', cadastrarProduto: true })

async function carregar() {
  try {
    itens.value = await api(`/agendamentos/${props.agendamentoId}/itens-avulsos`)
  } catch (e) {
    notificar.erro(e)
  }
}
function salvar() {
  if (!form.codigo.trim() || !form.descricao.trim() || !(form.quantidade > 0)) {
    notificar.aviso('Informe código, descrição e quantidade')
    return
  }
  enviar(async () => {
    try {
      await api(`/agendamentos/${props.agendamentoId}/itens-avulsos`, { method: 'POST', body: { ...form } })
      notificar.sucesso('Produto avulso lançado')
      Object.assign(form, { codigo: '', descricao: '', quantidade: 1, observacao: '' })
      aberto.value = false
      await carregar()
    } catch (e) {
      notificar.erro(e)
    }
  })
}
onMounted(carregar)
</script>

<template>
  <section class="painel">
    <div class="painel-titulo">
      <h2>Produtos avulsos</h2>
      <Button v-if="admin" :label="aberto ? 'Fechar' : 'Adicionar produto avulso'" :icon="aberto ? 'pi pi-times' : 'pi pi-plus'" class="btn-secundario" size="small" @click="aberto = !aberto" />
    </div>
    <form v-if="aberto" class="grade" novalidate @submit.prevent="salvar">
      <div class="campo c3"><label for="ia-cod">Código ou EAN <span class="obrig">*</span></label><InputText id="ia-cod" v-model="form.codigo" maxlength="60" /></div>
      <div class="campo c6"><label for="ia-desc">Descrição <span class="obrig">*</span></label><InputText id="ia-desc" v-model="form.descricao" maxlength="120" /></div>
      <div class="campo c3"><label for="ia-qtd">Quantidade <span class="obrig">*</span></label><InputNumber v-model="form.quantidade" input-id="ia-qtd" :min="0.001" :max-fraction-digits="3" /></div>
      <div class="campo c3"><label for="ia-un">Unidade</label><InputText id="ia-un" v-model="form.unidade" maxlength="6" /></div>
      <div class="campo c6"><label for="ia-obs">Observação</label><InputText id="ia-obs" v-model="form.observacao" maxlength="255" /></div>
      <div class="campo c3 checagem"><label for="ia-cad">Cadastrar no catálogo</label><ToggleSwitch v-model="form.cadastrarProduto" input-id="ia-cad" /></div>
      <div class="c12 rodape-form"><Button type="submit" label="Lançar" icon="pi pi-check" class="btn-confirmar" :loading="enviando" /></div>
    </form>
    <DataTable v-if="itens.length" :value="itens" size="small" data-key="id">
      <Column field="codigo" header="Código" />
      <Column field="descricao" header="Descrição" />
      <Column header="Qtd." class="num"><template #body="{ data }">{{ Number(data.quantidade).toLocaleString('pt-BR') }} {{ data.unidade || '' }}</template></Column>
      <Column header="Catálogo"><template #body="{ data }"><Tag :value="data.produtoId ? 'Cadastrado' : 'Sem cadastro'" :severity="data.produtoId ? 'success' : 'secondary'" /></template></Column>
      <Column header="Lançado"><template #body="{ data }">{{ data.criadoPor }} · {{ fmtDataHora(data.criadoEm) }}</template></Column>
    </DataTable>
    <p v-else class="texto-suave">Nenhum produto avulso. Use quando chegar item que não está na NF-e.</p>
  </section>
</template>

<style scoped>
.checagem { justify-content: flex-end; }
</style>
