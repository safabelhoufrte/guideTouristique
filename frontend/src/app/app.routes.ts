import { Routes } from '@angular/router';
import { ReservationComponent } from './pages/reservation/reservation.component';
import { roleGuard, accountRedirect } from './auth/auth.guard';

export const routes: Routes = [
  {
    path: 'circuits',
    loadComponent: () => import('./pages/circuits/circuit-list').then((m) => m.CircuitList),
    title: 'Nos circuits | Guide Touristique',
  },
  {
    path: 'circuits/:id',
    loadComponent: () => import('./pages/circuits/circuit-detail').then((m) => m.CircuitDetail),
    title: 'Détail du circuit | Guide Touristique',
  },
  {
    path: 'reservations',
    component: ReservationComponent,
    canActivate: [roleGuard],
    data: { roles: ['USER'] },
  },
    {
    path: 'mes-reservations',
    canActivate: [roleGuard],
    data: { roles: ['USER'] },
    loadComponent: () =>
      import('./pages/reservation/mes-reservations').then((m) => m.MesReservations),
    title: 'Mes réservations | Guide Touristique',
  },
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
    // Protected entry point only (role USER). The Reservation team builds the content.
    path: 'espace-touriste',
    canActivate: [roleGuard],
    data: { roles: ['USER'] },
    children: [
      {
        path: '',
        title: 'Espace Touriste | Guide Touristique',
        loadComponent: () => import('./pages/spaces/tourist-space').then((m) => m.TouristSpace),
      },
      {
        path: 'avis',
        loadComponent: () => import('./pages/spaces/reviews').then((m) => m.Reviews),
        data: { scope: 'me' },
      },
      {
        path: 'avis/nouveau/:id',
        loadComponent: () => import('./pages/spaces/review-form').then((m) => m.ReviewForm),
      },
    ],
  },
  {
    // Guide dashboard (Safa): profile, circuits, reservations, reviews in one page.
    path: 'espace-guide',
    canActivate: [roleGuard],
    data: { roles: ['GUIDE'] },
    loadComponent: () =>
      import('./pages/guide/dashboard/dashboard').then((m) => m.GuideDashboard),
    title: 'Mon espace Guide | Guide Touristique',
  },
  {
    // Old local guide login address: Keycloak login now.
    path: 'connexion-guide',
    redirectTo: 'connexion',
  },
  {
    path: 'administration',
    canActivate: [roleGuard],
    data: { roles: ['ADMIN'], space: 'admin' },
    loadComponent: () => import('./pages/spaces/space-shell').then((m) => m.SpaceShell),
    children: [
      {
        path: 'promos',
        loadComponent: () => import('./pages/spaces/admin-promos').then((m) => m.AdminPromos),
      },
      {
        path: 'analyses',
        redirectTo: '',
        pathMatch: 'full',
      },
      {
        path: '',
        title: 'Dashboard | Guide Touristique',
        loadComponent: () => import('./pages/spaces/admin-home').then((m) => m.AdminHome),
      },
      {
        path: 'circuits',
        loadComponent: () => import('./pages/spaces/admin-circuits').then((m) => m.AdminCircuits),
      },
      {
        path: 'circuits/nouveau',
        loadComponent: () => import('./pages/spaces/circuit-editor').then((m) => m.CircuitEditor),
      },
      {
        path: 'circuits/:id',
        loadComponent: () => import('./pages/spaces/circuit-editor').then((m) => m.CircuitEditor),
      },
      {
        path: 'guides',
        loadComponent: () => import('./pages/spaces/admin-guides').then((m) => m.AdminGuides),
      },
      {
        path: 'guides/nouveau',
        loadComponent: () => import('./pages/spaces/guide-editor').then((m) => m.GuideEditor),
      },
      {
        path: 'guides/:id',
        loadComponent: () => import('./pages/spaces/guide-editor').then((m) => m.GuideEditor),
      },
      {
        path: 'reservations',
        loadComponent: () =>
          import('./pages/spaces/admin-reservations').then((m) => m.AdminReservations),
        data: { scope: 'admin' },
      },
      {
        path: 'avis',
        loadComponent: () => import('./pages/spaces/reviews').then((m) => m.Reviews),
        data: { scope: 'admin' },
      },
    ],
  },
  {
    path: 'avis',
    loadComponent: () => import('./pages/spaces/reviews').then((m) => m.Reviews),
    data: { scope: 'public' },
  },
  {
    path: 'guides',
    loadComponent: () => import('./pages/spaces/guide-directory').then((m) => m.GuideDirectory),
  },
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./auth/landing').then((m) => m.Landing),
    title: 'Découvrez le Maroc | Guide Touristique',
  },
  {
    path: '**',
    loadComponent: () => import('./pages/unavailable').then((m) => m.Unavailable),
    title: 'Guide Touristique',
  },
];
