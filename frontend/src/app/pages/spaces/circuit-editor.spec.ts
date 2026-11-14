import { Component } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { CircuitEditor } from './circuit-editor';

@Component({ template: '' })
class Destination {}

describe('Circuit admin editor', () => {
  let http: HttpTestingController;
  let harness: RouterTestingHarness;
  const body = {
    titre: 'Kasbah',
    description: 'Visite de la Kasbah',
    ville: 'Agadir',
    dureeHeures: 3,
    prix: 0,
    guideId: 7,
  };
  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'administration/circuits/nouveau', component: CircuitEditor },
          { path: 'administration/circuits/:id', component: CircuitEditor },
          { path: 'administration/circuits', component: Destination },
        ]),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });
  afterEach(() => http.verify());
  async function createEditor() {
    const editor = await harness.navigateByUrl('/administration/circuits/nouveau', CircuitEditor);
    http.expectOne('/api/guides').flush([{ id: 7, nom: 'Guide test', disponible: true }]);
    editor.form.setValue(body);
    harness.detectChanges();
    return editor;
  }

  it('creates a free circuit once and navigates only after API success', async () => {
    const editor = await createEditor();
    editor.save();
    editor.save();
    const req = http.expectOne('/api/circuits');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    expect(editor.saving()).toBe(true);
    expect(TestBed.inject(Router).url).toContain('/nouveau');
    req.flush({ ...body, id: 12 });
    await vi.waitFor(() => expect(TestBed.inject(Router).url).toBe('/administration/circuits'));
  });

  it.each(['dureeHeures', 'prix', 'guideId'] as const)(
    'blocks submission when the numeric field %s is cleared',
    async (field) => {
      const editor = await createEditor();
      const input = harness.routeNativeElement!.querySelector(
        `[formControlName="${field}"]`,
      ) as HTMLInputElement;
      input.value = '';
      input.dispatchEvent(new Event(field === 'guideId' ? 'change' : 'input'));
      editor.save();
      expect(editor.form.controls[field].invalid).toBe(true);
      http.expectNone('/api/circuits');
    },
  );

  it('preserves all input after Guide is unavailable and allows retry', async () => {
    const editor = await createEditor();
    editor.save();
    http.expectOne('/api/circuits').flush({}, { status: 503, statusText: 'Unavailable' });
    harness.detectChanges();
    expect(editor.form.getRawValue()).toEqual(body);
    expect(editor.saving()).toBe(false);
    expect(harness.routeNativeElement!.querySelector('[role="alert"]')!.textContent).toContain(
      'momentanément indisponible',
    );
    editor.save();
    http.expectOne('/api/circuits').flush({ ...body, id: 12 });
    await vi.waitFor(() => expect(TestBed.inject(Router).url).toBe('/administration/circuits'));
  });

  it('does not offer an editable form when loading Guide fails', async () => {
    await harness.navigateByUrl('/administration/circuits/nouveau', CircuitEditor);
    http.expectOne('/api/guides').flush({}, { status: 503, statusText: 'Unavailable' });
    harness.detectChanges();
    expect(harness.routeNativeElement!.querySelector('form')).toBeNull();
    expect(harness.routeNativeElement!.querySelector('[role="alert"]')!.textContent).toContain(
      'momentanément indisponible',
    );
  });

  it('updates the existing circuit ID rather than creating another circuit', async () => {
    const editor = await harness.navigateByUrl('/administration/circuits/12', CircuitEditor);
    http.expectOne('/api/guides').flush([{ id: 7, nom: 'Guide test', disponible: true }]);
    http.expectOne('/api/circuits/12').flush({ ...body, id: 12 });
    editor.form.controls.titre.setValue('Kasbah modifiée');
    editor.save();
    const req = http.expectOne('/api/circuits/12');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ ...body, titre: 'Kasbah modifiée' });
    req.flush({ ...body, id: 12, titre: 'Kasbah modifiée' });
    await vi.waitFor(() => expect(TestBed.inject(Router).url).toBe('/administration/circuits'));
  });
});
