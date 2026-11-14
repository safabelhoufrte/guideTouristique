import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import Keycloak, { KeycloakConfig } from 'keycloak-js';
import { firstValueFrom } from 'rxjs';

export type Role = 'USER' | 'GUIDE' | 'ADMIN';
const roles: Role[] = ['USER', 'GUIDE', 'ADMIN'];

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private keycloak: Keycloak | null = null;
  readonly authenticated = signal(false);
  readonly roles = signal<Role[]>([]);
  readonly name = signal('');
  readonly email = signal('');
  readonly problem = signal('');

  async init() {
    try {
      const config = await firstValueFrom(
        this.http.get<KeycloakConfig>('/auth-config.json', { timeout: 5000 }),
      );
      this.keycloak = new Keycloak(config);
      this.keycloak.onAuthLogout = () => this.sync();
      this.keycloak.onAuthRefreshError = () => {
        this.keycloak?.clearToken();
        this.sync();
      };
      await this.keycloak.init({
        onLoad: 'check-sso',
        pkceMethod: 'S256',
        checkLoginIframe: false,
        messageReceiveTimeout: 5000,
        silentCheckSsoRedirectUri: location.origin + '/silent-check-sso.html',
        silentCheckSsoFallback: false,
      });
      this.sync();
    } catch {
      this.problem.set(
        'La connexion est momentanément indisponible. La consultation publique reste accessible.',
      );
    }
  }

  private sync() {
    const token = this.keycloak?.tokenParsed;
    this.authenticated.set(this.keycloak?.authenticated === true);
    this.roles.set(roles.filter((role) => token?.realm_access?.roles.includes(role)));
    this.name.set(token?.['name'] || token?.['preferred_username'] || 'Mon compte');
    this.email.set(token?.['email'] || '');
  }

  has(role: Role) {
    return this.authenticated() && this.roles().includes(role);
  }
  home() {
    return this.has('ADMIN')
      ? '/administration'
      : this.has('GUIDE')
        ? '/espace-guide'
        : this.has('USER')
          ? '/espace-touriste'
          : '/acces-refuse';
  }

  async accessToken(): Promise<string | undefined> {
    if (!this.keycloak?.authenticated) return undefined;
    try {
      await this.keycloak.updateToken(30);
      this.sync();
      return this.keycloak.token;
    } catch {
      this.keycloak.clearToken();
      this.sync();
      throw new Error('Votre session a expiré. Reconnectez-vous.');
    }
  }

  async login(returnUrl = '/mon-espace') {
    if (!this.keycloak || this.problem()) return;
    const safe =
      returnUrl.startsWith('/') && !returnUrl.startsWith('//') ? returnUrl : '/mon-espace';
    await this.keycloak.login({ redirectUri: location.origin + safe, locale: 'fr' });
  }
  async register(returnUrl = '/mon-espace') {
    if (!this.keycloak || this.problem()) return;
    const safe =
      returnUrl.startsWith('/') && !returnUrl.startsWith('//') ? returnUrl : '/mon-espace';
    await this.keycloak.register({ redirectUri: location.origin + safe, locale: 'fr' });
  }
  async logout() {
    await this.keycloak?.logout({ redirectUri: location.origin + '/circuits' });
  }
}
