const getApiUrl = () => {
  const apiUrlFromEnv = (window as any).__env__?.apiUrl;
  if (apiUrlFromEnv && !apiUrlFromEnv.startsWith('${')) {
    return apiUrlFromEnv;
  }
  return 'http://localhost:8083';
};

export const environment = {
  production: false,
  apiUrl: getApiUrl()
};