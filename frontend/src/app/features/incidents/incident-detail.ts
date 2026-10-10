import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { LiveUpdates } from '../../core/live/live-updates';
import { EnumLabelPipe } from '../../shared/enum-label.pipe';
import { MapView } from '../../shared/map/map-view';
import { MapMarker } from '../../shared/map/map.models';
import { SeverityBadge, StatusBadge } from './badges';
import { IncidentDispatch } from './incident-dispatch';
import { AuditEntry, Incident, IncidentStatus } from './incident.models';
import { IncidentService } from './incident.service';

@Component({
  selector: 'app-incident-detail',
  imports: [
    DatePipe,
    EnumLabelPipe,
    IncidentDispatch,
    MapView,
    MatButtonModule,
    MatProgressBarModule,
    RouterLink,
    SeverityBadge,
    StatusBadge,
  ],
  templateUrl: './incident-detail.html',
  styleUrl: './incident-detail.scss',
})
export class IncidentDetail implements OnInit {
  private readonly incidentService = inject(IncidentService);
  private readonly live = inject(LiveUpdates);

  readonly id = input.required<string>();

  protected readonly incident = signal<Incident | null>(null);
  protected readonly transitions = signal<IncidentStatus[]>([]);
  protected readonly history = signal<AuditEntry[] | null>(null);
  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly actionError = signal<string | null>(null);
  protected readonly changing = signal(false);

  protected readonly locationMarker = computed<MapMarker[]>(() => {
    const incident = this.incident();
    return incident === null
      ? []
      : [
          {
            id: incident.id,
            kind: 'incident',
            tone: incident.severity.toLowerCase(),
            label: `${incident.reference} · ${incident.title}`,
            latitude: incident.latitude,
            longitude: incident.longitude,
          },
        ];
  });

  constructor() {
    this.live
      .when((update) => update.kind === 'RESOURCE' || (update.kind === 'INCIDENT' && update.id === this.id()))
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.load(true));
  }

  ngOnInit(): void {
    this.load();
  }

  protected changeStatus(status: IncidentStatus): void {
    if (this.changing()) {
      return;
    }

    this.changing.set(true);
    this.actionError.set(null);

    this.incidentService.changeStatus(this.id(), status).subscribe({
      next: () => {
        this.changing.set(false);
        this.load();
      },
      error: (failure: unknown) => {
        this.changing.set(false);
        this.actionError.set(messageOf(failure, 'The status could not be changed.'));
        this.load();
      },
    });
  }

  protected actionLabel(target: IncidentStatus): string {
    const current = this.incident()?.status;

    switch (target) {
      case 'VERIFIED':
        return 'Verify';
      case 'REJECTED':
        return 'Reject as false report';
      case 'ACTIVE':
        return current === 'VERIFIED' ? 'Activate' : 'Reopen';
      case 'CONTAINED':
        return 'Mark contained';
      case 'RESOLVED':
        return 'Resolve';
      case 'ARCHIVED':
        return 'Archive';
      default:
        return target;
    }
  }

  protected isForward(target: IncidentStatus): boolean {
    const current = this.incident()?.status;
    const reopening = target === 'ACTIVE' && current !== 'VERIFIED';
    return target !== 'REJECTED' && !reopening;
  }

  protected describe(entry: AuditEntry): string {
    switch (entry.action) {
      case 'INCIDENT_REPORTED':
        return 'Incident reported';
      case 'INCIDENT_STATUS_CHANGED':
        return `Status changed from ${label(entry.previousState)} to ${label(entry.newState)}`;
      case 'RESOURCE_ASSIGNED':
        return `${entry.entityReference ?? 'A resource'} assigned`;
      case 'RESOURCE_RELEASED':
        return `${entry.entityReference ?? 'A resource'} released`;
    }
  }

  protected load(quietly = false): void {
    this.loading.set(!quietly);
    this.loadError.set(null);

    forkJoin({
      incident: this.incidentService.get(this.id()),
      transitions: this.incidentService.allowedTransitions(this.id()),
    }).subscribe({
      next: ({ incident, transitions }) => {
        this.incident.set(incident);
        this.transitions.set(transitions);
        this.loading.set(false);
      },
      error: (failure: unknown) => {
        this.loading.set(false);
        this.loadError.set(
          failure instanceof HttpErrorResponse && failure.status === 404
            ? 'This incident does not exist.'
            : 'The incident could not be loaded. Check that the server is running and try again.',
        );
      },
    });

    this.incidentService.history(this.id()).subscribe({
      next: (entries) => this.history.set(entries),
      error: () => this.history.set(null),
    });
  }
}

function label(state: string | null): string {
  return new EnumLabelPipe().transform(state).toLowerCase();
}

function messageOf(failure: unknown, fallback: string): string {
  if (failure instanceof HttpErrorResponse && typeof failure.error?.detail === 'string') {
    return failure.error.detail;
  }
  return fallback;
}
