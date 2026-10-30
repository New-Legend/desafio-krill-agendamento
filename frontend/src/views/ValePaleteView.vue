<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { pode } from '../sessao'
import { deIso, fmtData, fmtDataHora, hoje, isoData } from '../utils'
import { useEnvio } from '../composables/useEnvio'
import { useNotificar } from '../composables/useNotificar'
import AssinaturaPad from '../components/AssinaturaPad.vue'
import { somar, useTiposPalete, zerar } from '../composables/useTiposPalete'

const route = useRoute()
const router = useRouter()
const notificar = useNotificar()
const { enviando, enviar } = useEnvio()
const tiposPalete = useTiposPalete()
const STATUS = { PENDENTE: ['Pendente de assinatura', 'warn'], ASSINADO: ['Assinado', 'info'], QUITADO: ['Quitado', 'success'], CANCELADO: ['Cancelado', 'danger'] }

const novo = computed(() => route.name === 'vale-novo')
const vale = ref(null)
const form = reactive({ agendamentoId: null, data: deIso(hoje()), fornecedor: '', placa: '', motorista: '', quantidades: {}, observacao: '' })
const dev = ref({})
const assinando = ref(null)
const nomeAss = ref('')
const pad = ref(null)
const total = somar
const tiposNovo = computed(() => tiposPalete.ativos.value)
const tiposVale = computed(() => (vale.value ? Object.keys(vale.value.entregues) : []))

async function carregar() {
  try {
    await tiposPalete.carregar()
  } catch (e) {
    notificar.erro(e)
  }
  if (novo.value) {
    vale.value = null
    form.quantidades = zerar(tiposNovo.value.map((t) => t.codigo))
    if (route.query.agendamento) {
      try {
        const a = await api(`/agendamentos/${route.query.agendamento}`)
        Object.assign(form, { agendamentoId: a.id, data: deIso(a.data), fornecedor: a.fornecedor, placa: a.placa || '', motorista: a.motorista || '' })
        const primeiro = tiposNovo.value[0]?.codigo
        if (primeiro) form.quantidades[primeiro] = a.paletesRecebidos ?? a.quantidadePaletes
      } catch (e) {
        notificar.erro(e)
      }
    }
    return
  }
  try {
    vale.value = await api(`/vales-palete/${route.params.id}`)
    dev.value = zerar(tiposVale.value)
  } catch (e) {
    notificar.erro(e)
    router.replace('/vales-palete')
  }
}

function criar() {
  if (!form.fornecedor.trim()) return notificar.aviso('Informe o fornecedor')
  if (!somar(form.quantidades)) return notificar.aviso('Informe ao menos um palete entregue')
  enviar(async () => {
    try {
      const v = await api('/vales-palete', { method: 'POST', body: { ...form, data: isoData(form.data), fornecedor: form.fornecedor.trim(), placa: form.placa.trim() || null, motorista: form.motorista.trim() || null, observacao: form.observacao.trim() || null } })
      notificar.sucesso(`Vale ${v.codigo} emitido. Colete as assinaturas.`)
      router.replace(`/vales-palete/${v.id}`)
    } catch (e) {
      notificar.erro(e)
    }
  })
}

function abrirAssinatura(quem) {
  assinando.value = quem
  nomeAss.value = quem === 'MOTORISTA' ? vale.value.motorista || '' : ''
}

function salvarAssinatura() {
  const img = pad.value?.imagem()
  if (!nomeAss.value.trim()) return notificar.aviso('Informe o nome de quem assina')
  if (!img) return notificar.aviso('Faça a assinatura no quadro')
  enviar(async () => {
    try {
      vale.value = await api(`/vales-palete/${vale.value.id}/assinaturas/${assinando.value}`, { method: 'PUT', body: { nome: nomeAss.value.trim(), imagem: img } })
      assinando.value = null
      notificar.sucesso('Assinatura registrada')
    } catch (e) {
      notificar.erro(e)
    }
  })
}

function refazer(quem) {
  enviar(async () => {
    try {
      vale.value = await api(`/vales-palete/${vale.value.id}/assinaturas/${quem}`, { method: 'DELETE' })
      abrirAssinatura(quem)
    } catch (e) {
      notificar.erro(e)
    }
  })
}

function devolver() {
  enviar(async () => {
    try {
      vale.value = await api(`/vales-palete/${vale.value.id}/devolucoes`, { method: 'POST', body: { quantidades: dev.value } })
      dev.value = zerar(tiposVale.value)
      notificar.sucesso(vale.value.status === 'QUITADO' ? 'Vale quitado' : 'Devolução registrada')
    } catch (e) {
      notificar.erro(e)
    }
  })
}

