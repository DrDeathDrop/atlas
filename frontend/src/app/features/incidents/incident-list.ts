import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { LiveUpdates } from '../../core/live/live-updates';
import { EnumLabelPipe } from '../../shared/enum-label.pipe';
import { SeverityBadge, StatusBadge } from './badges';
import { Incident } from './incident.models';
import { IncidentService } from './incident.service';

@Component({
  selector: 'app-incident-list',
  imports: [
    DatePipe,
    EnumLabelPipe,
    MatButtonModule,
    MatProgressBarModule,
    MatTableModule,
    RouterLink,
    SeverityBadge,
    StatusBadge,
  ],
  templateUrl: './incident-list.html',
  styleUrl: './incident-list.scss',
})
export class IncidentList implements OnInit {
  private readonly incidentService = inject(IncidentService);
  private readonly auth = inject(AuthService);
  private readonly live = inject(LiveUpdates);

  protected readonly mayReport = computed(() => this.auth.hasRole('ADMIN', 'DISPATCHER'));

  protected readonly columns = ['reference', 'title', 'category', 'severity', 'status', 'affectedPeople', 'createdAt'];
  protected readonly incidents = signal<Incident[]>([]);
  protected readonly loading = signal(true);
  protected readonly failed = signal(false);

  constructor() {
    this.live
      .when((update) => update.kind === 'INCIDENT')
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.load(true));
  }

  ngOnInit(): void {
    this.load();
  }

  protected load(quietly = false): void {
    this.loading.set(!quietly);
    this.failed.set(false);

    this.incidentService.list().subscribe({
      next: (incidents) => {
        this.incidents.set(incidents);
        this.loading.set(false);
      },
      error: () => {
        this.failed.set(true);
        this.loading.set(false);
      },
    });
  }
}
