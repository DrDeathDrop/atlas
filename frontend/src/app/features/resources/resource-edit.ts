import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { EnumLabelPipe } from '../../shared/enum-label.pipe';
import { MapView } from '../../shared/map/map-view';
import { MapPoint } from '../../shared/map/map.models';
import { Resource } from './resource.models';
import { ResourceService } from './resource.service';

@Component({
  selector: 'app-resource-edit',
  imports: [
    EnumLabelPipe,
    MapView,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
    ReactiveFormsModule,
    RouterLink,
  ],
  templateUrl: './resource-edit.html',
  styleUrl: '../../shared/form-page.scss',
})
export class ResourceEdit implements OnInit {
  private readonly resourceService = inject(ResourceService);
  private readonly formBuilder = inject(NonNullableFormBuilder);

  readonly id = input.required<string>();

  protected readonly form = this.formBuilder.group({
    latitude: [null as number | null, [Validators.required, Validators.min(-90), Validators.max(90)]],
    longitude: [null as number | null, [Validators.required, Validators.min(-180), Validators.max(180)]],
  });

  protected readonly resource = signal<Resource | null>(null);
  protected readonly loading = signal(true);
  protected readonly notFound = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly saved = signal<string | null>(null);

  private readonly values = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });

  protected readonly location = computed<MapPoint | null>(() => {
    const { latitude, longitude } = this.values();
    const valid =
      typeof latitude === 'number' &&
      typeof longitude === 'number' &&
      Math.abs(latitude) <= 90 &&
      Math.abs(longitude) <= 180;
    return valid ? { latitude, longitude } : null;
  });

  protected readonly onAssignment = computed(() => {
    const status = this.resource()?.status;
    return status === 'EN_ROUTE' || status === 'ON_SCENE';
  });

  protected readonly inService = computed(() => this.resource()?.status !== 'OUT_OF_SERVICE');

  ngOnInit(): void {
    this.resourceService.get(this.id()).subscribe({
      next: (resource) => {
        this.show(resource);
        this.loading.set(false);
      },
      error: () => {
        this.notFound.set(true);
        this.loading.set(false);
      },
    });
  }

  protected placeAt(point: MapPoint): void {
    this.form.patchValue({ latitude: point.latitude, longitude: point.longitude });
    this.form.markAsDirty();
  }

  protected saveLocation(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.saving()) {
      return;
    }

    const value = this.form.getRawValue();
    this.begin();

    this.resourceService
      .updateLocation(this.id(), value.latitude as number, value.longitude as number)
      .subscribe({
        next: (resource) => {
          this.show(resource);
          this.finish('Location saved.');
        },
        error: (failure: unknown) => this.fail(failure),
      });
  }

  protected toggleService(): void {
    if (this.saving() || this.onAssignment()) {
      return;
    }

    const inService = !this.inService();
    this.begin();

    this.resourceService.setInService(this.id(), inService).subscribe({
      next: (resource) => {
        this.resource.set(resource);
        this.finish(inService ? 'Returned to service.' : 'Taken out of service.');
      },
      error: (failure: unknown) => this.fail(failure),
    });
  }

  private show(resource: Resource): void {
    this.resource.set(resource);
    this.form.reset({ latitude: resource.latitude, longitude: resource.longitude });
  }

  private begin(): void {
    this.error.set(null);
    this.saved.set(null);
    this.saving.set(true);
  }

  private finish(message: string): void {
    this.saving.set(false);
    this.saved.set(message);
  }

  private fail(failure: unknown): void {
    this.saving.set(false);
    this.error.set(
      failure instanceof HttpErrorResponse && typeof failure.error?.detail === 'string'
        ? failure.error.detail
        : 'The change could not be saved. Try again.',
    );
  }
}