function imprimir() {
  window.print()
}

function cancelar() {
  enviar(async () => {
    try {
      vale.value = await api(`/vales-palete/${vale.value.id}/cancelamento`, { method: 'PATCH' })
      notificar.sucesso('Vale cancelado')
    } catch (e) {
      notificar.erro(e)
    }
  })
}

watch(() => route.fullPath, carregar)
onMounted(carregar)
</script>

<template>
  <template v-if="novo">
    <div class="cabecalho"><h1>Novo vale-palete</h1></div>
    <form class="painel" @submit.prevent="criar">
      <div class="form-grade">
        <div class="campo c3"><label for="vd">Data</label><DatePicker v-model="form.data" input-id="vd" date-format="dd/mm/yy" show-icon icon-display="input" /></div>
        <div class="campo c9"><label for="vfo">Fornecedor / transportadora <span class="obrig">*</span></label><InputText id="vfo" v-model="form.fornecedor" maxlength="80" /></div>
        <div class="campo c3"><label for="vpl">Placa</label><InputText id="vpl" v-model="form.placa" maxlength="8" /></div>
        <div class="campo c9"><label for="vmo">Motorista</label><InputText id="vmo" v-model="form.motorista" maxlength="80" /></div>
        <div v-for="t in tiposNovo" :key="t.codigo" class="campo c3"><label :for="`q-${t.codigo}`">{{ t.nome }}</label><InputNumber v-model="form.quantidades[t.codigo]" :input-id="`q-${t.codigo}`" :min="0" :max="1000" /></div>
        <div class="campo c12"><label for="vob">Observação</label><Textarea id="vob" v-model="form.observacao" rows="2" maxlength="500" /></div>
      </div>
      <div class="rodape-form">
        <Button label="Voltar" type="button" class="btn-secundario" @click="router.back()" />
        <Button type="submit" label="Emitir vale" icon="pi pi-check" class="btn-confirmar" :loading="enviando" :disabled="enviando" />
      </div>
    </form>
  </template>

  <template v-else-if="vale">
    <div class="cabecalho nao-imprimir">
      <h1>Vale {{ vale.codigo }}</h1>
      <div class="acoes">
        <Button label="Voltar" class="btn-secundario" @click="router.push('/vales-palete')" />
        <Button label="Imprimir" icon="pi pi-print" class="btn-confirmar" @click="imprimir" />
      </div>
    </div>

    <article class="painel vale-impressao">
      <header class="vale-topo">
        <div class="vale-marca"><img src="/img/rede-krill-logo.png" alt="Rede Krill" width="570" height="528" /><div><strong>Rede Krill · Centro de Distribuição</strong><small>Vale-palete</small></div></div>
        <div class="vale-codigo"><strong>{{ vale.codigo }}</strong><Tag :value="STATUS[vale.status][0]" :severity="STATUS[vale.status][1]" /></div>
      </header>
      <dl class="detalhes">
        <dt>Data</dt><dd>{{ fmtData(vale.data) }}</dd>
        <dt>Fornecedor</dt><dd>{{ vale.fornecedor }}</dd>
        <dt>Placa / motorista</dt><dd>{{ vale.placa || '—' }} · {{ vale.motorista || '—' }}</dd>
        <template v-if="vale.agendamentoId"><dt>Agendamento</dt><dd>#{{ vale.agendamentoId }}</dd></template>
        <template v-if="vale.observacao"><dt>Observação</dt><dd>{{ vale.observacao }}</dd></template>
      </dl>
      <table class="tabela-vale">
        <thead><tr><th>Tipo</th><th>Entregues</th><th>Devolvidos</th><th>Saldo a devolver</th></tr></thead>
        <tbody>
          <tr v-for="k in tiposVale" :key="k"><td>{{ tiposPalete.rotulo(k) }}</td><td>{{ vale.entregues[k] || 0 }}</td><td>{{ vale.devolvidos[k] || 0 }}</td><td><b>{{ vale.saldo[k] || 0 }}</b></td></tr>
        </tbody>
        <tfoot><tr><td>Total</td><td>{{ total(vale.entregues) }}</td><td>{{ total(vale.devolvidos) }}</td><td><b>{{ total(vale.saldo) }}</b></td></tr></tfoot>
      </table>
      <p class="texto-legal">Declaramos que os paletes acima ficaram em poder da Rede Krill e serão devolvidos ou compensados conforme saldo.</p>
      <div class="assinaturas">
        <div v-for="[quem, rotulo, ass] in [['MOTORISTA', 'Motorista', vale.assinaturaMotorista], ['CONFERENTE', 'Conferente Rede Krill', vale.assinaturaConferente]]" :key="quem" class="caixa-assinatura">
          <img v-if="ass" :src="ass.imagem" :alt="`Assinatura ${rotulo}`" />
          <div v-else class="linha-vazia" />
          <span>{{ ass ? ass.nome : rotulo }}</span>
          <small class="texto-suave">{{ ass ? `${rotulo} · ${fmtDataHora(ass.em)}` : 'assinatura' }}</small>
          <template v-if="pode(1303) && vale.status !== 'CANCELADO' && vale.status !== 'QUITADO'">
            <Button v-if="!ass" label="Assinar" icon="pi pi-pencil" size="small" class="btn-confirmar nao-imprimir" @click="abrirAssinatura(quem)" />
            <Button v-else label="Refazer" icon="pi pi-replay" size="small" text class="nao-imprimir" :disabled="enviando" @click="refazer(quem)" />
          </template>
        </div>
      </div>
    </article>

    <section v-if="pode(1303) && ['PENDENTE', 'ASSINADO'].includes(vale.status)" class="painel nao-imprimir">
      <h2>Registrar devolução</h2>
      <form class="form-grade" @submit.prevent="devolver">
        <div v-for="k in tiposVale" :key="k" class="campo c2"><label :for="`d-${k}`">{{ tiposPalete.rotulo(k) }}</label><InputNumber v-model="dev[k]" :input-id="`d-${k}`" :min="0" :max="vale.saldo[k] || 0" /></div>
        <div class="campo c4 alinhar-fim"><Button type="submit" label="Registrar devolução" icon="pi pi-check" class="btn-confirmar" :loading="enviando" :disabled="enviando" /></div>
      </form>
      <div class="rodape-form"><Button label="Cancelar vale" icon="pi pi-times" class="btn-excluir" :disabled="enviando" @click="cancelar" /></div>
    </section>

    <Dialog :visible="!!assinando" modal :header="assinando === 'MOTORISTA' ? 'Assinatura do motorista' : 'Assinatura do conferente'" :style="{ width: '560px' }" :breakpoints="{ '600px': '96vw' }" @update:visible="(v) => { if (!v) assinando = null }">
      <div class="campo"><label for="na">Nome <span class="obrig">*</span></label><InputText id="na" v-model="nomeAss" maxlength="80" /></div>
      <AssinaturaPad ref="pad" />
      <template #footer>
        <Button label="Voltar" class="btn-secundario" :disabled="enviando" @click="assinando = null" />
        <Button label="Salvar assinatura" icon="pi pi-check" class="btn-confirmar" :loading="enviando" :disabled="enviando" @click="salvarAssinatura" />
      </template>
    </Dialog>
  </template>
