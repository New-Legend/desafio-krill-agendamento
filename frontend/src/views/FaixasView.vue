<script setup>
import CadastroCrud from '../components/CadastroCrud.vue'

const campos = [
  { campo: 'horaInicio', rotulo: 'Hora inicial', tipo: 'hora', obrigatorio: true },
  { campo: 'horaFim', rotulo: 'Hora final', tipo: 'hora', obrigatorio: true },
  { campo: 'capacidadePaletes', rotulo: 'Capacidade (paletes)', tipo: 'numero', obrigatorio: true, min: 1, maxNum: 100000, dica: 'Ao exceder, o sistema avisa mas não bloqueia' },
  { campo: 'ativo', rotulo: 'Situação', tipo: 'ativo' }
]
const novo = () => ({ horaInicio: '', horaFim: '', capacidadePaletes: 120, ativo: true })
const paraForm = (r) => ({ ...r, horaInicio: r.horaInicio.slice(0, 5), horaFim: r.horaFim.slice(0, 5) })
const validarExtra = (f) => (f.horaInicio && f.horaFim && f.horaFim <= f.horaInicio ? { horaFim: 'Hora final deve ser posterior à inicial' } : null)
</script>

<template>
  <CadastroCrud titulo="Faixas horárias" recurso="/faixas-horarias" :campos="campos" :novo-padrao="novo" :para-form="paraForm"
                :validar-extra="validarExtra" rotulo-novo="Nova faixa">
    <template #rodape="{ lista }">
      <p class="texto-suave">{{ lista.filter((f) => f.ativo).length }} faixa(s) ativa(s). Duração e quantidade máxima seguem o cadastro de capacidade.</p>
    </template>
  </CadastroCrud>
</template>
