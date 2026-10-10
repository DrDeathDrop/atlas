import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { MapView } from '../../shared/map/map-view';
import { MapMarker } from '../../shared/map/map.models';
import { Incident, IncidentStatus } from '../incidents/incident.models';
import { IncidentService } from '../incidents/incident.service';
import { Resource } from '../resources/resource.models';
import { ResourceService } from '../resources/resource.service';

const OPEN_STATUSES: IncidentStatus[] = ['REPORTED', 'VERIFIED', 'ACTIVE', 'CONTAINED'];

@Component({
  selector: 'app-map-page',
  imports: [MapView, MatButtonModule, MatCheckboxModule, MatProgressBarModule],
  templateUrl: './map-page.html',
  styleUrl: './map-page.scss',
})
export class MapPage implements OnInit {
  private readonly incidentService = inject(IncidentService);
  private readonly resourceService = inject(ResourceService);
  private readonly router = inject(Router);

  protected readonly incidents = signal<Incident[]>([]);
  protected readonly resources = signal<Resource[]>([]);
  protected readonly showIncidents = signal(true);
  protected readonly showResources = signal(true);
  protected readonly loading = signal(true);
  protected readonly failed = signal(false);

  protected readonly openIncidents = computed(() =>
    this.incidents().filter((incident) => OPEN_STATUSES.includes(incident.status)),
  );

  protected readonly markers = computed<MapMarker[]>(() => {
    const resources: MapMarker[] = this.showResources()
      ? this.resources().map((resource) => ({
          id: resource.id,
          kind: 'resource',
          tone: resource.status.toLowerCase(),
          label: `${resource.callSign} · ${readable(resource.status)}`,
          latitude: resource.latitude,
          longitude: resource.longitude,
        }))
      : [];

    const incidents: MapMarker[] = this.showIncidents()
      ? this.openIncidents().map((incident) => ({
          id: incident.id,
          kind: 'incident',
          tone: incident.severity.toLowerCase(),
          label: `${incident.reference} · ${incident.title}`,
          latitude: incident.latitude,
          longitude: incident.longitude,
        }))
      : [];

    return [...resources, ...incidents];
  });

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.failed.set(false);

    forkJoin({ incidents: this.incidentService.list(), resources: this.resourceService.list() }).subscribe({
      next: ({ incidents, resources }) => {
        this.incidents.set(incidents);
        this.resources.set(resources);
        this.loading.set(false);
      },
      error: () => {
        this.failed.set(true);
        this.loading.set(false);
      },
    });
  }

  protected open(marker: MapMarker): void {
    if (marker.kind === 'incident') {
      void this.router.navigate(['/incidents', marker.id]);
    }
  }
}

function readable(value: string): string {
  return value.toLowerCase().split('_').join(' ');
}
