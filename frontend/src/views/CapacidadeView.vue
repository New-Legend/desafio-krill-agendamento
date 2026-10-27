<script setup>
import { onMounted, reactive } from 'vue'
import { api } from '../api'
import { useEnvio } from '../composables/useEnvio'
import { useNotificar } from '../composables/useNotificar'

const notificar = useNotificar()
const { enviando, enviar } = useEnvio()
const form = reactive({ capacidadeDiariaPaletes: null, quantidadeFaixas: null, duracaoFaixaMinutos: null })

onMounted(async () => {
  try {
    Object.assign(form, await api('/parametros/capacidade'))
  } catch (e) {
    notificar.erro(e)
  }
})

function salvar() {
  if (!form.capacidadeDiariaPaletes || !form.quantidadeFaixas || !form.duracaoFaixaMinutos) {
    notificar.aviso('Preencha todos os campos')
    return
  }
  enviar(async () => {
    try {
      Object.assign(form, await api('/parametros/capacidade', { method: 'PUT', body: { ...form } }))
      notificar.sucesso('Parâmetros de capacidade atualizados')
    } catch (e) {
      notificar.erro(e)
    }
  })
}
</script>

<template>
  <div class="cabecalho"><h1>Capacidade diária</h1></div>
  <form class="painel" @submit.prevent="salvar">
    <div class="grade">
      <div class="campo">
        <label for="cap">Capacidade por dia (paletes) <span class="obrig">*</span></label>
        <InputNumber v-model="form.capacidadeDiariaPaletes" input-id="cap" :min="1" :max="100000" />
        <span class="dica">Ao exceder, o sistema avisa mas não bloqueia</span>
      </div>
      <div class="campo">
        <label for="qf">Quantidade de faixas ativas <span class="obrig">*</span></label>
        <InputNumber v-model="form.quantidadeFaixas" input-id="qf" :min="1" :max="24" />
      </div>
      <div class="campo">
        <label for="df">Duração da faixa (minutos) <span class="obrig">*</span></label>
        <InputNumber v-model="form.duracaoFaixaMinutos" input-id="df" :min="15" :max="720" :step="15" />
      </div>
    </div>
    <div class="rodape-form">
      <Button type="submit" label="Salvar" icon="pi pi-check" class="btn-confirmar" :loading="enviando" :disabled="enviando" />
    </div>
  </form>
</template>
