import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Router } from '@angular/router';
import { Observable, forkJoin } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { MapView } from '../../shared/map/map-view';
import { MapDraft, MapMarker, MapPoint, MapShape } from '../../shared/map/map.models';
import { Incident, IncidentStatus } from '../incidents/incident.models';
import { IncidentService } from '../incidents/incident.service';
import { Resource } from '../resources/resource.models';
import { ResourceService } from '../resources/resource.service';
import { EditorMode, MapEditor } from './map-editor';
import { Facility, RoadClosure, Zone } from './map-feature.models';
import { MapFeatureService } from './map-feature.service';

const OPEN_STATUSES: IncidentStatus[] = ['REPORTED', 'VERIFIED', 'ACTIVE', 'CONTAINED'];

const DRAFT_KINDS: Record<EditorMode, MapDraft['kind']> = { zone: 'area', closure: 'line', facility: 'point' };

interface Selection {
  kind: 'zone' | 'closure' | 'facility';
  id: string;
  title: string;
  detail: string;
}

@Component({
  selector: 'app-map-page',
  imports: [MapEditor, MapView, MatButtonModule, MatCheckboxModule, MatProgressBarModule],
  templateUrl: './map-page.html',
  styleUrl: './map-page.scss',
})
export class MapPage implements OnInit {
  private readonly incidentService = inject(IncidentService);
  private readonly resourceService = inject(ResourceService);
  private readonly features = inject(MapFeatureService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly incidents = signal<Incident[]>([]);
  protected readonly resources = signal<Resource[]>([]);
  protected readonly facilities = signal<Facility[]>([]);
  protected readonly zones = signal<Zone[]>([]);
  protected readonly closures = signal<RoadClosure[]>([]);

  protected readonly showIncidents = signal(true);
  protected readonly showResources = signal(true);
  protected readonly showFacilities = signal(true);
  protected readonly showZones = signal(true);

  protected readonly loading = signal(true);
  protected readonly failed = signal(false);

  protected readonly mode = signal<EditorMode | null>(null);
  protected readonly points = signal<MapPoint[]>([]);
  protected readonly selection = signal<Selection | null>(null);
  protected readonly ending = signal(false);
  protected readonly actionError = signal<string | null>(null);

  protected readonly mayEdit = computed(() => this.auth.hasRole('ADMIN', 'DISPATCHER'));

  protected readonly openIncidents = computed(() =>
    this.incidents().filter((incident) => OPEN_STATUSES.includes(incident.status)),
  );

  protected readonly draft = computed<MapDraft | null>(() => {
    const mode = this.mode();
    return mode === null ? null : { kind: DRAFT_KINDS[mode], points: this.points() };
  });

  protected readonly markers = computed<MapMarker[]>(() => {
    const facilities: MapMarker[] = this.showFacilities()
      ? this.facilities().map((facility) => ({
          id: facility.id,
          kind: 'facility',
          tone: facility.type.toLowerCase(),
          label: `${facility.name} · ${facility.occupancy} / ${facility.capacity}`,
          latitude: facility.latitude,
          longitude: facility.longitude,
        }))
      : [];

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

    return [...facilities, ...resources, ...incidents];
  });

  protected readonly shapes = computed<MapShape[]>(() => {
    if (!this.showZones()) {
      return [];
    }

    const zones: MapShape[] = this.zones().map((zone) => ({
      id: zone.id,
      kind: 'zone',
      tone: zone.type.toLowerCase(),
      label: `${zone.name} · ${readable(zone.type)} zone`,
      points: zone.boundary,
    }));

    const closures: MapShape[] = this.closures().map((closure) => ({
      id: closure.id,
      kind: 'closure',
      tone: 'closure',
      label: `${closure.roadName} · road closed`,
      points: closure.path,
    }));

    return [...zones, ...closures];
  });

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.failed.set(false);

    forkJoin({
      incidents: this.incidentService.list(),
      resources: this.resourceService.list(),
      facilities: this.features.facilities(),
      zones: this.features.zones(),
      closures: this.features.roadClosures(),
    }).subscribe({
      next: ({ incidents, resources, facilities, zones, closures }) => {
        this.incidents.set(incidents);
        this.resources.set(resources);
        this.facilities.set(facilities);
        this.zones.set(zones);
        this.closures.set(closures);
        this.loading.set(false);
      },
      error: () => {
        this.failed.set(true);
        this.loading.set(false);
      },
    });
  }

  protected start(mode: EditorMode): void {
    this.selection.set(null);
    this.actionError.set(null);
    this.points.set([]);
    this.mode.set(mode);
  }

  protected stop(): void {
    this.mode.set(null);
    this.points.set([]);
  }

  protected addPoint(point: MapPoint): void {
    if (this.mode() === 'facility') {
      this.points.set([point]);
    } else {
      this.points.update((points) => [...points, point]);
    }
  }

  protected undoPoint(): void {
    this.points.update((points) => points.slice(0, -1));
  }

  protected onSaved(): void {
    this.stop();
    this.load();
  }

  protected openMarker(marker: MapMarker): void {
    if (marker.kind === 'incident') {
      void this.router.navigate(['/incidents', marker.id]);
      return;
    }

    if (marker.kind === 'facility') {
      const facility = this.facilities().find((candidate) => candidate.id === marker.id);
      if (facility) {
        this.actionError.set(null);
        this.selection.set({
          kind: 'facility',
          id: facility.id,
          title: facility.name,
          detail: [
            readable(facility.type),
            `${facility.occupancy} of ${facility.capacity} places taken`,
            facility.address,
          ]
            .filter(Boolean)
            .join(' · '),
        });
      }
    }
  }

  protected openShape(shape: MapShape): void {
    this.actionError.set(null);

    if (shape.kind === 'zone') {
      const zone = this.zones().find((candidate) => candidate.id === shape.id);
      if (zone) {
        this.selection.set({
          kind: 'zone',
          id: zone.id,
          title: zone.name,
          detail: `${readable(zone.type)} zone · ${zone.boundary.length} corners`,
        });
      }
      return;
    }

    const closure = this.closures().find((candidate) => candidate.id === shape.id);
    if (closure) {
      this.selection.set({
        kind: 'closure',
        id: closure.id,
        title: closure.roadName,
        detail: ['road closed', closure.reason].filter(Boolean).join(' · '),
      });
    }
  }

  protected end(selection: Selection): void {
    if (selection.kind === 'facility' || this.ending()) {
      return;
    }

    this.ending.set(true);
    this.actionError.set(null);

    const request: Observable<unknown> =
      selection.kind === 'zone' ? this.features.liftZone(selection.id) : this.features.reopenRoad(selection.id);

    request.subscribe({
      next: () => {
        this.ending.set(false);
        this.selection.set(null);
        this.load();
      },
      error: () => {
        this.ending.set(false);
        this.actionError.set('That did not work. Refresh the map and try again.');
      },
    });
  }
}

function readable(value: string): string {
  return value.toLowerCase().split('_').join(' ');
}
