<script setup>
import { ref } from 'vue'
import { api } from '../api'
import { useEnvio } from '../composables/useEnvio'
import { useNotificar } from '../composables/useNotificar'

/**
 * Aviso ao fornecedor por WhatsApp. Padrão: link wa.me com a mensagem pronta (o usuário confirma o envio
 * no próprio WhatsApp). Com a API do WhatsApp Business configurada no servidor, envia direto.
 */
const props = defineProps({
  documento: { type: String, required: true }, // 'cobrancas' | 'agendamentos'
  id: { type: Number, required: true },
  admin: { type: Boolean, default: false },
  rotulo: { type: String, default: 'WhatsApp' }
})
const notificar = useNotificar()
const { enviando, enviar } = useEnvio()
const visivel = ref(false)
const dados = ref(null)
const telefone = ref('')

async function abrir(tel) {
  try {
    dados.value = await api(`/notificacoes/${props.documento}/${props.id}/whatsapp`, { params: { telefone: tel || null } })
    if (tel === undefined) telefone.value = dados.value.telefone || ''
    visivel.value = true
  } catch (e) {
    notificar.erro(e)
  }
}

function registrar() {
  if (!props.admin) return
  enviar(async () => {
    try {
      const r = await api(`/notificacoes/${props.documento}/${props.id}/whatsapp`, { method: 'POST', body: { telefone: telefone.value.trim() || null } })
      if (r.status === 'ENVIADO') notificar.sucesso('Mensagem enviada pelo WhatsApp')
      else if (r.status === 'FALHOU') notificar.aviso(r.erro || 'Falha no envio', 'WhatsApp')
    } catch (e) {
      notificar.erro(e)
    }
  })
}
defineExpose({ abrir, registrar, dados, telefone })
</script>

<template>
  <Button :label="rotulo" icon="pi pi-whatsapp" size="small" class="btn-secundario" @click="abrir()" />
  <Dialog v-model:visible="visivel" modal header="Aviso por WhatsApp" :style="{ width: '520px' }" :breakpoints="{ '560px': '96vw' }">
    <template v-if="dados">
      <div class="form-grade">
        <div class="campo c8"><label :for="`wtel-${id}`">Telefone com DDD</label><InputText :id="`wtel-${id}`" v-model="telefone" maxlength="20" @change="abrir(telefone)" /></div>
        <div class="campo c4"><label>Canal</label><span class="texto-suave">{{ dados.apiHabilitada ? `API (${dados.provedor})` : 'Link wa.me' }}</span></div>
        <div class="campo c12"><label>Mensagem</label><p class="mensagem-whats">{{ dados.mensagem }}</p></div>
      </div>
      <small v-if="!dados.telefone" class="texto-suave">Sem telefone válido: o WhatsApp abre para você escolher o contato.</small>
    </template>
    <template #footer>
      <Button label="Fechar" class="btn-secundario" @click="visivel = false" />
      <Button v-if="dados?.apiHabilitada && admin" label="Enviar pela API" icon="pi pi-send" class="btn-confirmar" :loading="enviando" :disabled="enviando || !dados.telefone" @click="registrar" />
      <a v-else-if="dados" class="p-button btn-confirmar link-whats" :href="dados.link" target="_blank" rel="noopener noreferrer" @click="registrar"><i class="pi pi-whatsapp" /> Abrir no WhatsApp</a>
    </template>
  </Dialog>
</template>

<style scoped>
.mensagem-whats { margin: 0; padding: 8px 10px; background: var(--gc-zebra); border: 1px solid var(--gc-border); border-radius: var(--raio, 6px); white-space: pre-wrap; font-size: 13px; }
.link-whats { display: inline-flex; align-items: center; gap: 6px; text-decoration: none; }
</style>
