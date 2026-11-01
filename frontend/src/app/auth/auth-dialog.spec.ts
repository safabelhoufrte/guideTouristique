import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { AuthDialog, AuthDialogService } from './auth-dialog';
import { AuthService } from './auth.service';

describe('Sign-in dialog', () => {
  const auth = {
    authenticated: signal(false),
    problem: signal(''),
    login: vi.fn(async (_destination: string) => {}),
    register: vi.fn(async (_destination: string) => {}),
    home: () => '/espace-touriste',
  };

  beforeEach(() => {
    auth.login.mockReset();
    auth.register.mockReset();
    auth.problem.set('');
    TestBed.configureTestingModule({
      imports: [AuthDialog],
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    });
    // jsdom does not implement the browser's top-layer dialog methods.
    Object.defineProperty(HTMLDialogElement.prototype, 'showModal', {
      configurable: true,
      value: function (this: HTMLDialogElement) {
        this.setAttribute('open', '');
      },
    });
    Object.defineProperty(HTMLDialogElement.prototype, 'close', {
      configurable: true,
      value: function (this: HTMLDialogElement) {
        this.removeAttribute('open');
      },
    });
  });
  afterEach(() => {
    Reflect.deleteProperty(HTMLDialogElement.prototype, 'showModal');
    Reflect.deleteProperty(HTMLDialogElement.prototype, 'close');
  });

  it('keeps the protected destination for sign-in and registration', async () => {
    const fixture = TestBed.createComponent(AuthDialog);
    const state = TestBed.inject(AuthDialogService);
    state.open('/reservations?circuitId=10');
    fixture.detectChanges();
    await fixture.componentInstance.continue();
    expect(auth.login).toHaveBeenCalledWith('/reservations?circuitId=10');
    await fixture.componentInstance.continue(true);
    expect(auth.register).toHaveBeenCalledWith('/reservations?circuitId=10');
  });

  it('reports a failed redirect and lets the visitor retry', async () => {
    auth.login.mockRejectedValueOnce(new Error('Network unavailable'));
    const fixture = TestBed.createComponent(AuthDialog);
    TestBed.inject(AuthDialogService).open();
    fixture.detectChanges();
    await fixture.componentInstance.continue();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain(
      'réessayer',
    );
    expect(fixture.componentInstance.busy()).toBe(false);
    await fixture.componentInstance.continue();
    expect(auth.login).toHaveBeenCalledTimes(2);
    expect(fixture.componentInstance.error()).toBe('');
  });

  it('prevents duplicate redirects while a connection is starting', async () => {
    let finish!: () => void;
    auth.login.mockImplementationOnce(
      () =>
        new Promise<void>((resolve) => {
          finish = resolve;
        }),
    );
    const fixture = TestBed.createComponent(AuthDialog);
    TestBed.inject(AuthDialogService).open();
    fixture.detectChanges();
    const pending = fixture.componentInstance.continue();
    await fixture.componentInstance.continue(true);
    expect(auth.register).not.toHaveBeenCalled();
    finish();
    await pending;
  });

  it('keeps browsing available when authentication is unavailable', async () => {
    auth.problem.set('Connexion indisponible');
    const fixture = TestBed.createComponent(AuthDialog);
    const state = TestBed.inject(AuthDialogService);
    state.open();
    fixture.detectChanges();
    await fixture.componentInstance.continue();
    expect(auth.login).not.toHaveBeenCalled();
    const browse = fixture.nativeElement.querySelector('.browse-button') as HTMLButtonElement;
    expect(browse.disabled).toBe(false);
    browse.click();
    fixture.detectChanges();
    expect(state.destination()).toBeNull();
  });
});
