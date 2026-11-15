import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, Role } from './auth.service';

export const roleGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.authenticated())
    return router.createUrlTree(['/connexion'], { queryParams: { retour: state.url } });
  const allowed = route.data['roles'] as Role[] | undefined;
  return (
    !allowed || allowed.some((role) => auth.has(role)) || router.createUrlTree(['/acces-refuse'])
  );
};

export const accountRedirect: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return router.createUrlTree([auth.authenticated() ? auth.home() : '/connexion']);
};
