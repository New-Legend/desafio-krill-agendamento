// jsdom não implementa matchMedia/ResizeObserver, usados pelos componentes PrimeVue.
if (!window.matchMedia) {
  window.matchMedia = (q) => ({ matches: false, media: q, onchange: null, addListener() {}, removeListener() {}, addEventListener() {}, removeEventListener() {}, dispatchEvent: () => false })
}
if (!window.ResizeObserver) {
  window.ResizeObserver = class { observe() {} unobserve() {} disconnect() {} }
}
