import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { API } from '../pages/reservation/reservation.config';
import { Reservation, ReservationRequest } from '../models/reservation';
import { CircuitService } from './circuit.service';
import { GuideService } from './guide.service';

@Injectable({ providedIn: 'root' })
export class ReservationService {
  private readonly http = inject(HttpClient);
  private readonly circuits = inject(CircuitService);
  private readonly guides = inject(GuideService);

  getCircuits() {
    return this.circuits.list();
  }
  getGuide(id: number) {
    return this.guides.get(id);
  }

  creer(request: ReservationRequest) {
    return this.http.post<Reservation>(API.reservations, request, { timeout: 10000 });
  }

  lister(email?: string) {
    let params = new HttpParams();
    if (email) params = params.set('email', email);
    return this.http.get<Reservation[]>(API.reservations, { params });
  }

  annuler(id: number) {
    return this.http.patch<Reservation>(`${API.reservations}/${id}/annuler`, {});
  }
}
