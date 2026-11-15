import {
  ApplicationConfig,
  LOCALE_ID,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { AuthService } from './auth/auth.service';
import { authInterceptor } from './auth/auth.interceptor';
import { routes } from './app.routes';

registerLocaleData(localeFr, 'fr-MA');

export const appConfig: ApplicationConfig = {
  providers: [
    { provide: LOCALE_ID, useValue: 'fr-MA' },
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withInMemoryScrolling({ scrollPositionRestoration: 'enabled' })),
    // Keycloak: one interceptor adds the access token to every /api/... request.
    provideHttpClient(withInterceptors([authInterceptor])),
    // Restores the Keycloak session (silent check) before the first route is resolved.
    provideAppInitializer(() => inject(AuthService).init()),
  ],
};