</template>

<style scoped>
.rodape-form { display: flex; justify-content: flex-end; gap: 8px; margin-top: 12px; }
.alinhar-fim { justify-content: flex-end; }
.vale-topo { display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 2px solid var(--cor-texto); padding-bottom: 8px; margin-bottom: 10px; }
.vale-topo div { display: flex; flex-direction: column; gap: 4px; }
.vale-codigo { align-items: flex-end; }
.vale-topo .vale-marca { flex-direction: row; align-items: center; gap: 10px; }
.vale-marca img { height: 54px; width: auto; aspect-ratio: 570 / 528; object-fit: contain; }
.vale-codigo strong { font-size: 18px; font-variant-numeric: tabular-nums; }
.tabela-vale { width: 100%; border-collapse: collapse; margin: 12px 0; font-size: 13px; }
.tabela-vale th, .tabela-vale td { border: 1px solid var(--cor-linha); padding: 6px 8px; text-align: right; }
.tabela-vale th:first-child, .tabela-vale td:first-child { text-align: left; }
.tabela-vale thead, .tabela-vale tfoot { background: var(--cor-fundo); font-weight: 600; }
.texto-legal { font-size: 12px; color: var(--cor-texto-suave); }
.assinaturas { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; margin-top: 16px; }
.caixa-assinatura { display: flex; flex-direction: column; align-items: center; gap: 4px; text-align: center; }
.caixa-assinatura img { height: 80px; max-width: 100%; object-fit: contain; border-bottom: 1px solid var(--cor-texto); width: 100%; }
.linha-vazia { height: 80px; width: 100%; border-bottom: 1px solid var(--cor-texto); }
@media (max-width: 640px) { .assinaturas { grid-template-columns: 1fr; } }
@media print {
  .nao-imprimir { display: none; }
  .vale-impressao { border: 0; box-shadow: none; padding: 0; }
}
</style>
