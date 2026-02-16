// Polyfill for sockjs-client which references Node.js `global` variable.
// In browsers, `global` does not exist — map it to `window`.
(function () {
  if (typeof globalThis !== 'undefined' && !globalThis.global) {
    globalThis.global = globalThis;
  }
})();
