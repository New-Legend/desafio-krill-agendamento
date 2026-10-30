<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '../api'
import { fmtDataHora } from '../utils'
import { pode, sessao } from '../sessao'
import { useEnvio } from '../composables/useEnvio'
import { useNotificar } from '../composables/useNotificar'

const notificar = useNotificar()
const { enviando, enviar } = useEnvio()
const usuarios = ref([])
const perfis = ref([])
const carregando = ref(false)
const filtro = reactive({ texto: '', perfilId: null, ativo: true })
const situacoes = [{ label: 'Ativos', value: true }, { label: 'Inativos', value: false }]
const editar = pode(1002)

const visivel = ref(false)
const form = reactive({})
const erros = ref({})
const senhaVisivel = ref(false)
const senha = reactive({ usuario: null, valor: '', erro: '' })
const menu = ref()
const menuAlvo = ref(null)

const opcoesPerfil = computed(() => perfis.value.filter((p) => p.ativo).map((p) => ({ label: p.nome, value: p.id })))
const filtrados = computed(() => {
  const t = filtro.texto.trim().toLowerCase()
  return usuarios.value.filter((u) => (!t || u.login.includes(t) || u.nome.toLowerCase().includes(t))
    && (!filtro.perfilId || u.perfilId === filtro.perfilId)
    && (filtro.ativo === null || u.ativo === filtro.ativo))
})
const itensMenu = computed(() => {
  const u = menuAlvo.value
  if (!u) return []
  const proprio = u.login === sessao.atual?.usuario
  return [
    { label: 'Editar', icon: 'pi pi-pencil', command: () => abrir(u) },
    { label: 'Redefinir senha', icon: 'pi pi-key', command: () => abrirSenha(u) },
    { label: u.ativo ? 'Inativar' : 'Reativar', icon: u.ativo ? 'pi pi-ban' : 'pi pi-check-circle', disabled: proprio, command: () => alternar(u) }
  ]
})

function ultimoAcesso(u) {
  return u.ultimoAcesso ? fmtDataHora(u.ultimoAcesso) : 'Nunca entrou'
}

async function carregar() {
  carregando.value = true
  try {
    ;[usuarios.value, perfis.value] = await Promise.all([api('/usuarios'), api('/perfis')])
  } catch (e) {
    notificar.erro(e)
  } finally {
    carregando.value = false
  }
}

function abrirMenu(ev, u) {
  menuAlvo.value = u
  menu.value.toggle(ev)
}

function abrir(u) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, u ? { ...u, senhaProvisoria: '' } : { id: null, login: '', nome: '', perfilId: null, ativo: true, senhaProvisoria: '' })
  erros.value = {}
  visivel.value = true
}

function validarSenha(v, login) {
  if (!v || v.length < 8) return 'Mínimo de 8 caracteres'
  if (!/[A-Za-z]/.test(v) || !/\d/.test(v)) return 'Use letras e números'
  if (login && v.toLowerCase() === login.toLowerCase()) return 'Não pode ser igual ao login'
  return null
}

function validar() {
  const e = {}
  if (!form.id && !/^[a-z0-9][a-z0-9._-]{2,59}$/.test((form.login || '').trim().toLowerCase())) e.login = 'De 3 a 60 caracteres: letras, números, ponto, hífen ou sublinhado'
  if (!(form.nome || '').trim()) e.nome = 'Informe o nome'
  if (!form.perfilId) e.perfilId = 'Escolha o perfil'
  if (!form.id) {
    const s = validarSenha(form.senhaProvisoria, form.login)
    if (s) e.senhaProvisoria = s
  }
  erros.value = e
  return !Object.keys(e).length
}

function salvar() {
  if (!validar()) return
  enviar(async () => {
    try {
      const body = { login: form.login.trim().toLowerCase(), nome: form.nome.trim(), perfilId: form.perfilId, ativo: form.ativo, senhaProvisoria: form.id ? null : form.senhaProvisoria }
      await api(form.id ? `/usuarios/${form.id}` : '/usuarios', { method: form.id ? 'PUT' : 'POST', body })
      notificar.sucesso(form.id ? 'Usuário atualizado' : 'Usuário incluído. A troca de senha será pedida no primeiro acesso.')
      visivel.value = false
      carregar()
    } catch (e) {
      notificar.erro(e)
    }
  })
}

