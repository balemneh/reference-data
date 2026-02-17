import { APP_INITIALIZER, ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { Router, provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { AuthConfig, OAuthService, OAuthStorage, provideOAuthClient } from 'angular-oauth2-oidc';
import { routes } from './app.routes';
import { AuthGuard } from './guards/auth.guard';
import { FeatureFlagGuard } from './guards/feature-flag.guard';
import { authInterceptor } from './interceptors/auth.interceptor';
import { JwksValidationHandler } from 'angular-oauth2-oidc-jwks';
import { provideMarkdown } from 'ngx-markdown';

// Read from window.__env__ provided by env.js, with fallback for local dev
const keycloakUrlFromEnv = (window as any).__env__?.keycloakUrl;
const keycloakUrl = (keycloakUrlFromEnv && !keycloakUrlFromEnv.startsWith('${'))
  ? keycloakUrlFromEnv
  : 'http://localhost:8085';

const keycloakRealmFromEnv = (window as any).__env__?.keycloakRealm;
const keycloakRealm = (keycloakRealmFromEnv && !keycloakRealmFromEnv.startsWith('${'))
  ? keycloakRealmFromEnv
  : 'reference-data';

const keycloakClientIdFromEnv = (window as any).__env__?.keycloakClientId;
const keycloakClientId = (keycloakClientIdFromEnv && !keycloakClientIdFromEnv.startsWith('${'))
  ? keycloakClientIdFromEnv
  : 'reference-admin-ui';
const requireHttps = (window as any).__env__?.requireHttps === 'true' || false;

export const authConfig: AuthConfig = {
  issuer: `${keycloakUrl}/realms/${keycloakRealm}`,
  redirectUri: window.location.origin,
  clientId: keycloakClientId,
  responseType: 'code',
  scope: 'openid profile email roles',
  showDebugInformation: true,
  requireHttps: requireHttps,
  postLogoutRedirectUri: window.location.origin + '/logout',
};

export function storageFactory(): OAuthStorage {
  return localStorage;
}

export function initializeOAuth(oauthService: OAuthService, router: Router): () => Promise<void> {
  return () => {
    oauthService.configure(authConfig);
    oauthService.tokenValidationHandler = new JwksValidationHandler();
    return oauthService.loadDiscoveryDocument().then(() => {
        // Manually handle login
        if (window.location.search.includes('code=')) {
            return oauthService.tryLogin().then(() => {
                // After successful login, clean the URL
                const url = router.url.split('?')[0];
                router.navigateByUrl(url);
            });
        } else {
            return Promise.resolve();
        }
    });
  };
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAnimations(),
    provideOAuthClient(),
    provideMarkdown(),
    AuthGuard,
    FeatureFlagGuard,
    { provide: OAuthStorage, useFactory: storageFactory },
    {
      provide: APP_INITIALIZER,
      useFactory: initializeOAuth,
      deps: [OAuthService, Router],
      multi: true,
    },
  ],
};
