import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { LiveUpdates } from '../../core/live/live-updates';
import { EnumLabelPipe } from '../../shared/enum-label.pipe';
import { MapView } from '../../shared/map/map-view';
import { MapPoint, PLOVDIV } from '../../shared/map/map.models';
import { SCENARIOS, Simulation } from './simulator.models';
import { SimulatorService } from './simulator.service';

@Component({
  selector: 'app-simulator-page',
  imports: [
    DatePipe,
    EnumLabelPipe,
    MapView,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
    ReactiveFormsModule,
  ],
  templateUrl: './simulator-page.html',
  styleUrl: './simulator-page.scss',
})
export class SimulatorPage implements OnInit {
  private readonly simulator = inject(SimulatorService);
  private readonly live = inject(LiveUpdates);
  private readonly formBuilder = inject(NonNullableFormBuilder);

  protected readonly scenarios = SCENARIOS;

  protected readonly form = this.formBuilder.group({
    scenario: ['FLOOD', Validators.required],
    latitude: [PLOVDIV.latitude as number | null, [Validators.required, Validators.min(-90), Validators.max(90)]],
    longitude: [PLOVDIV.longitude as number | null, [Validators.required, Validators.min(-180), Validators.max(180)]],
    radiusKm: [5 as number | null, [Validators.required, Validators.min(0.5), Validators.max(100)]],
    incidents: [10 as number | null, [Validators.required, Validators.min(1), Validators.max(50)]],
    intervalSeconds: [5 as number | null, [Validators.required, Validators.min(1), Validators.max(120)]],
    relatedIncidents: [false],
  });

  protected readonly runs = signal<Simulation[]>([]);
  protected readonly loading = signal(true);
  protected readonly failed = signal(false);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  private latestLoad = 0;

  private readonly values = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });

  protected readonly centre = computed<MapPoint | null>(() => {
    const { latitude, longitude } = this.values();
    const valid =
      typeof latitude === 'number' &&
      typeof longitude === 'number' &&
      Math.abs(latitude) <= 90 &&
      Math.abs(longitude) <= 180;
    return valid ? { latitude, longitude } : null;
  });

  protected readonly isRandom = computed(() => this.values().scenario === 'RANDOM');

  protected readonly running = computed(() => this.runs().find((run) => run.status === 'RUNNING') ?? null);

  constructor() {
    this.live
      .when((update) => update.kind === 'INCIDENT')
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.load(true));
  }

  ngOnInit(): void {
    this.load();
  }

  protected placeAt(point: MapPoint): void {
    this.form.patchValue({ latitude: point.latitude, longitude: point.longitude });
  }

  protected start(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.busy() || this.running() !== null) {
      return;
    }

    const value = this.form.getRawValue();
    this.begin();

    this.simulator
      .start({
        scenario: value.scenario,
        latitude: value.latitude as number,
        longitude: value.longitude as number,
        radiusKm: value.radiusKm as number,
        incidents: value.incidents as number,
        intervalSeconds: value.intervalSeconds as number,
        relatedIncidents: value.relatedIncidents,
      })
      .subscribe({
        next: () => this.finish(),
        error: (failure: unknown) => this.fail(failure, 'The simulation could not be started.'),
      });
  }

  protected stop(run: Simulation): void {
    if (this.busy()) {
      return;
    }

    this.begin();

    this.simulator.stop(run.id).subscribe({
      next: () => this.finish(),
      error: (failure: unknown) => this.fail(failure, 'The simulation could not be stopped.'),
    });
  }

  protected resume(run: Simulation): void {
    if (this.busy() || this.running() !== null) {
      return;
    }

    this.begin();

    this.simulator.resume(run.id).subscribe({
      next: () => this.finish(),
      error: (failure: unknown) => this.fail(failure, 'The simulation could not be resumed.'),
    });
  }

  protected mix(run: Simulation): string {
    if (run.scenario === 'RANDOM') {
      return 'any kind of incident';
    }
    return run.relatedIncidents ? 'with related incidents' : 'one kind only';
  }

  protected percent(run: Simulation): number {
    return Math.round((run.created / run.planned) * 100);
  }

  protected load(quietly = false): void {
    this.loading.set(!quietly);
    this.failed.set(false);

    const thisLoad = ++this.latestLoad;

    this.simulator.latest().subscribe({
      next: (runs) => {
        if (thisLoad === this.latestLoad) {
          this.runs.set(runs);
          this.loading.set(false);
        }
      },
      error: () => {
        if (thisLoad === this.latestLoad) {
          this.failed.set(true);
          this.loading.set(false);
        }
      },
    });
  }

  private begin(): void {
    this.busy.set(true);
    this.error.set(null);
  }

  private finish(): void {
    this.busy.set(false);
    this.load(true);
  }

  private fail(failure: unknown, fallback: string): void {
    this.busy.set(false);
    this.error.set(
      failure instanceof HttpErrorResponse && typeof failure.error?.detail === 'string'
        ? failure.error.detail
        : fallback,
    );
    this.load(true);
  }
}
