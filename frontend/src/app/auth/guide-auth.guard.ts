import { inject } from '@angular/core';
import {
  CanActivateFn,
  Router
} from '@angular/router';

import { GuideAuthService } from
  '../services/guide-auth.service';

export const guideAuthGuard: CanActivateFn = () => {

  const auth = inject(GuideAuthService);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree(['/connexion-guide']);
};
