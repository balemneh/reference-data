import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OAuthService } from 'angular-oauth2-oidc';
import { MarkdownModule } from 'ngx-markdown';
import { UserGuideComponent } from './user-guide';
import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('UserGuideComponent', () => {
  let component: UserGuideComponent;
  let fixture: ComponentFixture<UserGuideComponent>;

  beforeEach(async () => {
    const oauthServiceSpy = jasmine.createSpyObj('OAuthService', ['getIdentityClaims']);
    oauthServiceSpy.getIdentityClaims.and.returnValue({ name: 'Test User', 'preferred_username': 'consumer' });

    await TestBed.configureTestingModule({
      imports: [UserGuideComponent, HttpClientTestingModule, MarkdownModule.forRoot({ loader: HttpClient })],
      providers: [
        { provide: OAuthService, useValue: oauthServiceSpy }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UserGuideComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
