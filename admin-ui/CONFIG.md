# Frontend Configuration Guide

## API URL Configuration

The frontend application supports multiple ways to configure the backend API URL:

### 1. Local Development

Edit `src/assets/env.js`:
```javascript
window.__env__.apiUrl = 'http://localhost:8081';
```

### 2. Docker Runtime Configuration

Set environment variables when running the container:
```bash
docker run -d \
  -e API_URL=https://api.example.com \
  -e KEYCLOAK_URL=https://auth.example.com \
  -e KEYCLOAK_REALM=reference-data \
  -e KEYCLOAK_CLIENT_ID=reference-ui \
  -p 80:80 \
  refdata-ui
```

### 3. Docker Compose

Add environment variables in `docker-compose.yml`:
```yaml
services:
  admin-ui:
    image: refdata-ui
    environment:
      API_URL: ${API_URL:-http://refdata-api:8080}
      KEYCLOAK_URL: ${KEYCLOAK_URL:-http://keycloak:8080}
      KEYCLOAK_REALM: ${KEYCLOAK_REALM:-reference-data}
      KEYCLOAK_CLIENT_ID: ${KEYCLOAK_CLIENT_ID:-reference-ui}
```

### 4. Kubernetes ConfigMap

Create a ConfigMap:
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: refdata-ui-config
data:
  env.js: |
    (function (window) {
      window.__env__ = window.__env__ || {};
      window.__env__.apiUrl = 'https://api.refdata.cbp.gov';
      window.__env__.keycloakUrl = 'https://auth.refdata.cbp.gov';
      window.__env__.keycloakRealm = 'reference-data';
      window.__env__.keycloakClientId = 'reference-ui';
    }(this));
```

Mount the ConfigMap:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: refdata-ui
spec:
  template:
    spec:
      containers:
      - name: ui
        image: refdata-ui
        volumeMounts:
        - name: config
          mountPath: /usr/share/nginx/html/assets/env.js
          subPath: env.js
      volumes:
      - name: config
        configMap:
          name: refdata-ui-config
```

### 5. Build-Time Configuration

For static builds, modify `environment.prod.ts` before building:
```typescript
export const environment = {
  production: true,
  apiUrl: 'https://api.production.example.com'
};
```

## Environment Variables

| Variable | Description | Default | Example |
|----------|-------------|---------|---------|
| API_URL | Backend API URL | (empty - same origin) | `https://api.example.com` |
| KEYCLOAK_URL | Keycloak server URL | (empty) | `https://auth.example.com` |
| KEYCLOAK_REALM | Keycloak realm name | `reference-data` | `my-realm` |
| KEYCLOAK_CLIENT_ID | OAuth client ID | `reference-ui` | `my-app` |

## Order of Precedence

1. Runtime environment variables (Docker/K8s)
2. `window.__env__` object in `env.js`
3. Build-time environment files (`environment.ts`/`environment.prod.ts`)
4. Default values

## Testing Configuration

To verify the configuration is loaded correctly:

1. Open browser DevTools Console
2. Run: `window.__env__`
3. Check the values are correct

## Proxy Configuration (Development)

For local development with proxy, create `proxy.conf.json`:
```json
{
  "/api": {
    "target": "http://localhost:8081",
    "secure": false,
    "changeOrigin": true
  }
}
```

Run with: `ng serve --proxy-config proxy.conf.json`