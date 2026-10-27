import { api } from './api'

export function pushSuportado() {
  return 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window
}

function chaveParaBytes(base64) {
  const pad = '='.repeat((4 - (base64.length % 4)) % 4)
  const b = atob((base64 + pad).replace(/-/g, '+').replace(/_/g, '/'))
  return Uint8Array.from([...b].map((c) => c.charCodeAt(0)))
}

export async function inscricaoAtual() {
  if (!pushSuportado()) return null
  const reg = await navigator.serviceWorker.getRegistration()
  return reg ? reg.pushManager.getSubscription() : null
}

export async function ativarPush() {
  if (!pushSuportado()) throw new Error('Este navegador não suporta notificações push')
  const permissao = await Notification.requestPermission()
  if (permissao !== 'granted') throw new Error('Permissão de notificação negada no navegador')
  const reg = await navigator.serviceWorker.ready
  const { chavePublica } = await api('/push/chave-publica')
  let sub = await reg.pushManager.getSubscription()
  if (!sub) {
    sub = await reg.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: chaveParaBytes(chavePublica) })
  }
  const j = sub.toJSON()
  await api('/push/inscricoes', { method: 'POST', body: { endpoint: j.endpoint, p256dh: j.keys.p256dh, auth: j.keys.auth } })
  return sub
}

export async function desativarPush() {
  const sub = await inscricaoAtual()
  if (!sub) return
  await api('/push/inscricoes', { method: 'DELETE', params: { endpoint: sub.endpoint } }).catch(() => {})
  await sub.unsubscribe()
}
