export const environment = {
  production: true,
  // API URL can be overridden by setting window.__env__.apiUrl
  // In production, defaults to same origin
  apiUrl: (window as any).__env__?.apiUrl || ''
};