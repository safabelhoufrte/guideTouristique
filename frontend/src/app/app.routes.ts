import { Routes } from '@angular/router';
import { ReservationComponent } from './pages/reservation/reservation.component';

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
  { path: 'reservations', component: ReservationComponent },
  { path: '', redirectTo: 'circuits', pathMatch: 'full' },
  {
    path: '**',
    loadComponent: () => import('./pages/unavailable').then((m) => m.Unavailable),
    title: 'Guide Touristique',
  },
];
