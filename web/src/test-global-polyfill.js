/**
 * Testing-only polyfill for SockJS. 
 * It expects a Node.js-style 'global' object that browser environments 
 * don't usually provide, so we bridge that gap.
 */
(function () {
  if (typeof globalThis !== 'undefined' && !globalThis.global) {
    globalThis.global = globalThis;
  }
})();
