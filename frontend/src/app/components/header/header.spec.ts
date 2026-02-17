import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';
import { OAuthService } from 'angular-oauth2-oidc';
import { HeaderComponent } from './header';
import { SearchService } from '../../services/search.service';
import { NotificationService } from '../../services/notification.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { KeycloakService } from 'keycloak-angular';
import { AuthService } from '../../services/auth.service';

class MockKeycloakService {
  isLoggedIn(): boolean {
    return true; // Simulate logged-in state for tests
  }
  
  logout(redirectUri?: string): void {
    // Mock logout logic
  }
  
  getToken(): Promise<string> {
    return Promise.resolve('mock-token');
  }
  
  loadUserProfile(): Promise<any> {
    return Promise.resolve({
      username: 'testuser',
      firstName: 'Test',
      lastName: 'User'
    });
  }
  
  getUserRoles(): string[] {
    return ['user'];
  }
  
  isUserInRole(role: string): boolean {
    return role === 'user';
  }

  getKeycloakInstance(): any {
    return {
      // Mock any properties or methods of the Keycloak instance if needed
      login: () => {},
      logout: () => {}
    };
  }
}

class MockAuthService {
  isLoggedIn(): boolean {
    return true; // Simulate logged-in state for tests
  }

  login(): Promise<void> {
    return Promise.resolve();
  }

  logout(): void {
    // Mock logout logic
  }

  getToken(): Promise<string> {
    return Promise.resolve('mock-token');
  }

  getUserProfile(): Promise<any> {
    return Promise.resolve({
      username: 'testuser',
      firstName: 'Test',
      lastName: 'User'
    });
  }

  getRoles(): string[] {
    return ['user'];
  }

  hasRole(role: string): boolean {
    return role === 'user';
  }
}

describe('HeaderComponent', () => {
  let component: HeaderComponent;
  let fixture: ComponentFixture<HeaderComponent>;
  let keycloakService: MockKeycloakService;
  let authService: MockAuthService;
  let router: Router;

  beforeEach(async () => {
    const searchServiceSpy = jasmine.createSpyObj('SearchService', ['getSearchSuggestions', 'searchGlobal']);
    Object.defineProperty(searchServiceSpy, 'searchHistory$', { value: of([]) });
    searchServiceSpy.getSearchSuggestions.and.returnValue(of([]));

    const notificationServiceSpy = jasmine.createSpyObj('NotificationService', ['getRecentNotifications']);
    notificationServiceSpy.getRecentNotifications.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [HeaderComponent, RouterTestingModule, HttpClientTestingModule],
      providers: [
        { provide: KeycloakService, useClass: MockKeycloakService },
        { provide: AuthService, useClass: MockAuthService },
        { provide: SearchService, useValue: searchServiceSpy },
        { provide: NotificationService, useValue: notificationServiceSpy },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(HeaderComponent);
    component = fixture.componentInstance;
    keycloakService = TestBed.inject(KeycloakService) as unknown as MockKeycloakService;
    authService = TestBed.inject(AuthService) as unknown as MockAuthService;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create and load user data on init', async () => {
    await fixture.whenStable();
    expect(component).toBeTruthy();
    expect(component.currentUser.name).toBe('Test User');
    expect(component.currentUser.initials).toBe('TU');
  });

  it('should toggle help menu', () => {
    expect(component.helpMenuOpen).toBeFalse();
    
    const allButtons = fixture.debugElement.queryAll(By.css('button.usa-button--outline'));
    const helpButton = allButtons.find(btn => btn.nativeElement.textContent.includes('Help'));
    
    expect(helpButton).toBeTruthy();

    // Open menu
    helpButton!.triggerEventHandler('click', null);
    fixture.detectChanges();

    expect(component.helpMenuOpen).toBeTrue();
    const dropdown = fixture.debugElement.query(By.css('.dropdown-menu.is-visible'));
    expect(dropdown).toBeTruthy();

    // Close menu
    helpButton!.triggerEventHandler('click', null);
    fixture.detectChanges();
    expect(component.helpMenuOpen).toBeFalse();
    const closedDropdown = fixture.debugElement.query(By.css('.dropdown-menu.is-visible'));
    expect(closedDropdown).toBeFalsy();
  });

  it('should call logout and navigate on sign out', () => {
    spyOn(router, 'navigate');
    spyOn(authService, 'logout').and.callThrough();
    const allButtons = fixture.debugElement.queryAll(By.css('button.usa-button--outline'));
    const signoutButton = allButtons.find(btn => btn.nativeElement.textContent.includes('Sign Out'));

    expect(signoutButton).toBeTruthy();

    signoutButton!.triggerEventHandler('click', null);
    
    expect(authService.logout).toHaveBeenCalled();
  });
});

