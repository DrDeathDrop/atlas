import { HttpErrorResponse } from '@angular/common/http';
import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, inject, input, output, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Observable } from 'rxjs';
import { EnumLabelPipe } from '../../shared/enum-label.pipe';
import { MapPoint } from '../../shared/map/map.models';
import { FACILITY_TYPES, ZONE_TYPES } from './map-feature.models';
import { MapFeatureService } from './map-feature.service';

export type EditorMode = 'zone' | 'closure' | 'facility';

const POINTS_NEEDED: Record<EditorMode, number> = { zone: 3, closure: 2, facility: 1 };

const HINTS: Record<EditorMode, string> = {
  zone: 'Click the map to mark each corner of the zone. At least three corners are needed.',
  closure: 'Click the map along the closed stretch of road. At least two points are needed.',
  facility: 'Click the map where the facility is.',
};

@Component({
  selector: 'app-map-editor',
  imports: [
    EnumLabelPipe,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    NgTemplateOutlet,
    ReactiveFormsModule,
  ],
  templateUrl: './map-editor.html',
  styleUrl: './map-editor.scss',
})
export class MapEditor {
  private readonly features = inject(MapFeatureService);
  private readonly formBuilder = inject(NonNullableFormBuilder);

  readonly mode = input.required<EditorMode>();
  readonly points = input<MapPoint[]>([]);

  readonly undo = output<void>();
  readonly dismiss = output<void>();
  readonly saved = output<void>();

  protected readonly zoneTypes = ZONE_TYPES;
  protected readonly facilityTypes = FACILITY_TYPES;

  protected readonly zoneForm = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    type: ['EVACUATION', Validators.required],
  });

  protected readonly closureForm = this.formBuilder.group({
    roadName: ['', [Validators.required, Validators.maxLength(120)]],
    reason: ['', Validators.maxLength(255)],
  });

  protected readonly facilityForm = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    type: ['HOSPITAL', Validators.required],
    address: ['', Validators.maxLength(255)],
    capacity: [null as number | null, [Validators.required, Validators.min(0), Validators.max(100000)]],
  });

  protected readonly error = signal<string | null>(null);
  protected readonly submitting = signal(false);

  protected readonly hint = computed(() => HINTS[this.mode()]);
  protected readonly enoughPoints = computed(() => this.points().length >= POINTS_NEEDED[this.mode()]);

  protected submit(): void {
    const form = this.currentForm();
    if (form.invalid) {
      form.markAllAsTouched();
      return;
    }
    if (!this.enoughPoints()) {
      this.error.set(this.hint());
      return;
    }
    if (this.submitting()) {
      return;
    }

    this.error.set(null);
    this.submitting.set(true);

    this.save().subscribe({
      next: () => {
        this.submitting.set(false);
        this.saved.emit();
      },
      error: (failure: unknown) => {
        this.submitting.set(false);
        this.error.set(
          failure instanceof HttpErrorResponse && typeof failure.error?.detail === 'string'
            ? failure.error.detail
            : 'This could not be saved. Check the form and try again.',
        );
      },
    });
  }

  private currentForm() {
    switch (this.mode()) {
      case 'zone':
        return this.zoneForm;
      case 'closure':
        return this.closureForm;
      case 'facility':
        return this.facilityForm;
    }
  }

  private save(): Observable<unknown> {
    const points = this.points();

    switch (this.mode()) {
      case 'zone': {
        const value = this.zoneForm.getRawValue();
        return this.features.createZone({ name: value.name.trim(), type: value.type, boundary: points });
      }
      case 'closure': {
        const value = this.closureForm.getRawValue();
        return this.features.createRoadClosure({
          roadName: value.roadName.trim(),
          reason: value.reason.trim() || null,
          path: points,
        });
      }
      case 'facility': {
        const value = this.facilityForm.getRawValue();
        return this.features.createFacility({
          name: value.name.trim(),
          type: value.type,
          address: value.address.trim() || null,
          latitude: points[0].latitude,
          longitude: points[0].longitude,
          capacity: value.capacity as number,
        });
      }
    }
  }
}
