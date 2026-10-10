import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { EnumLabelPipe } from '../../shared/enum-label.pipe';
import { INCIDENT_CATEGORIES, SEVERITIES, Severity } from './incident.models';
import { IncidentService } from './incident.service';

@Component({
  selector: 'app-incident-form',
  imports: [EnumLabelPipe, MatButtonModule, MatFormFieldModule, MatInputModule, ReactiveFormsModule, RouterLink],
  templateUrl: './incident-form.html',
  styleUrl: '../../shared/form-page.scss',
})
export class IncidentForm {
  private readonly incidentService = inject(IncidentService);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(NonNullableFormBuilder);

  protected readonly categories = INCIDENT_CATEGORIES;
  protected readonly severities = SEVERITIES;

  protected readonly form = this.formBuilder.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    description: ['', [Validators.required, Validators.maxLength(5000)]],
    category: ['', Validators.required],
    severity: ['' as Severity | '', Validators.required],
    latitude: [null as number | null, [Validators.required, Validators.min(-90), Validators.max(90)]],
    longitude: [null as number | null, [Validators.required, Validators.min(-180), Validators.max(180)]],
    affectedPeople: [0, [Validators.required, Validators.min(0)]],
  });

  protected readonly error = signal<string | null>(null);
  protected readonly submitting = signal(false);

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.submitting()) {
      return;
    }

    this.error.set(null);
    this.submitting.set(true);

    const value = this.form.getRawValue();

    this.incidentService
      .report({
        title: value.title.trim(),
        description: value.description.trim(),
        category: value.category,
        severity: value.severity as Severity,
        latitude: value.latitude as number,
        longitude: value.longitude as number,
        affectedPeople: value.affectedPeople,
      })
      .subscribe({
        next: (incident) => void this.router.navigate(['/incidents', incident.id]),
        error: (failure: unknown) => {
          this.submitting.set(false);
          this.error.set(
            failure instanceof HttpErrorResponse && typeof failure.error?.detail === 'string'
              ? failure.error.detail
              : 'The incident could not be reported. Check the form and try again.',
          );
        },
      });
  }
}
