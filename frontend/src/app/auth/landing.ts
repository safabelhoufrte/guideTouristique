import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from './auth.service';
import { AuthDialogService } from './auth-dialog';
import { LandingShowcase } from './landing-showcase';

@Component({
  selector: 'app-landing',
  imports: [RouterLink, LandingShowcase],
  templateUrl: './landing.html',
  styleUrl: './landing.scss',
})
export class Landing {
  readonly auth = inject(AuthService);
  readonly dialog = inject(AuthDialogService);
}
