<script setup>
import { onMounted, ref } from 'vue'

/** Quadro de assinatura (mouse/toque). Emite PNG em data URL. */
const emit = defineEmits(['alterado'])
const canvas = ref(null)
let ctx
let desenhando = false
let vazio = true

function pos(e) {
  const r = canvas.value.getBoundingClientRect()
  const p = e.touches ? e.touches[0] : e
  return [(p.clientX - r.left) * (canvas.value.width / r.width), (p.clientY - r.top) * (canvas.value.height / r.height)]
}
function iniciar(e) {
  e.preventDefault()
  desenhando = true
  ctx.beginPath()
  ctx.moveTo(...pos(e))
}
function mover(e) {
  if (!desenhando) return
  e.preventDefault()
  ctx.lineTo(...pos(e))
  ctx.stroke()
  vazio = false
}
function parar() {
  if (desenhando) emit('alterado', !vazio)
  desenhando = false
}
function limpar() {
  ctx.clearRect(0, 0, canvas.value.width, canvas.value.height)
  vazio = true
  emit('alterado', false)
}
function imagem() {
  return vazio ? null : canvas.value.toDataURL('image/png')
}
onMounted(() => {
  ctx = canvas.value.getContext('2d')
  ctx.lineWidth = 2.5
  ctx.lineCap = 'round'
  ctx.strokeStyle = '#0f172a'
})
defineExpose({ limpar, imagem })
</script>

<template>
  <div class="assinatura">
    <canvas ref="canvas" width="600" height="180" @mousedown="iniciar" @mousemove="mover" @mouseup="parar" @mouseleave="parar"
            @touchstart="iniciar" @touchmove="mover" @touchend="parar" />
    <Button label="Limpar" icon="pi pi-eraser" text size="small" type="button" @click="limpar" />
  </div>
</template>

<style scoped>
.assinatura { display: flex; flex-direction: column; gap: 4px; align-items: flex-end; }
canvas { width: 100%; height: 140px; background: var(--cor-campo); border: 1px dashed #94a3b8; border-radius: var(--raio); touch-action: none; }
</style>
