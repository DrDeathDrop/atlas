import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, input, output, signal, untracked } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { AuthService } from '../../core/auth/auth.service';
import { EnumLabelPipe } from '../../shared/enum-label.pipe';
import { Assignment, NearbyResource } from '../resources/resource.models';
import { ResourceService } from '../resources/resource.service';
import { Incident } from './incident.models';

@Component({
  selector: 'app-incident-dispatch',
  imports: [DatePipe, EnumLabelPipe, MatButtonModule, MatButtonToggleModule],
  templateUrl: './incident-dispatch.html',
  styleUrl: './incident-dispatch.scss',
})
export class IncidentDispatch {
  private readonly resourceService = inject(ResourceService);
  private readonly auth = inject(AuthService);

  readonly incident = input.required<Incident>();
  readonly changed = output<void>();

  protected readonly radiusOptions = [5, 15, 50];
  protected readonly radiusKm = signal(15);
  protected readonly assignments = signal<Assignment[]>([]);
  protected readonly nearby = signal<NearbyResource[] | null>(null);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly mayDispatch = computed(() => this.auth.hasRole('ADMIN', 'DISPATCHER'));
  protected readonly acceptsResources = computed(() => {
    const status = this.incident().status;
    return status === 'VERIFIED' || status === 'ACTIVE';
  });
  protected readonly showsNearby = computed(() => this.mayDispatch() && this.acceptsResources());

  constructor() {
    effect(() => {
      this.incident();
      untracked(() => this.load());
    });
  }

  protected chooseRadius(radiusKm: number): void {
    this.radiusKm.set(radiusKm);
    this.loadNearby();
  }

  protected dispatch(resourceId: string): void {
    this.run(() => this.resourceService.dispatch(this.incident().id, resourceId), 'The resource could not be assigned.');
  }

  protected release(assignmentId: string): void {
    this.run(() => this.resourceService.release(assignmentId), 'The resource could not be released.');
  }

  protected kilometres(meters: number): string {
    return (meters / 1000).toFixed(1);
  }

  private run(action: () => ReturnType<ResourceService['dispatch']>, fallback: string): void {
    if (this.busy()) {
      return;
    }

    this.busy.set(true);
    this.error.set(null);

    action().subscribe({
      next: () => {
        this.busy.set(false);
        this.changed.emit();
      },
      error: (failure: unknown) => {
        this.busy.set(false);
        this.error.set(
          failure instanceof HttpErrorResponse && typeof failure.error?.detail === 'string'
            ? failure.error.detail
            : fallback,
        );
        this.changed.emit();
      },
    });
  }

  private load(): void {
    this.resourceService.assignments(this.incident().id).subscribe({
      next: (assignments) => this.assignments.set(assignments),
      error: () => this.assignments.set([]),
    });
    this.loadNearby();
  }

  private loadNearby(): void {
    if (!this.showsNearby()) {
      this.nearby.set(null);
      return;
    }

    this.resourceService.nearby(this.incident().id, this.radiusKm()).subscribe({
      next: (found) => this.nearby.set(found),
      error: () => this.nearby.set([]),
    });
  }
}
