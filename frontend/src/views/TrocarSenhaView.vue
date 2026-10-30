<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'
import { atualizarSessao, sessao } from '../sessao'
import { useEnvio } from '../composables/useEnvio'
import { useNotificar } from '../composables/useNotificar'

const router = useRouter()
const notificar = useNotificar()
const { enviando, enviar } = useEnvio()
const form = reactive({ atual: '', nova: '', confirmacao: '' })
const erros = ref({})

function validar() {
  const e = {}
  if (!form.atual) e.atual = 'Informe a senha atual'
  if (form.nova.length < 8) e.nova = 'Mínimo de 8 caracteres'
  else if (!/[A-Za-z]/.test(form.nova) || !/\d/.test(form.nova)) e.nova = 'Use letras e números'
  else if (form.nova === form.atual) e.nova = 'A nova senha deve ser diferente da atual'
  if (form.confirmacao !== form.nova) e.confirmacao = 'As senhas não conferem'
  erros.value = e
  return !Object.keys(e).length
}

function salvar() {
  if (!validar()) return
  enviar(async () => {
    try {
      await api('/auth/senha', { method: 'POST', body: { senhaAtual: form.atual, novaSenha: form.nova } })
      atualizarSessao(await api('/auth/eu'))
      notificar.sucesso('Use a nova senha nos próximos acessos', 'Senha alterada')
      router.replace('/inicio')
    } catch (e) {
      notificar.erro(e)
    }
  })
}
</script>

<template>
  <section class="painel troca-senha">
    <h1>Trocar senha</h1>
    <p v-if="sessao.atual?.trocarSenha" class="texto-suave">Você entrou com uma senha provisória. Defina a sua senha para continuar.</p>
    <form class="grade" novalidate @submit.prevent="salvar">
      <div class="campo c12"><label for="ts-atual">Senha atual</label><InputText id="ts-atual" v-model="form.atual" type="password" autocomplete="current-password" :invalid="!!erros.atual" /><span v-if="erros.atual" class="erro">{{ erros.atual }}</span></div>
      <div class="campo c12"><label for="ts-nova">Nova senha</label><InputText id="ts-nova" v-model="form.nova" type="password" maxlength="72" autocomplete="new-password" :invalid="!!erros.nova" /><span class="dica">Mínimo de 8 caracteres com letras e números</span><span v-if="erros.nova" class="erro">{{ erros.nova }}</span></div>
      <div class="campo c12"><label for="ts-conf">Repita a nova senha</label><InputText id="ts-conf" v-model="form.confirmacao" type="password" maxlength="72" autocomplete="new-password" :invalid="!!erros.confirmacao" /><span v-if="erros.confirmacao" class="erro">{{ erros.confirmacao }}</span></div>
      <div class="c12 rodape-form"><Button type="submit" label="Salvar nova senha" icon="pi pi-lock" class="btn-confirmar" :loading="enviando" :disabled="enviando" /></div>
    </form>
  </section>
</template>
