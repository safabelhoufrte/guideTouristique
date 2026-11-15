
import {
  ApplicationConfig,
  LOCALE_ID,
  provideBrowserGlobalErrorListeners
} from '@angular/core';

import {
  registerLocaleData
} from '@angular/common';

import localeFr from '@angular/common/locales/fr';

import {
  provideRouter,
  withInMemoryScrolling
} from '@angular/router';

import {
  provideHttpClient,
  withInterceptors
} from '@angular/common/http';

import { routes } from './app.routes';

import { guideAuthInterceptor } from
  './auth/guide-auth.interceptor';

registerLocaleData(localeFr, 'fr-MA');

export const appConfig: ApplicationConfig = {
  providers: [
    {
      provide: LOCALE_ID,
      useValue: 'fr-MA'
    },

    provideBrowserGlobalErrorListeners(),

    provideRouter(
      routes,
      withInMemoryScrolling({
        scrollPositionRestoration: 'enabled'
      })
    ),

    provideHttpClient(
      withInterceptors([
        guideAuthInterceptor
      ])
    )
  ]
};
