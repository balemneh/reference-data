import { Injectable } from '@angular/core';
import Keycloak from 'keycloak-js';
import { KeycloakService } from 'keycloak-angular';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private keycloakInstance: Keycloak;

  constructor(private keycloakService: KeycloakService) {
    this.keycloakInstance = this.keycloakService.getKeycloakInstance();
  }

  public async login(): Promise<void> {
    await this.keycloakService.login();
  }

  public logout(): void {
    this.keycloakService.logout(window.location.origin);
  }

  public getToken(): Promise<string> {
    return this.keycloakService.getToken();
  }

  public isLoggedIn(): boolean {
    return this.keycloakService.isLoggedIn();
  }

  public getUserProfile(): Promise<any> {
    return this.keycloakService.loadUserProfile();
  }

  public getRoles(): string[] {
    return this.keycloakService.getUserRoles();
  }

  public hasRole(role: string): boolean {
    return this.keycloakService.isUserInRole(role);
  }
}
