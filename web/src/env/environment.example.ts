// Copy this file to environment.ts and configure for your environment
// DO NOT commit environment.ts to version control

export const environment = {
  production: false,
  apiHost: '/api/',
  wsHost: 'http://localhost:8081/ws',
  map: {
    // Default map center coordinates (Novi Sad, Serbia)
    defaultLat: 45.2671,
    defaultLng: 19.8335,
    defaultZoom: 14,
    // OpenStreetMap tile layer configuration
    tileLayerUrl: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
  }
};
