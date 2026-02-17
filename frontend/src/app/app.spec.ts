import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { OAuthService } from 'angular-oauth2-oidc';
import { App } from './app';
import { of } from 'rxjs';

describe('App', () => {
  let oauthService: jasmine.SpyObj<OAuthService>;

  beforeEach(async () => {
    const oauthServiceSpy = jasmine.createSpyObj('OAuthService', [
      'configure',
      'loadDiscoveryDocumentAndTryLogin',
      'setupAutomaticSilentRefresh',
      'tryLogin'
    ]);
    
    // Add the 'events' property to the spy object
    (oauthServiceSpy as any).events = of({});
    oauthServiceSpy.loadDiscoveryDocumentAndTryLogin.and.returnValue(Promise.resolve(true));

    await TestBed.configureTestingModule({
      imports: [App, RouterTestingModule],
      providers: [
        { provide: OAuthService, useValue: oauthServiceSpy }
      ]
    }).compileComponents();

    oauthService = TestBed.inject(OAuthService) as jasmine.SpyObj<OAuthService>;
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  // Keep DOM assertion minimal to avoid coupling to layout
  it('renders root component without errors', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled).toBeTruthy();
  });
});
