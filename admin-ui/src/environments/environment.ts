export const environment = {
  production: false,
  // API URL can be overridden by setting window.__env__.apiUrl
  apiUrl: (window as any).__env__?.apiUrl || 'http://localhost:8081'
};