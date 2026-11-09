import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LandingShowcase } from './landing-showcase';
@Component({
  selector: 'app-landing',
  imports: [RouterLink, LandingShowcase],
  templateUrl: './landing.html',
  styleUrl: './landing.scss',
})
export class Landing {}
