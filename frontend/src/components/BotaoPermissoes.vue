<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { pode, sessao } from '../sessao'
import { usePermissoes } from '../composables/usePermissoes'

/** Botão "Permissões" de cada tela: mostra os números que a tela usa e se o perfil logado os tem. */
const props = defineProps({ tela: { type: String, required: true } })
const router = useRouter()
const { catalogo } = usePermissoes()
const aberto = ref(false)
const daTela = computed(() => catalogo.value.filter((p) => p.telas.includes(props.tela)))

function editarPerfil() {
  aberto.value = false
  router.push({ name: 'perfis', query: { perfil: sessao.atual?.perfil } })
}
</script>

<template>
  <template v-if="daTela.length">
    <Button label="Permissões" icon="pi pi-user" size="small" class="btn-secundario botao-permissoes" @click="aberto = true" />
    <Dialog v-model:visible="aberto" modal header="Permissões desta tela" :style="{ width: '460px' }" :breakpoints="{ '560px': '96vw' }">
      <p class="texto-suave">Perfil atual: <strong>{{ sessao.atual?.perfil }}</strong></p>
      <ul class="lista-permissoes">
        <li v-for="p in daTela" :key="p.codigo" :class="{ negada: !pode(p.codigo) }">
          <span class="numero-permissao">{{ p.codigo }}</span>
          <span class="nome-permissao">{{ p.nome }}<small>{{ p.modulo }}</small></span>
          <Tag :value="pode(p.codigo) ? 'Liberada' : 'Sem acesso'" :severity="pode(p.codigo) ? 'success' : 'secondary'" />
        </li>
      </ul>
      <template #footer>
        <Button label="Fechar" class="btn-secundario" @click="aberto = false" />
        <Button v-if="pode(1003)" label="Editar perfis" icon="pi pi-key" class="btn-primario" @click="editarPerfil" />
      </template>
    </Dialog>
  </template>
</template>
