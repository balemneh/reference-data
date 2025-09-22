# Reference Data Frontend

Angular 20 application with USWDS 3.13 for the CBP Reference Data Service.

## Configuration

### Environment Variables

| Variable | Description | Default | Example |
|----------|-------------|---------|---------|
| API_URL | Backend API URL | (empty - same origin) | `https://api.example.com` |
| KEYCLOAK_URL | Keycloak server URL | (empty) | `https://auth.example.com` |
| KEYCLOAK_REALM | Keycloak realm name | `reference-data` | `my-realm` |
| KEYCLOAK_CLIENT_ID | OAuth client ID | `reference-ui` | `my-app` |

### Local Development

1. **Edit `src/assets/env.js`:**
```javascript
window.__env__.apiUrl = 'http://localhost:8081';
```

2. **Run with proxy:**
```bash
ng serve --proxy-config proxy.conf.json
```

### Docker Deployment

```bash
docker run -d \
  -e API_URL=https://api.example.com \
  -e KEYCLOAK_URL=https://auth.example.com \
  -e KEYCLOAK_REALM=reference-data \
  -e KEYCLOAK_CLIENT_ID=reference-ui \
  -p 80:80 \
  refdata-ui
```

### Kubernetes ConfigMap

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

## Development

```bash
# Install dependencies
npm install

# Development server
npm start

# Build for production
npm run build

# Run tests
npm test

# Run linting
npm run lint
```

## Architecture

- **Framework**: Angular 20.1
- **UI Library**: USWDS 3.13 (US Web Design System)
- **State Management**: RxJS
- **HTTP Client**: Angular HttpClient
- **Authentication**: Keycloak/OAuth2
- **Build Tool**: Angular CLI

## Features

- Government banner and CBP branding
- Responsive design
- Accessibility (WCAG 2.1 AA)
- Real-time data updates
- Advanced filtering and search
- Data export capabilities