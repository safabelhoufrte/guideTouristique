import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Circuit } from '../../../models/circuit';
import { Avis } from '../../../services/workspace.service';
import { AuthService } from '../../../auth/auth.service';
import { GuideProfile, GuideProfileUpdate, GuideWorkspaceService } from '../../../services/guide-workspace.service';

type Page = 'dashboard' | 'profil' | 'circuits' | 'reservations' | 'avis';

@Component({
  selector: 'app-guide-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class GuideDashboard implements OnInit {
  private workspace = inject(GuideWorkspaceService);
  private auth = inject(AuthService);
  // The app is zoneless: values changed inside subscribe() must be signalled to Angular,
  // otherwise the screen stays on "Chargement de votre profil…" until the next click.
  private cdr = inject(ChangeDetectorRef);

  page: Page = 'dashboard';
  loading = true;
  errorMessage = '';
  guide: GuideProfile | null = null;
  circuits: Circuit[] = [];
  avis: Avis[] = [];
  circuitsLoading = false;
  avisLoading = false;
  circuitsError = '';
  avisError = '';
  edit = false;
  saving = false;
  saveMessage = '';
  form: GuideProfileUpdate = { nom:'', prenom:'', ville:'', langues:[], email:'', telephone:'', tarifParJour:0 };
  
    afficherAjoutLangue = false;
    langueSelectionnee = '';

    languesDisponibles: string[] = [
    'Français',
    'Anglais',
    'Arabe',
    'Amazigh',
    'Espagnol',
    'Allemand',
    'Italien',
    'Portugais',
    'Chinois',
    'Japonais',
    'Russe',
    'Turc',
    'Néerlandais',
    'Hindi',
    'Coréen',
    'Suédois'
    ];

    get languesAjoutables(): string[] {
    return this.languesDisponibles.filter(langue =>
        !this.form.langues.some(
        existante =>
            existante.toLowerCase() === langue.toLowerCase()
        )
    );
    }

    ajouterLangue(): void {
    const langue = this.langueSelectionnee;

    if (!this.edit || !langue ||
        !this.languesAjoutables.includes(langue)) {
        return;
    }

    this.form.langues = [...this.form.langues, langue];
    this.langueSelectionnee = '';
    this.afficherAjoutLangue = false;
    }

    supprimerLangue(langue: string): void {
    if (!this.edit) return;

    this.form.langues = this.form.langues.filter(
        l => l !== langue
    );
    }
  circuitFilter = '';

  ngOnInit(): void { this.reload(); }

  reload(): void {
    this.loading = true;
    this.errorMessage = '';
    this.workspace.me().subscribe({
      next: guide => {
        this.guide = guide;
        this.loading = false;
        this.loadCircuits(guide.id);
        this.loadAvis(guide.id);
        this.cdr.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        this.loading = false;
        if (error.status === 401) {
          // Session expired: back to the Keycloak login page.
          void this.auth.login('/espace-guide');
          return;
        }
        this.errorMessage =
          error.status === 404
            ? 'Aucun profil guide n’est encore lié à votre compte. Demandez à un administrateur de le relier.'
            : error.status === 403
              ? 'Ce compte n’a pas le rôle Guide.'
              : `Profil indisponible (HTTP ${error.status || 'réseau'}). Vérifiez le service Guide (port 8081).`;
        this.cdr.markForCheck();
      }
    });
  }

  get pageTitle(): string {
    return {dashboard:'Tableau de bord',profil:'Mon profil',circuits:'Mes circuits',reservations:'Réservations',avis:'Mes avis'}[this.page];
  }

  select(page: Page): void { this.page = page; this.saveMessage = ''; }

  loadCircuits(guideId: number): void {
    this.circuitsLoading = true; this.circuitsError = '';
    this.workspace.circuits(guideId).subscribe({
      next: rows => { this.circuits = rows.filter(c => c.guideId === guideId); this.circuitsLoading = false; this.cdr.markForCheck(); },
      error: () => { this.circuitsLoading = false; this.circuitsError = 'Service Circuit inaccessible : les circuits ne peuvent pas être affichés.'; this.cdr.markForCheck(); }
    });
  }

  loadAvis(guideId: number): void {
    this.avisLoading = true; this.avisError = '';
    this.workspace.publicReviews().subscribe({
      next: rows => { this.avis = rows.filter(a => a.guideId === guideId && a.publie); this.avisLoading = false; this.cdr.markForCheck(); },
      error: () => { this.avisLoading = false; this.avisError = 'Service Avis inaccessible : aucun avis chargé.'; this.cdr.markForCheck(); }
    });
  }

  get visibleCircuits(): Circuit[] {
    const text = this.circuitFilter.trim().toLocaleLowerCase();
    return this.circuits.filter(c => !text || (c.titre + ' ' + (c.ville ?? '')).toLocaleLowerCase().includes(text));
  }
  get initiale(): string { return (this.guide?.prenom || this.guide?.nom || 'G').charAt(0).toUpperCase(); }
  get fullName(): string { return [this.guide?.prenom, this.guide?.nom].filter(Boolean).join(' ') || 'Guide'; }
  get noteMoyenne(): string {
    if (!this.avis.length) return '—';
    return (this.avis.reduce((s,a) => s + a.noteGuide,0)/this.avis.length).toFixed(1);
  }

  startEdit(): void {
    if (!this.guide) return;
    const g = this.guide;
    this.form = {nom:g.nom, prenom:g.prenom, ville:g.ville, langues:[...g.langues], email:g.email,
      telephone:g.telephone, tarifParJour:g.tarifParJour};
    this.afficherAjoutLangue = false;
    this.langueSelectionnee = '';
    this.edit = true; this.saveMessage = '';
  }
  cancelEdit(): void { this.edit = false; this.saveMessage = ''; }

  saveProfile(): void {
    if (this.saving || !this.form.nom.trim() || this.form.tarifParJour < 0) return;
    this.saving = true; this.saveMessage = '';
    const body: GuideProfileUpdate = {
      ...this.form,
      nom:this.form.nom.trim(),
      prenom:this.form.prenom?.trim() || null,
      ville:this.form.ville?.trim() || null,
      email:this.form.email?.trim() || null,
      telephone:this.form.telephone?.trim() || null,
      langues: [...this.form.langues]
    };
    this.workspace.updateMe(body).subscribe({
      next: g => { this.guide = g; this.saving = false; this.edit = false; this.saveMessage = 'Profil enregistré avec succès.'; this.cdr.markForCheck(); },
      error: (e: HttpErrorResponse) => { this.saving = false; this.saveMessage = `Modification refusée (HTTP ${e.status || 'réseau'}). Vérifiez les champs et l'API.`; this.cdr.markForCheck(); }
    });
  }

  logout(): void { void this.auth.logout(); }
}
