import {
  Component,
  effect,
  ElementRef,
  inject,
  Injectable,
  signal,
  viewChild,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class AuthDialogService {
  readonly destination = signal<string | null>(null);
  open(returnUrl = '/mon-espace') {
    this.destination.set(returnUrl);
  }
  close() {
    this.destination.set(null);
  }
}

@Component({
  selector: 'app-auth-dialog',
  imports: [RouterLink],
  templateUrl: './auth-dialog.html',
  styleUrl: './auth-dialog.scss',
})
export class AuthDialog {
  readonly auth = inject(AuthService);
  readonly state = inject(AuthDialogService);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly modal = viewChild<ElementRef<HTMLDialogElement>>('modal');

  constructor() {
    effect(() => {
      const destination = this.state.destination();
      const modal = this.modal()?.nativeElement;
      if (!modal) return;
      if (destination !== null && !modal.open) {
        this.error.set('');
        this.busy.set(false);
        modal.showModal();
      } else if (destination === null && modal.open) modal.close();
    });
  }

  close() {
    this.state.close();
  }

  dismissOutside(event: MouseEvent) {
    const modal = this.modal()?.nativeElement;
    if (event.target !== modal || !modal) return;
    const bounds = modal.getBoundingClientRect();
    if (
      event.clientX < bounds.left ||
      event.clientX > bounds.right ||
      event.clientY < bounds.top ||
      event.clientY > bounds.bottom
    )
      this.close();
  }

  async continue(register = false) {
    if (this.busy() || this.auth.problem()) return;
    this.busy.set(true);
    this.error.set('');
    try {
      const destination = this.state.destination() || '/mon-espace';
      if (register) await this.auth.register(destination);
      else await this.auth.login(destination);
    } catch {
      this.error.set('La connexion n’a pas pu démarrer. Veuillez réessayer.');
    } finally {
      this.busy.set(false);
    }
  }
}
