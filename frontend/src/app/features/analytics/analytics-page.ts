import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { BarChart, ChartItem } from '../../shared/chart/bar-chart';
import { MapView } from '../../shared/map/map-view';
import { MapMarker } from '../../shared/map/map.models';
import { AnalyticsOverview, Count, TimeStat } from './analytics.models';
import { AnalyticsService } from './analytics.service';

const SEVERITY_ORDER = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
const STATUS_ORDER = ['REPORTED', 'VERIFIED', 'ACTIVE', 'CONTAINED', 'RESOLVED', 'REJECTED', 'ARCHIVED'];

interface Tile {
  label: string;
  value: string;
  note: string;
}

@Component({
  selector: 'app-analytics-page',
  imports: [BarChart, MapView, MatButtonModule, MatButtonToggleModule, MatCheckboxModule, MatProgressBarModule],
  templateUrl: './analytics-page.html',
  styleUrl: './analytics-page.scss',
})
export class AnalyticsPage implements OnInit {
  private readonly analytics = inject(AnalyticsService);

  protected readonly periods = [7, 30, 90];
  protected readonly days = signal(30);
  protected readonly includeSimulated = signal(false);
  protected readonly overview = signal<AnalyticsOverview | null>(null);
  protected readonly loading = signal(true);
  protected readonly failed = signal(false);

  private latestLoad = 0;

  protected readonly tiles = computed<Tile[]>(() => {
    const overview = this.overview();
    if (overview === null) {
      return [];
    }

    const times = overview.responseTimes;
    return [
      { label: 'Incidents reported', value: overview.totals.reported.toLocaleString(), note: periodNote(overview) },
      { label: 'Still open', value: overview.totals.open.toLocaleString(), note: 'of those reported in the period' },
      { label: 'Closed', value: overview.totals.closed.toLocaleString(), note: 'resolved, rejected or archived' },
      { label: 'Median time to verify', value: duration(times.toVerify), note: sampleNote(times.toVerify) },
      {
        label: 'Median time to first dispatch',
        value: duration(times.toFirstDispatch),
        note: sampleNote(times.toFirstDispatch),
      },
      { label: 'Median time to resolve', value: duration(times.toResolve), note: sampleNote(times.toResolve) },
    ];
  });

  protected readonly perDay = computed<ChartItem[]>(() =>
    (this.overview()?.perDay ?? []).map((day) => ({ label: shortDate(day.day), value: day.count })),
  );

  protected readonly byCategory = computed(() => toItems(this.overview()?.byCategory ?? []));
  protected readonly bySeverity = computed(() => toItems(ordered(this.overview()?.bySeverity ?? [], SEVERITY_ORDER)));
  protected readonly byStatus = computed(() => toItems(ordered(this.overview()?.byStatus ?? [], STATUS_ORDER)));
  protected readonly resourcesByStatus = computed(() => toItems(this.overview()?.resourcesByStatus ?? []));

  protected readonly busiestResources = computed<ChartItem[]>(() =>
    (this.overview()?.busiestResources ?? []).map((resource) => ({ label: resource.name, value: resource.count })),
  );

  protected readonly hotspots = computed<MapMarker[]>(() =>
    (this.overview()?.hotspots ?? []).map((hotspot, index) => ({
      id: `hotspot-${index}`,
      kind: 'incident',
      tone: hotspot.incidents >= 10 ? 'critical' : hotspot.incidents >= 5 ? 'high' : 'medium',
      label: `${hotspot.incidents} incidents in this area`,
      latitude: hotspot.latitude,
      longitude: hotspot.longitude,
    })),
  );

  ngOnInit(): void {
    this.load();
  }

  protected choosePeriod(days: number): void {
    this.days.set(days);
    this.load();
  }

  protected toggleSimulated(include: boolean): void {
    this.includeSimulated.set(include);
    this.load();
  }

  protected load(): void {
    const thisLoad = ++this.latestLoad;
    this.loading.set(true);
    this.failed.set(false);

    this.analytics.overview(this.days(), this.includeSimulated()).subscribe({
      next: (overview) => {
        if (thisLoad === this.latestLoad) {
          this.overview.set(overview);
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
}

function toItems(counts: Count[]): ChartItem[] {
  return counts.map((count) => ({ label: readable(count.name), value: count.count }));
}

function ordered(counts: Count[], order: string[]): Count[] {
  return [...counts].sort((first, second) => order.indexOf(first.name) - order.indexOf(second.name));
}

function readable(name: string): string {
  const words = name.toLowerCase().split('_').join(' ');
  return words.charAt(0).toUpperCase() + words.slice(1);
}

function periodNote(overview: AnalyticsOverview): string {
  const period = overview.days === 1 ? 'today' : `in the last ${overview.days} days`;
  if (overview.includeSimulated || overview.totals.simulated === 0) {
    return period;
  }
  return `${period}, not counting ${overview.totals.simulated.toLocaleString()} simulated`;
}

function sampleNote(stat: TimeStat): string {
  if (stat.samples === 0) {
    return 'no incidents yet';
  }
  return stat.samples === 1 ? 'from 1 incident' : `from ${stat.samples.toLocaleString()} incidents`;
}

function duration(stat: TimeStat): string {
  const minutes = stat.medianMinutes;
  if (minutes === null || stat.samples === 0) {
    return '–';
  }
  if (minutes < 1) {
    return `${Math.max(1, Math.round(minutes * 60))} s`;
  }
  if (minutes < 60) {
    return `${Math.round(minutes)} min`;
  }

  const hours = Math.floor(minutes / 60);
  const rest = Math.round(minutes - hours * 60);
  if (hours < 24) {
    return rest === 0 ? `${hours} h` : `${hours} h ${rest} min`;
  }

  const wholeDays = Math.floor(hours / 24);
  const restHours = hours - wholeDays * 24;
  return restHours === 0 ? `${wholeDays} d` : `${wholeDays} d ${restHours} h`;
}

function shortDate(day: string): string {
  const [, month, date] = day.split('-');
  const names = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  return `${Number(date)} ${names[Number(month) - 1] ?? ''}`.trim();
}
