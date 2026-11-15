import { inject } from '@angular/core';
import {
  HttpInterceptorFn
} from '@angular/common/http';

import { GuideAuthService } from
  '../services/guide-auth.service';

export const guideAuthInterceptor: HttpInterceptorFn =
  (request, next) => {

    const auth = inject(GuideAuthService);
    const token = auth.getToken();

    const isPrivateGuideRequest =
      request.url === '/api/guides/me';

    if (token && isPrivateGuideRequest) {

      const securedRequest = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });

      return next(securedRequest);
    }

    return next(request);
  };
