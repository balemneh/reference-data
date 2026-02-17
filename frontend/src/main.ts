import '@angular/compiler';
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';
import { OAuthService } from 'angular-oauth2-oidc';
import { authConfig } from './app/app.config';

bootstrapApplication(App, appConfig)
  .then(appRef => {
    const oauthService = appRef.injector.get(OAuthService);
    oauthService.configure(authConfig);
    oauthService.loadDiscoveryDocumentAndTryLogin();
  })
  .catch((err) => console.error(err));
