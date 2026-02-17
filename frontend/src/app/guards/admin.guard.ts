import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';

@Injectable({
  providedIn: 'root'
})
export class AdminGuard implements CanActivate {
  constructor(
    private keycloakService: KeycloakService,
    private router: Router
  ) {}

  canActivate(): boolean {
    const roles = this.keycloakService.getUserRoles();
    if (roles.includes('ADMIN')) {
      return true;
    } else {
      this.router.navigate(['/']);
      return false;
    }
  }
}
