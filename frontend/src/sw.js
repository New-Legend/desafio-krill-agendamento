/* Service worker: cache do shell (offline) + Web Push */
import { precacheAndRoute, cleanupOutdatedCaches, createHandlerBoundToURL } from 'workbox-precaching'
import { NavigationRoute, registerRoute } from 'workbox-routing'

self.skipWaiting()
cleanupOutdatedCaches()
precacheAndRoute(self.__WB_MANIFEST)

// Qualquer navegação (exceto API/Swagger) abre o index.html do cache: o app abre mesmo sem rede.
registerRoute(new NavigationRoute(createHandlerBoundToURL('/index.html'), {
  denylist: [/^\/api\//, /^\/swagger-ui/, /^\/v3\//]
}))

self.addEventListener('activate', (event) => event.waitUntil(self.clients.claim()))

self.addEventListener('push', (event) => {
  let dados = { title: 'Agendamento de Recebimento', body: '', url: '/painel' }
  try {
    dados = { ...dados, ...event.data.json() }
  } catch {
    if (event.data) dados.body = event.data.text()
  }
  event.waitUntil(self.registration.showNotification(dados.title, {
    body: dados.body,
    icon: '/icons/icon-192.png',
    badge: '/icons/icon-192.png',
    tag: dados.url,
    data: { url: dados.url }
  }))
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const url = event.notification.data?.url || '/painel'
  event.waitUntil((async () => {
    const janelas = await self.clients.matchAll({ type: 'window', includeUncontrolled: true })
    for (const c of janelas) {
      if ('focus' in c) {
        await c.focus()
        if ('navigate' in c) return c.navigate(url)
        return
      }
    }
    return self.clients.openWindow(url)
  })())
})
