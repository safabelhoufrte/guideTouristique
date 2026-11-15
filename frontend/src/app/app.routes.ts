import { Routes } from '@angular/router';
import { ReservationComponent } from './pages/reservation/reservation.component';
import { guideAuthGuard } from './auth/guide-auth.guard';

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


  {
    path: 'connexion-guide',
    loadComponent: () =>
      import('./auth/guide-login').then(
        m => m.GuideLogin
      ),
    title: 'Connexion Guide | Guide Touristique'
  },
  {
    path: 'espace-guide',
    canActivate: [guideAuthGuard],
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
