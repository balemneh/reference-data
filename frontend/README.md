# Reference Data Frontend

This is the frontend for the CBP Reference Data Service. It is an Angular application that uses the US Web Design System (USWDS) for its UI components.

## Features

*   Government banner and CBP branding
*   Responsive design
*   Accessibility (WCAG 2.1 AA)
*   Real-time data updates
*   Advanced filtering and search
*   Data export capabilities

## Development

### Prerequisites

*   Node.js
*   Angular CLI

### Getting Started

1.  **Install dependencies:**

    ```bash
    npm install
    ```

2.  **Start the development server:**

    ```bash
    npm start
    ```

    The application will be available at [http://localhost:4200](http://localhost:4200).

### Building

*   **Build for production:**

    ```bash
    npm run build
    ```

### Testing

*   **Run unit tests:**

    ```bash
    npm test
    ```

*   **Run linting:**

    ```bash
    npm run lint
    ```

## Configuration

The application can be configured using environment variables.

### Environment Variables

| Variable | Description | Default | Example |
|----------|-------------|---------|---------|
| API_URL | Backend API URL | (empty - same origin) | `https://api.example.com` |
| KEYCLOAK_URL | Keycloak server URL | (empty) | `https://auth.example.com` |
| KEYCLOAK_REALM | Keycloak realm name | `reference-data` | `my-realm` |
| KEYCLOAK_CLIENT_ID | OAuth client ID | `reference-ui` | `my-app` |

### Local Development

For local development, you can create a file named `src/assets/env.js` to override the default environment variables.

```javascript
window.__env__ = window.__env__ || {};
window.__env__.apiUrl = 'http://localhost:8081';
```