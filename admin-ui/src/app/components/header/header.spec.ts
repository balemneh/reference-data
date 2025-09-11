import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HeaderComponent } from './header';
import { RouterTestingModule } from '@angular/router/testing';

describe('HeaderComponent (a11y + state)', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HeaderComponent, RouterTestingModule]
    }).compileComponents();
  });

  function getLiveRegion(el: HTMLElement): HTMLElement | null {
    return el.querySelector('[aria-live="polite"]');
  }

  it('toggles Help menu aria-expanded and announces', fakeAsync(() => {
    const fixture = TestBed.createComponent(HeaderComponent);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;

    const helpBtn = el.querySelector<HTMLButtonElement>('button[aria-controls="help-dropdown"]')!;
    expect(helpBtn.getAttribute('aria-expanded')).toBeNull();

    helpBtn.click();
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(helpBtn.getAttribute('aria-expanded')).toBe('true');
    const live = getLiveRegion(el);
    expect(live?.textContent).toContain('Help menu opened');

    // Close
    helpBtn.click();
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
    expect(helpBtn.getAttribute('aria-expanded')).toBeNull();
    expect(live?.textContent).toContain('Help menu closed');
  }));

  it('toggles Notifications and Account menus and announces', fakeAsync(() => {
    const fixture = TestBed.createComponent(HeaderComponent);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;

    const notifBtn = el.querySelector<HTMLButtonElement>('button[aria-controls="notification-dropdown"]')!;
    const acctBtn = el.querySelector<HTMLButtonElement>('button[aria-controls="user-dropdown"]')!;

    notifBtn.click();
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
    expect(notifBtn.getAttribute('aria-expanded')).toBe('true');

    // Opening account should close notifications
    acctBtn.click();
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
    expect(acctBtn.getAttribute('aria-expanded')).toBe('true');
    expect(notifBtn.getAttribute('aria-expanded')).toBeNull();
    const live = getLiveRegion(el);
    expect(live?.textContent).toContain('Account menu opened');
  }));

  it('closes all menus when overlay clicked and announces', fakeAsync(() => {
    const fixture = TestBed.createComponent(HeaderComponent);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;

    // Open a couple of menus
    el.querySelector<HTMLButtonElement>('button[aria-controls="help-dropdown"]')!.click();
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
    el.querySelector<HTMLButtonElement>('button[aria-controls="user-dropdown"]')!.click();
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    // Click overlay
    const overlay = el.querySelector<HTMLElement>('.menu-overlay')!;
    overlay.click();
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(el.querySelector('button[aria-controls="help-dropdown"]')!.getAttribute('aria-expanded')).toBeNull();
    expect(el.querySelector('button[aria-controls="user-dropdown"]')!.getAttribute('aria-expanded')).toBeNull();
    const live = getLiveRegion(el);
    expect(live?.textContent).toContain('Menus closed');
  }));
});

