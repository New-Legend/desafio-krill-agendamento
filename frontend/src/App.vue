<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { sessao, isAdmin, encerrarSessao } from './sessao'
import { ativarPush, desativarPush, inscricaoAtual, pushSuportado } from './push'
import { useNotificar } from './composables/useNotificar'
import { menuItens } from './menu'

const route = useRoute()
const router = useRouter()
const notificar = useNotificar()
const menuAberto = ref(false)
const online = ref(navigator.onLine)
const pushAtivo = ref(false)
const mostrarTopo = computed(() => !route.meta.publica)

const itens = computed(() => menuItens(isAdmin.value))
const itensPlanos = computed(() => itens.value.flatMap((g) => g.itens))

const atualizarRede = () => { online.value = navigator.onLine }
onMounted(async () => {
  window.addEventListener('online', atualizarRede)
  window.addEventListener('offline', atualizarRede)
  pushAtivo.value = !!(await inscricaoAtual().catch(() => null))
})
onUnmounted(() => {
  window.removeEventListener('online', atualizarRede)
  window.removeEventListener('offline', atualizarRede)
})

async function alternarPush() {
  try {
    if (pushAtivo.value) {
      await desativarPush()
      pushAtivo.value = false
      notificar.sucesso('Notificações desativadas neste navegador')
    } else {
      await ativarPush()
      pushAtivo.value = true
      notificar.sucesso('Você receberá avisos de novos agendamentos, cancelamentos e capacidade excedida', 'Notificações ativas')
    }
  } catch (e) {
    notificar.erro(e)
  }
}

function sair() {
  encerrarSessao()
  router.push({ name: 'login' })
}
</script>

<template>
  <Toast position="top-right" />
  <template v-if="mostrarTopo">
    <header class="topo">
      <button class="btn-topo btn-hamburguer" aria-label="Abrir menu" @click="menuAberto = true"><i class="pi pi-bars" /></button>
      <RouterLink to="/inicio" class="marca">
        <img src="/img/rede-krill-logo.png" alt="Rede Krill" width="570" height="528" />
        <span>Gestão CD <small>· Recebimento</small></span>
      </RouterLink>
      <nav class="menu">
        <RouterLink v-for="i in itens[0].itens" :key="i.to" :to="i.to">{{ i.label }}</RouterLink>
        <RouterLink to="/rotina">Rotina</RouterLink>
      </nav>
      <div class="espaco" />
      <div class="usuario">
        <button
          v-if="pushSuportado()"
          class="btn-topo"
          :class="{ ativo: pushAtivo }"
          :title="pushAtivo ? 'Desativar notificações' : 'Ativar notificações'"
          @click="alternarPush"
        >
          <i :class="pushAtivo ? 'pi pi-bell' : 'pi pi-bell-slash'" />
        </button>
        <span class="nome">{{ sessao.atual?.nome }}</span>
        <span class="perfil">{{ sessao.atual?.perfil === 'ADMIN' ? 'Administrador' : 'Usuário' }}</span>
        <button class="btn-topo" title="Sair" @click="sair"><i class="pi pi-sign-out" /></button>
      </div>
    </header>
    <div v-if="!online" class="offline">Sem conexão. Os dados serão atualizados quando a rede voltar.</div>
    <Drawer v-model:visible="menuAberto" header="Menu">
      <nav class="menu-lateral" @click="menuAberto = false">
        <template v-for="g in itens" :key="g.grupo">
          <div class="grupo">{{ g.grupo }}</div>
          <RouterLink v-for="i in g.itens" :key="i.to" :to="i.to"><i :class="i.icon" />{{ i.label }}</RouterLink>
        </template>
      </nav>
    </Drawer>
    <main class="conteudo">
      <RouterView />
    </main>
  </template>
  <RouterView v-else />
</template>
