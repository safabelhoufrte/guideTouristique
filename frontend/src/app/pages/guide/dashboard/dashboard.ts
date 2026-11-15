import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { GuideService } from '../../../services/guide.service';
import { CircuitService } from '../../../services/circuit.service';
import { Guide, Circuit } from '../../../models/circuit';

@Component({
  selector: 'app-guide-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class GuideDashboard {
  private readonly route = inject(ActivatedRoute);
  private readonly guideService = inject(GuideService);
  private readonly circuitService = inject(CircuitService);

  guideId = 0;
  guide: Guide | null = null;
  circuits: Circuit[] = [];

  loading = true;
  errorMessage = '';

  ngOnInit(): void {
    this.route.paramMap.subscribe({
      next: (params) => {
        const id = Number(params.get('guideId'));

        if (!Number.isInteger(id) || id <= 0) {
          this.loading = false;
          this.errorMessage = 'Identifiant du guide invalide.';
          return;
        }

        this.guideId = id;
        this.loadDashboard();
      },
    });
  }

  private loadDashboard(): void {
    this.loading = true;
    this.errorMessage = '';

    this.guideService.get(this.guideId).subscribe({
      next: (guide) => {
        this.guide = guide;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.errorMessage =
          'Impossible de récupérer les informations du guide. Vérifiez que le service Guide est démarré.';
      },
    });

    this.circuitService.list({ guideId: this.guideId }).subscribe({
      next: (circuits) => {
        this.circuits = circuits;
      },
      error: (error) => {
        console.error('Erreur de chargement des circuits :', error);
      },
    });
  }

  get totalCircuits(): number {
    return this.circuits.length;
  }

  get totalPrix(): number {
    return this.circuits.reduce((total, circuit) => total + circuit.prix, 0);
  }

  get statutGuide(): string {
    return this.guide?.disponible ? 'Disponible' : 'Indisponible';
  }

  get initiale(): string {
    return this.guide?.nom?.charAt(0).toUpperCase() || 'G';
  }
}
