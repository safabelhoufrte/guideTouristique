import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';

interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

@Injectable({
  providedIn: 'root'
})
export class GuideAuthService {

  private http = inject(HttpClient);
  private router = inject(Router);

  private readonly TOKEN_KEY = 'guide_token';

  login(email: string, password: string) {
    return this.http.post<LoginResponse>(
      '/api/guide-auth/login',
      { email, password }
    ).pipe(
      tap(response => {
        sessionStorage.setItem(
          this.TOKEN_KEY,
          response.accessToken
        );
      })
    );
  }

  getToken(): string | null {
    return sessionStorage.getItem(this.TOKEN_KEY);
  }

  isAuthenticated(): boolean {
    return this.getToken() !== null;
  }

  logout(): void {
    sessionStorage.removeItem(this.TOKEN_KEY);
    this.router.navigate(['/connexion-guide']);
  }
}
