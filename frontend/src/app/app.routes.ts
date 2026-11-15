import { Routes } from '@angular/router';
import { ReservationComponent } from './pages/reservation/reservation.component';
import { accountRedirect, roleGuard } from './auth/auth.guard';

export const routes: Routes = [
  {
    path: 'circuits',
    loadComponent: () =>
      import('./pages/circuits/circuit-list').then((m) => m.CircuitList),
    title: 'Nos circuits | Guide Touristique',
  },
  {
    path: 'circuits/:id',
    loadComponent: () =>
      import('./pages/circuits/circuit-detail').then((m) => m.CircuitDetail),
    title: 'Détail du circuit | Guide Touristique',
  },
  {
    path: 'reservations',
    component: ReservationComponent,
  },
  {
    path: 'guides',
    loadComponent: () =>
      import('./pages/spaces/guide-directory').then((m) => m.GuideDirectory),
  },
  {
    path: 'avis',
    loadComponent: () =>
      import('./pages/spaces/reviews').then((m) => m.Reviews),
  },
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () =>
      import('./auth/landing').then((m) => m.Landing),
    title: 'Découvrez le Maroc | Guide Touristique',
  },


  // Keycloak: same login routes as main2.
  {
    path: 'connexion',
    loadComponent: () => import('./auth/login').then((m) => m.Login),
    title: 'Connexion | Guide Touristique',
  },
  {
    path: 'acces-refuse',
    loadComponent: () => import('./auth/forbidden').then((m) => m.Forbidden),
  },
  {
    path: 'mon-espace',
    canActivate: [accountRedirect],
    loadComponent: () => import('./auth/login').then((m) => m.Login),
  },
  {
    // Old local login address: send people to the Keycloak login.
    path: 'connexion-guide',
    redirectTo: 'connexion',
  },
  {
    path: 'espace-guide',
    canActivate: [roleGuard],
    data: { roles: ['GUIDE'] },
    loadComponent: () =>
      import('./pages/guide/dashboard/dashboard').then(
        m => m.GuideDashboard
      ),
    title: 'Mon espace Guide | Guide Touristique'
  },

  
  // Toujours garder cette route à la fin
  {
    path: '**',
    loadComponent: () =>
      import('./pages/unavailable').then((m) => m.Unavailable),
    title: 'Page introuvable | Guide Touristique',
  },
];
