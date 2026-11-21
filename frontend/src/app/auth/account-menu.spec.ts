import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AccountMenu } from './account-menu';
import { AuthService } from './auth.service';

describe('Traveller account menu', () => {
  const auth = {
    name: signal('Sara Alami'),
    email: signal('touriste@demo.example'),
    logout: vi.fn(),
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    });
  });

  it('shows the initials and opens a menu with the traveller pages', () => {
    const fixture = TestBed.createComponent(AccountMenu);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.avatar')?.textContent?.trim()).toBe('SA');
    expect(el.querySelector('[role="menu"]')).toBeNull();

    el.querySelector<HTMLButtonElement>('.avatar-button')!.click();
    fixture.detectChanges();
    const links = [...el.querySelectorAll('[role="menu"] a')].map((a) => a.getAttribute('href'));
    expect(links).toEqual([
      '/mes-reservations',
      '/espace-touriste/avis',
      '/espace-touriste/profil',
    ]);

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();
    expect(el.querySelector('[role="menu"]')).toBeNull();
  });
});
