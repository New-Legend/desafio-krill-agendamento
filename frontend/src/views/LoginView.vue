<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { iniciarSessao } from '../sessao'
import { useEnvio } from '../composables/useEnvio'

const router = useRouter()
const route = useRoute()
const form = reactive({ usuario: '', senha: '' })
const erro = ref('')
const { enviando, enviar } = useEnvio()

function entrar() {
  erro.value = ''
  if (!form.usuario.trim() || !form.senha) {
    erro.value = 'Informe usuário e senha'
    return
  }
  enviar(async () => {
    try {
      iniciarSessao(await api('/auth/login', { method: 'POST', body: { usuario: form.usuario.trim(), senha: form.senha } }))
      router.replace(route.query.voltar || '/painel')
    } catch (e) {
      erro.value = e.message
    }
  })
}
</script>

<template>
  <div class="login">
    <form class="login-cartao" @submit.prevent="entrar">
      <img src="/img/rede-krill-logo.png" alt="Rede Krill Supermercados" width="570" height="528" />
      <h1>Agendamento Rede Krill</h1>
      <p>Recebimento do Centro de Distribuição</p>
      <div class="campo">
        <label for="usuario">Usuário</label>
        <InputText id="usuario" v-model="form.usuario" autocomplete="username" autofocus />
      </div>
      <div class="campo">
        <label for="senha">Senha</label>
        <InputText id="senha" v-model="form.senha" type="password" autocomplete="current-password" />
      </div>
      <div v-if="erro" class="alerta aviso"><i class="pi pi-exclamation-triangle" />{{ erro }}</div>
      <div class="rodape-form">
        <Button type="submit" label="Entrar" icon="pi pi-sign-in" class="btn-confirmar" :loading="enviando" :disabled="enviando" />
      </div>
    </form>
  </div>
</template>