function abrirSenha(u) {
  Object.assign(senha, { usuario: u, valor: '', erro: '' })
  senhaVisivel.value = true
}

function redefinir() {
  senha.erro = validarSenha(senha.valor, senha.usuario.login) || ''
  if (senha.erro) return
  enviar(async () => {
    try {
      await api(`/usuarios/${senha.usuario.id}/senha`, { method: 'POST', body: { senhaProvisoria: senha.valor } })
      notificar.sucesso(`${senha.usuario.nome} vai trocar a senha no próximo acesso`, 'Senha redefinida')
      senhaVisivel.value = false
      carregar()
    } catch (e) {
      notificar.erro(e)
    }
  })
}

function alternar(u) {
  enviar(async () => {
    try {
      await api(`/usuarios/${u.id}`, { method: 'PUT', body: { nome: u.nome, perfilId: u.perfilId, ativo: !u.ativo } })
      notificar.sucesso(u.ativo ? `${u.nome} não consegue mais entrar` : `${u.nome} reativado`)
      carregar()
    } catch (e) {
      notificar.erro(e)
    }
  })
}

onMounted(carregar)
</script>

<template>
  <div class="cabecalho">
    <h1>Usuários</h1>
    <div class="acoes"><Button v-if="editar" label="Novo usuário" icon="pi pi-plus" class="btn-confirmar" @click="abrir(null)" /></div>
  </div>
  <section class="painel filtros">
    <div class="campo"><label for="uf-texto">Nome ou login</label><InputText id="uf-texto" v-model="filtro.texto" /></div>
    <div class="campo"><label for="uf-perfil">Perfil</label><Select v-model="filtro.perfilId" input-id="uf-perfil" :options="perfis.map((p) => ({ label: p.nome, value: p.id }))" option-label="label" option-value="value" show-clear /></div>
    <div class="campo"><label for="uf-sit">Situação</label><Select v-model="filtro.ativo" input-id="uf-sit" :options="situacoes" option-label="label" option-value="value" show-clear /></div>
  </section>
  <section class="painel">
    <div class="painel-titulo"><h2>Usuários do sistema</h2><span class="texto-suave">{{ filtrados.length }} de {{ usuarios.length }}</span></div>
    <DataTable class="tabela-acesso" :value="filtrados" :loading="carregando" size="small" striped-rows data-key="id">
      <template #empty><div class="vazio">Nenhum usuário com esses filtros</div></template>
      <Column header="Nome"><template #body="{ data }"><strong>{{ data.nome }}</strong></template></Column>
      <Column field="login" header="Login" />
      <Column header="Perfil"><template #body="{ data }"><span class="pilula">{{ data.perfil }}</span></template></Column>
      <Column header="Situação">
        <template #body="{ data }">
          <span class="pilula" :class="data.ativo ? 'ativo' : 'inativo'">{{ data.ativo ? 'Ativo' : 'Inativo' }}</span>
          <span v-if="data.trocarSenha && data.ativo" class="pilula aviso">Troca de senha pendente</span>
        </template>
      </Column>
      <Column header="Último acesso"><template #body="{ data }">{{ ultimoAcesso(data) }}</template></Column>
      <Column v-if="editar" header="" style="width: 56px">
        <template #body="{ data }"><Button icon="pi pi-ellipsis-v" text rounded :aria-label="`Ações de ${data.nome}`" @click="abrirMenu($event, data)" /></template>
      </Column>
    </DataTable>
    <div class="cartoes-acesso">
      <div v-if="!filtrados.length && !carregando" class="vazio">Nenhum usuário com esses filtros</div>
      <article v-for="u in filtrados" :key="u.id" class="cartao-acesso">
        <div class="topo-cartao">
          <div><strong>{{ u.nome }}</strong><div class="texto-suave">{{ u.login }}</div></div>
          <Button v-if="editar" icon="pi pi-ellipsis-v" text rounded :aria-label="`Ações de ${u.nome}`" @click="abrirMenu($event, u)" />
        </div>
        <div class="linha-cartao">
          <span class="pilula">{{ u.perfil }}</span>
          <span class="pilula" :class="u.ativo ? 'ativo' : 'inativo'">{{ u.ativo ? 'Ativo' : 'Inativo' }}</span>
          <span v-if="u.trocarSenha && u.ativo" class="pilula aviso">Troca de senha pendente</span>
        </div>
        <div class="linha-cartao"><i class="pi pi-clock" />{{ ultimoAcesso(u) }}</div>
      </article>
    </div>
    <Menu ref="menu" :model="itensMenu" popup />
  </section>

  <Dialog v-model:visible="visivel" modal :header="form.id ? `Editar ${form.nome}` : 'Novo usuário'" :style="{ width: '520px' }" :breakpoints="{ '560px': '96vw' }">
    <form class="grade" novalidate @submit.prevent="salvar">
      <div class="campo c12"><label for="u-nome">Nome completo <span class="obrig">*</span></label><InputText id="u-nome" v-model="form.nome" maxlength="120" :invalid="!!erros.nome" /><span v-if="erros.nome" class="erro">{{ erros.nome }}</span></div>
      <div class="campo c6">
        <label for="u-login">Login <span class="obrig">*</span></label>
        <InputText id="u-login" v-model="form.login" maxlength="60" :disabled="!!form.id" autocomplete="off" :invalid="!!erros.login" />
        <span v-if="form.id" class="dica">O login não muda depois de criado</span>
        <span v-if="erros.login" class="erro">{{ erros.login }}</span>
      </div>
      <div class="campo c6"><label for="u-perfil">Perfil <span class="obrig">*</span></label><Select v-model="form.perfilId" input-id="u-perfil" :options="opcoesPerfil" option-label="label" option-value="value" :invalid="!!erros.perfilId" /><span v-if="erros.perfilId" class="erro">{{ erros.perfilId }}</span></div>
      <div v-if="!form.id" class="campo c12">
        <label for="u-senha">Senha provisória <span class="obrig">*</span></label>
        <InputText id="u-senha" v-model="form.senhaProvisoria" type="password" maxlength="72" autocomplete="new-password" :invalid="!!erros.senhaProvisoria" />
        <span class="dica">Mínimo de 8 caracteres com letras e números. O usuário troca no primeiro acesso.</span>
        <span v-if="erros.senhaProvisoria" class="erro">{{ erros.senhaProvisoria }}</span>
      </div>
      <div class="campo c6"><label for="u-ativo">Ativo</label><ToggleSwitch v-model="form.ativo" input-id="u-ativo" :disabled="form.login === sessao.atual?.usuario" /></div>
      <button type="submit" hidden />
    </form>
    <template #footer>
      <Button label="Cancelar" class="btn-secundario" :disabled="enviando" @click="visivel = false" />
      <Button label="Salvar" icon="pi pi-check" class="btn-confirmar" :loading="enviando" :disabled="enviando" @click="salvar" />
    </template>
  </Dialog>

  <Dialog v-model:visible="senhaVisivel" modal header="Redefinir senha" :style="{ width: '420px' }" :breakpoints="{ '460px': '96vw' }">
    <p>{{ senha.usuario?.nome }} ({{ senha.usuario?.login }}) vai entrar com a senha provisória e trocá-la em seguida.</p>
    <div class="campo">
      <label for="u-nova">Senha provisória <span class="obrig">*</span></label>
      <InputText id="u-nova" v-model="senha.valor" type="password" maxlength="72" autocomplete="new-password" :invalid="!!senha.erro" @keyup.enter="redefinir" />
      <span v-if="senha.erro" class="erro">{{ senha.erro }}</span>
    </div>
    <template #footer>
      <Button label="Cancelar" class="btn-secundario" :disabled="enviando" @click="senhaVisivel = false" />
      <Button label="Redefinir" icon="pi pi-key" class="btn-confirmar" :loading="enviando" :disabled="enviando" @click="redefinir" />
    </template>
  </Dialog>
</template>
