<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api'
import { atualizarSessao } from '../sessao'
import { porModulo, usePermissoes } from '../composables/usePermissoes'
import { useEnvio } from '../composables/useEnvio'
import { useNotificar } from '../composables/useNotificar'

const route = useRoute()
const notificar = useNotificar()
const { enviando, enviar } = useEnvio()
const { catalogo } = usePermissoes()
const perfis = ref([])
const form = reactive({ id: null, nome: '', descricao: '', ativo: true, permissoes: [] })
const erro = ref('')
const modulos = computed(() => porModulo(catalogo.value))

function selecionar(p) {
  Object.assign(form, p
    ? { id: p.id, nome: p.nome, descricao: p.descricao || '', ativo: p.ativo, permissoes: [...p.permissoes].sort() }
    : { id: null, nome: '', descricao: '', ativo: true, permissoes: [] })
  erro.value = ''
}

async function carregar(manterId) {
  try {
    perfis.value = await api('/perfis')
    const alvo = perfis.value.find((p) => p.id === manterId) || perfis.value.find((p) => p.nome === route.query.perfil) || perfis.value[0]
    selecionar(alvo)
  } catch (e) {
    notificar.erro(e)
  }
}

function moduloMarcado(m) {
  return m.itens.every((p) => form.permissoes.includes(p.codigo))
}

function alternarModulo(m) {
  const codigos = m.itens.map((p) => p.codigo)
  form.permissoes = moduloMarcado(m)
    ? form.permissoes.filter((c) => !codigos.includes(c))
    : [...new Set([...form.permissoes, ...codigos])]
}

function salvar() {
  erro.value = form.nome.trim().length < 3 ? 'O nome do perfil precisa de ao menos 3 caracteres' : ''
  if (erro.value) return
  enviar(async () => {
    try {
      const body = { nome: form.nome.trim(), descricao: form.descricao.trim() || null, ativo: form.ativo, permissoes: form.permissoes }
      const salvo = await api(form.id ? `/perfis/${form.id}` : '/perfis', { method: form.id ? 'PUT' : 'POST', body })
      notificar.sucesso(`${salvo.permissoes.length} permissões gravadas em ${salvo.nome}`, 'Perfil salvo')
      api('/auth/eu').then(atualizarSessao).catch(() => {})
      carregar(salvo.id)
    } catch (e) {
      notificar.erro(e)
    }
  })
}

onMounted(() => carregar())
</script>

<template>
  <div class="cabecalho">
    <h1>Perfis e permissões</h1>
    <div class="acoes"><Button label="Novo perfil" icon="pi pi-plus" class="btn-confirmar" @click="selecionar(null)" /></div>
  </div>
  <div class="perfis-layout">
    <nav class="lista-perfis" aria-label="Perfis">
      <button v-for="p in perfis" :key="p.id" type="button" :class="{ selecionado: p.id === form.id }" @click="selecionar(p)">
        <strong>{{ p.nome }}</strong>
        <small v-if="p.descricao">{{ p.descricao }}</small>
        <span class="linha-cartao">
          <span class="pilula" :class="p.ativo ? 'ativo' : 'inativo'">{{ p.ativo ? 'Ativo' : 'Inativo' }}</span>
          <span class="pilula">{{ p.usuarios }} usuário(s)</span>
          <span class="pilula">{{ p.permissoes.length }} permissões</span>
        </span>
      </button>
    </nav>
    <section class="painel">
      <form class="grade" novalidate @submit.prevent="salvar">
        <div class="campo c4"><label for="pf-nome">Nome do perfil <span class="obrig">*</span></label><InputText id="pf-nome" v-model="form.nome" maxlength="40" :invalid="!!erro" /><span v-if="erro" class="erro">{{ erro }}</span></div>
        <div class="campo c6"><label for="pf-desc">Descrição</label><InputText id="pf-desc" v-model="form.descricao" maxlength="160" /></div>
        <div class="campo c2"><label for="pf-ativo">Ativo</label><ToggleSwitch v-model="form.ativo" input-id="pf-ativo" /></div>
        <div class="c12">
          <div v-for="m in modulos" :key="m.modulo" class="modulo-permissoes">
            <h3>{{ m.modulo }} <Button :label="moduloMarcado(m) ? 'Desmarcar módulo' : 'Marcar módulo'" size="small" text @click="alternarModulo(m)" /></h3>
            <div class="grade-permissoes">
              <label v-for="p in m.itens" :key="p.codigo">
                <Checkbox v-model="form.permissoes" :value="p.codigo" />
                <span class="numero-permissao">{{ p.codigo }}</span>{{ p.nome }}
              </label>
            </div>
          </div>
        </div>
      </form>
      <div class="rodape-form">
        <span class="texto-suave">{{ form.permissoes.length }} de {{ catalogo.length }} permissões marcadas</span>
        <Button :label="form.id ? 'Salvar perfil' : 'Incluir perfil'" icon="pi pi-check" class="btn-confirmar" :loading="enviando" :disabled="enviando" @click="salvar" />
      </div>
    </section>
  </div>
</template>
