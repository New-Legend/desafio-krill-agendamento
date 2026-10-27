<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'
import { deIso, diaSemana, fmtData, hoje, isoData } from '../utils'
import { useNotificar } from '../composables/useNotificar'
import SeletorCd from '../components/SeletorCd.vue'

const router = useRouter()
const notificar = useNotificar()
const data = ref(deIso(hoje()))
const cdId = ref(null)
const rotina = ref(null)
const ICONES = { SEM_CONFIRMACAO: 'pi-phone', ATRASADOS: 'pi-clock', ESPERA: 'pi-hourglass', DESCARGA_LONGA: 'pi-truck', SEM_NFE: 'pi-file', DIVERGENCIAS: 'pi-exclamation-triangle', VALES: 'pi-ticket', COBRANCAS: 'pi-wallet' }

async function carregar() {
  try {
    rotina.value = await api('/rotina', { params: { data: isoData(data.value), cdId: cdId.value } })
  } catch (e) {
    notificar.erro(e)
  }
}
watch([data, cdId], carregar)
onMounted(carregar)
</script>

<template>
  <div class="cabecalho">
    <h1>Rotina do dia</h1>
    <div class="acoes filtros-inline">
      <SeletorCd v-model="cdId" input-id="rcd" />
      <div class="campo"><label for="rd">Data</label><DatePicker v-model="data" input-id="rd" date-format="dd/mm/yy" show-icon icon-display="input" /></div>
    </div>
  </div>
  <p v-if="rotina" class="texto-suave">{{ diaSemana(rotina.data) }}, {{ fmtData(rotina.data) }} — o que o recebimento precisa resolver.</p>
  <div v-if="rotina" class="rotina">
    <section v-for="i in rotina.itens" :key="i.codigo" class="painel item" :class="i.itens.length ? `nivel-${i.nivel}` : 'ok'">
      <div class="painel-titulo">
        <h2><i class="pi" :class="ICONES[i.codigo] || 'pi-info-circle'" /> {{ i.titulo }}</h2>
        <strong class="contador">{{ i.itens.length }}</strong>
      </div>
      <ul v-if="i.itens.length">
        <li v-for="r in i.itens.slice(0, 8)" :key="`${i.codigo}${r.id}`"><a href="#" @click.prevent="router.push(r.link)">{{ r.descricao }}</a></li>
        <li v-if="i.itens.length > 8" class="texto-suave">+ {{ i.itens.length - 8 }}</li>
      </ul>
      <p v-else class="texto-suave">Tudo em dia</p>
    </section>
  </div>
</template>

<style scoped>
.filtros-inline { display: flex; gap: 10px; align-items: flex-end; }
.rotina { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 12px; }
.item { margin: 0; border-left: 4px solid var(--cor-borda, #d9dee5); }
.item.nivel-erro { border-left-color: #d64545; }
.item.nivel-aviso { border-left-color: #e0a31a; }
.item.nivel-info { border-left-color: #3b82c4; }
.item.ok { border-left-color: #3aa76d; }
.item h2 { font-size: 14px; display: flex; gap: 6px; align-items: center; }
.contador { font-size: 20px; }
.item ul { margin: 6px 0 0; padding-left: 18px; font-size: 13px; }
.item li { margin: 2px 0; }
</style>
