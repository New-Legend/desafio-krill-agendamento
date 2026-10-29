<script setup>
import { ref } from 'vue'
import { api } from '../api'
import { fmtDataHora } from '../utils'
import { useEnvio } from '../composables/useEnvio'
import { useNotificar } from '../composables/useNotificar'

/** Envio da cobrança (ou do recibo, se paga) por e-mail ao fornecedor, com pré-visualização e histórico. */
const props = defineProps({ cobrancaId: { type: Number, required: true }, admin: { type: Boolean, default: false } })
const notificar = useNotificar()
const { enviando, enviar } = useEnvio()
const visivel = ref(false)
const previa = ref(null)
const envios = ref([])
const para = ref('')
const erro = ref('')
const STATUS = { ENVIADO: ['Enviado', 'success'], FALHOU: ['Falhou', 'danger'], NAO_CONFIGURADO: ['Não configurado', 'secondary'], LINK_GERADO: ['Link gerado', 'info'] }
const EMAIL = /^[\w.+-]+@[\w-]+(\.[\w-]+)+$/

async function abrir() {
  try {
    ;[previa.value, envios.value] = await Promise.all([
      api(`/notificacoes/cobrancas/${props.cobrancaId}/email`),
      api(`/notificacoes/cobrancas/${props.cobrancaId}/envios`)
    ])
    para.value = previa.value.contato?.email || ''
    erro.value = ''
    visivel.value = true
  } catch (e) {
    notificar.erro(e)
  }
}

function enviarEmail() {
  erro.value = EMAIL.test(para.value.trim()) ? '' : 'Informe um e-mail válido'
  if (erro.value) return
  enviar(async () => {
    try {
      const r = await api(`/notificacoes/cobrancas/${props.cobrancaId}/email`, { method: 'POST', body: { para: para.value.trim() } })
      if (r.status === 'ENVIADO') notificar.sucesso(`E-mail enviado para ${r.destinatario}`)
      else notificar.aviso(r.erro || 'Não foi possível enviar', 'E-mail')
      envios.value = await api(`/notificacoes/cobrancas/${props.cobrancaId}/envios`)
    } catch (e) {
      notificar.erro(e)
    }
  })
}
defineExpose({ abrir, enviarEmail, para, erro })
</script>

<template>
  <Button label="E-mail" icon="pi pi-envelope" size="small" class="btn-secundario" @click="abrir" />
  <Dialog v-model:visible="visivel" modal :header="previa?.assunto" :style="{ width: '640px' }" :breakpoints="{ '680px': '96vw' }">
    <template v-if="previa">
      <div class="form-grade">
        <div class="campo c12"><label :for="`em-${cobrancaId}`">Para <span class="obrig">*</span></label><InputText :id="`em-${cobrancaId}`" v-model="para" maxlength="80" :invalid="!!erro" :disabled="!admin" /><span v-if="erro" class="erro">{{ erro }}</span></div>
      </div>
      <!-- pré-visualização gerada no servidor com os valores escapados -->
      <div class="previa-email" v-html="previa.html.replace('cid:logo', '/img/rede-krill-logo.png')" />
      <h3 class="titulo-envios">Envios</h3>
      <DataTable :value="envios" size="small">
        <template #empty><div class="vazio">Nenhum envio ainda</div></template>
        <Column header="Quando"><template #body="{ data }">{{ fmtDataHora(data.criadoEm) }}</template></Column>
        <Column field="canal" header="Canal" />
        <Column field="destinatario" header="Para" />
        <Column header="Situação"><template #body="{ data }"><Tag :value="STATUS[data.status][0]" :severity="STATUS[data.status][1]" /></template></Column>
        <Column field="usuario" header="Por" />
      </DataTable>
    </template>
    <template #footer>
      <Button label="Fechar" class="btn-secundario" @click="visivel = false" />
      <Button v-if="admin" label="Enviar e-mail" icon="pi pi-send" class="btn-confirmar" :loading="enviando" :disabled="enviando" @click="enviarEmail" />
    </template>
  </Dialog>
</template>

<style scoped>
.previa-email { border: 1px solid var(--gc-border); border-radius: var(--raio, 6px); padding: 10px; margin-top: 8px; max-height: 320px; overflow: auto; background: var(--gc-panel); }
.titulo-envios { font-size: 13px; margin: 12px 0 4px; }
</style>
