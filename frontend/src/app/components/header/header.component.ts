import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OAuthService } from 'angular-oauth2-oidc';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './header.component.html',
})
export class HeaderComponent {

  constructor(private oauthService: OAuthService) {
  }

  public login() {
    this.oauthService.initCodeFlow();
  }

  public logout() {
    this.oauthService.logOut();
  }

  get userProfile() {
    const claims = this.oauthService.getIdentityClaims();
    if (!claims) {
      return null;
    }
    return { name: claims['name'] };
  }

  get isLoggedIn() {
    return this.oauthService.hasValidAccessToken();
  }
}
