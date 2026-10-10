import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { EnumLabelPipe } from '../../shared/enum-label.pipe';
import { RESOURCE_TYPES } from './resource.models';
import { ResourceService } from './resource.service';

@Component({
  selector: 'app-resource-form',
  imports: [EnumLabelPipe, MatButtonModule, MatFormFieldModule, MatInputModule, ReactiveFormsModule, RouterLink],
  templateUrl: './resource-form.html',
  styleUrl: '../../shared/form-page.scss',
})
export class ResourceForm {
  private readonly resourceService = inject(ResourceService);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(NonNullableFormBuilder);

  protected readonly types = RESOURCE_TYPES;

  protected readonly form = this.formBuilder.group({
    callSign: ['', [Validators.required, Validators.maxLength(64)]],
    type: ['', Validators.required],
    latitude: [null as number | null, [Validators.required, Validators.min(-90), Validators.max(90)]],
    longitude: [null as number | null, [Validators.required, Validators.min(-180), Validators.max(180)]],
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

    this.resourceService
      .create({
        callSign: value.callSign.trim().toUpperCase(),
        type: value.type,
        latitude: value.latitude as number,
        longitude: value.longitude as number,
      })
      .subscribe({
        next: () => void this.router.navigate(['/resources']),
        error: (failure: unknown) => {
          this.submitting.set(false);
          this.error.set(
            failure instanceof HttpErrorResponse && typeof failure.error?.detail === 'string'
              ? failure.error.detail
              : 'The resource could not be added. Check the form and try again.',
          );
        },
      });
  }
}
