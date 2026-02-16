// Polyfill for sockjs-client which references Node.js `global` variable.
// In browsers, `global` does not exist — map it to `window`.
(window).global = window;
