<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api'
import { useEnvio } from '../composables/useEnvio'
import { useNotificar } from '../composables/useNotificar'

const notificar = useNotificar()
const { enviando, enviar } = useEnvio()
const linhas = ref([])

async function carregar() {
  try {
    const [tipos, params] = await Promise.all([api('/tipos-caminhao'), api('/parametros/descarga')])
    linhas.value = tipos.map((t) => {
      const p = params.find((x) => x.tipoCaminhaoId === t.id) || {}
      return { tipoCaminhaoId: t.id, nome: t.nome, minutosPrevistos: p.minutosPrevistos ?? 60, toleranciaEsperaMinutos: p.toleranciaEsperaMinutos ?? 30, custoPorPalete: p.custoPorPalete ?? 0 }
    })
  } catch (e) {
    notificar.erro(e)
  }
}

function salvar(l) {
  enviar(async () => {
    try {
      await api(`/parametros/descarga/${l.tipoCaminhaoId}`, { method: 'PUT', body: { minutosPrevistos: l.minutosPrevistos, toleranciaEsperaMinutos: l.toleranciaEsperaMinutos, custoPorPalete: l.custoPorPalete } })
      notificar.sucesso(`Parâmetros de ${l.nome} salvos`)
    } catch (e) {
      notificar.erro(e)
    }
  })
}
onMounted(carregar)
</script>

<template>
  <div class="cabecalho"><h1>Tempos de descarga</h1></div>
  <section class="painel">
    <p class="texto-suave">Tempo previsto de descarga por tipo de caminhão, tolerância de espera no pátio e custo de descarga por palete (chapa).</p>
    <DataTable :value="linhas" size="small" striped-rows data-key="tipoCaminhaoId">
      <Column field="nome" header="Tipo de caminhão" />
      <Column header="Descarga prevista (min)"><template #body="{ data }"><InputNumber v-model="data.minutosPrevistos" :min="1" :max="1440" :input-id="`mp${data.tipoCaminhaoId}`" /></template></Column>
      <Column header="Tolerância de espera (min)"><template #body="{ data }"><InputNumber v-model="data.toleranciaEsperaMinutos" :min="0" :max="1440" /></template></Column>
      <Column header="Custo por palete"><template #body="{ data }"><InputNumber v-model="data.custoPorPalete" mode="currency" currency="BRL" locale="pt-BR" :min="0" /></template></Column>
      <Column header=""><template #body="{ data }"><Button icon="pi pi-check" label="Salvar" class="btn-confirmar" :disabled="enviando" @click="salvar(data)" /></template></Column>
    </DataTable>
  </section>
</template>
