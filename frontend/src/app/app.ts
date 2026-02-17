import { JwksValidationHandler } from 'angular-oauth2-oidc-jwks';
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { BannerComponent } from './components/banner/banner';
import { ToastContainerComponent } from './components/toast-container/toast-container';
import { CommandPaletteComponent } from './components/command-palette/command-palette';
import { HeaderComponent } from './components/header/header.component';
import { OAuthService } from 'angular-oauth2-oidc';
import { authConfig } from './app.config';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, HeaderComponent, BannerComponent, ToastContainerComponent, CommandPaletteComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  title = 'CBP Reference Data Admin';

  constructor(private oauthService: OAuthService) {
    this.oauthService.configure(authConfig);
    this.oauthService.loadDiscoveryDocumentAndTryLogin();

    this.oauthService.events.subscribe(event => {
      console.log(event);
    });

    this.oauthService.tryLogin();
  }
}

