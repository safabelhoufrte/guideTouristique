import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '../../auth/auth.service';
import { TouristProfile } from './tourist-profile';

describe('Tourist profile', () => {
  const auth = {
    name: signal('Sara Alami'),
    email: signal('touriste@demo.example'),
    account: signal({
      firstName: 'Sara',
      lastName: 'Alami',
      username: 'demo-touriste',
      emailVerified: true,
    }),
    accountAction: signal<{ action: string; status: string } | null>(null),
    manageAccount: vi.fn().mockResolvedValue(undefined),
    logout: vi.fn(),
  };

  beforeEach(() => {
    auth.manageAccount.mockClear();
    auth.accountAction.set(null);
    TestBed.configureTestingModule({ providers: [{ provide: AuthService, useValue: auth }] });
  });

  function render() {
    const fixture = TestBed.createComponent(TouristProfile);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('shows the account read from the Keycloak token', () => {
    const page = render();
    expect(page.querySelector('.avatar')?.textContent?.trim()).toBe('SA');
    expect(page.textContent).toContain('touriste@demo.example');
    expect(page.textContent).toContain('demo-touriste');
    expect(page.textContent).toContain('vérifiée');
  });

  it('sends profile and password changes to Keycloak and comes back to the profile', () => {
    const page = render();
    const buttons = [...page.querySelectorAll('button')];
    buttons.find((b) => b.textContent?.includes('Modifier mes informations'))!.click();
    buttons.find((b) => b.textContent?.includes('Changer mon mot de passe'))!.click();
    expect(auth.manageAccount).toHaveBeenNthCalledWith(
      1,
      'UPDATE_PROFILE',
      '/espace-touriste/profil',
    );
    expect(auth.manageAccount).toHaveBeenNthCalledWith(
      2,
      'UPDATE_PASSWORD',
      '/espace-touriste/profil',
    );
  });

  it('confirms a successful password change after the return from Keycloak', () => {
    auth.accountAction.set({ action: 'UPDATE_PASSWORD', status: 'success' });
    expect(render().querySelector('[role="status"]')?.textContent).toContain(
      'Votre mot de passe a bien été modifié.',
    );
  });
});
