import { Routes } from '@angular/router';
import { ReservationComponent } from './pages/reservation/reservation.component';

export const routes: Routes = [
  { path: 'reservations', component: ReservationComponent },
  { path: '', redirectTo: 'reservations', pathMatch: 'full' },
];