<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { fmtData, fmtDataHora, fmtMoeda } from '../utils'
import { useNotificar } from '../composables/useNotificar'

const route = useRoute()
const router = useRouter()
const notificar = useNotificar()
const c = ref(null)
const FORMAS = { PIX: 'PIX', BOLETO: 'Boleto', DINHEIRO: 'Dinheiro', DESCONTO_NF: 'Desconto na nota fiscal', DEVOLUCAO_FISICA: 'Devolução física dos paletes' }

onMounted(async () => {
  try {
    c.value = await api(`/paletes/cobrancas/${route.params.id}`)
  } catch (e) {
    notificar.erro(e)
  }
})
function imprimir() {
  window.print()
}
</script>

<template>
  <div class="cabecalho nao-imprimir">
    <h1>Recibo de paletes</h1>
    <div class="acoes">
      <Button label="Voltar" icon="pi pi-arrow-left" class="btn-secundario" @click="router.push('/paletes/cobrancas')" />
      <Button v-if="c?.reciboNumero" label="Imprimir" icon="pi pi-print" class="btn-confirmar" @click="imprimir" />
    </div>
  </div>
  <div v-if="c && !c.reciboNumero" class="painel vazio">Cobrança #{{ c.id }} ainda não foi paga — o recibo é emitido no recebimento.</div>
  <article v-else-if="c" class="painel recibo">
    <header class="recibo-topo">
      <div class="marca"><img src="/img/rede-krill-logo.png" alt="Rede Krill" width="570" height="528" /><div><strong>Rede Krill · Centro de Distribuição</strong><small>Recibo de pagamento de paletes</small></div></div>
      <div class="numero"><strong>{{ c.reciboNumero }}</strong><small>{{ fmtDataHora(c.pagoEm) }}</small></div>
    </header>
    <p class="texto">
      Recebemos de <strong>{{ c.fornecedor }}</strong> a importância de <strong>{{ fmtMoeda(c.valorTotal) }}</strong>,
      referente a <strong>{{ c.quantidade }}</strong> palete(s) não devolvido(s) ao CD, ao valor unitário de {{ fmtMoeda(c.valorUnitario) }}
      (cobrança #{{ c.id }}, vencimento {{ fmtData(c.vencimento) }}).
    </p>
    <dl class="detalhes">
      <dt>Forma de pagamento</dt><dd>{{ FORMAS[c.formaPagamento] || c.formaPagamento }}</dd>
      <dt>Recebido por</dt><dd>{{ c.recebidoPor }}</dd>
      <template v-if="c.observacao"><dt>Observação</dt><dd>{{ c.observacao }}</dd></template>
    </dl>
    <p class="texto-suave legal">Com este pagamento, o saldo de paletes do fornecedor é baixado na mesma quantidade.</p>
    <div class="assinatura"><div class="linha" /><span>Rede Krill · Recebimento</span></div>
  </article>
</template>

<style scoped>
.recibo { max-width: 760px; }
.recibo-topo { display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 2px solid var(--cor-texto); padding-bottom: 8px; margin-bottom: 14px; }
.marca { display: flex; align-items: center; gap: 10px; }
.marca div, .numero { display: flex; flex-direction: column; gap: 4px; }
.numero { align-items: flex-end; }
.numero strong { font-size: 18px; font-variant-numeric: tabular-nums; }
.marca img { height: 54px; width: auto; aspect-ratio: 570 / 528; object-fit: contain; }
.texto { line-height: 1.6; }
.legal { font-size: 12px; }
.assinatura { display: flex; flex-direction: column; align-items: center; margin: 40px auto 0; width: 300px; gap: 4px; }
.linha { width: 100%; border-bottom: 1px solid var(--cor-texto); height: 50px; }
@media print {
  .nao-imprimir { display: none; }
  .recibo { border: 0; box-shadow: none; padding: 0; }
}
</style>
