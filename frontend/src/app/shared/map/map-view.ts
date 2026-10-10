import {
  Component,
  ElementRef,
  OnDestroy,
  ViewEncapsulation,
  afterNextRender,
  effect,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import * as L from 'leaflet';
import { MapDraft, MapMarker, MapPoint, MapShape, PLOVDIV } from './map.models';

const SHAPE_COLORS: Record<string, string> = {
  evacuation: '#b3261e',
  affected: '#b45309',
  closure: '#111827',
};

const DRAFT_COLOR = '#005cbb';

const MARKER_SIZES: Record<MapMarker['kind'], number> = {
  incident: 22,
  resource: 14,
  facility: 18,
};

@Component({
  selector: 'app-map-view',
  template: '<div #host class="atlas-map" [class.pickable]="pickable()"></div>',
  styleUrl: './map-view.scss',
  encapsulation: ViewEncapsulation.None,
})
export class MapView implements OnDestroy {
  readonly markers = input<MapMarker[]>([]);
  readonly shapes = input<MapShape[]>([]);
  readonly draft = input<MapDraft | null>(null);
  readonly center = input<MapPoint>(PLOVDIV);
  readonly zoom = input(12);
  readonly pickable = input(false);
  readonly picked = input<MapPoint | null>(null);
  readonly fitToMarkers = input(false);

  readonly pick = output<MapPoint>();
  readonly markerClick = output<MapMarker>();
  readonly shapeClick = output<MapShape>();

  private readonly host = viewChild.required<ElementRef<HTMLDivElement>>('host');
  private readonly ready = signal(false);
  private readonly shapeLayer = L.layerGroup();
  private readonly markerLayer = L.layerGroup();
  private readonly draftLayer = L.layerGroup();
  private map: L.Map | null = null;
  private pickedMarker: L.Marker | null = null;

  constructor() {
    afterNextRender(() => this.create());

    effect(() => {
      const markers = this.markers();
      const fit = this.fitToMarkers();
      if (this.ready()) {
        this.drawMarkers(markers, fit);
      }
    });

    effect(() => {
      const shapes = this.shapes();
      const clickable = !this.pickable();
      if (this.ready()) {
        this.drawShapes(shapes, clickable);
      }
    });

    effect(() => {
      const draft = this.draft();
      if (this.ready()) {
        this.drawDraft(draft);
      }
    });

    effect(() => {
      const picked = this.picked();
      if (this.ready()) {
        this.drawPicked(picked);
      }
    });
  }

  ngOnDestroy(): void {
    this.map?.remove();
    this.map = null;
  }

  private create(): void {
    const start = this.picked() ?? this.center();

    const map = L.map(this.host().nativeElement, {
      zoomControl: true,
      attributionControl: true,
      renderer: L.svg(),
    }).setView([start.latitude, start.longitude], this.zoom());

    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    }).addTo(map);

    this.shapeLayer.addTo(map);
    this.markerLayer.addTo(map);
    this.draftLayer.addTo(map);

    map.on('click', (event: L.LeafletMouseEvent) => {
      if (this.pickable()) {
        this.pick.emit({ latitude: round(event.latlng.lat), longitude: round(event.latlng.lng) });
      }
    });

    this.map = map;
    this.ready.set(true);
    setTimeout(() => this.map?.invalidateSize(), 0);
  }

  private drawMarkers(markers: MapMarker[], fit: boolean): void {
    this.markerLayer.clearLayers();

    for (const marker of markers) {
      const size = MARKER_SIZES[marker.kind];
      const icon = L.divIcon({
        className: `atlas-marker ${marker.kind} ${marker.tone}`,
        iconSize: [size, size],
      });

      L.marker([marker.latitude, marker.longitude], { icon, title: marker.label, keyboard: true })
        .bindTooltip(text(marker.label), { direction: 'top', offset: [0, -size / 2] })
        .on('click', () => this.markerClick.emit(marker))
        .addTo(this.markerLayer);
    }

    if (fit && markers.length > 0 && this.map) {
      const bounds = L.latLngBounds(markers.map((marker) => [marker.latitude, marker.longitude] as [number, number]));
      this.map.fitBounds(bounds, { padding: [40, 40], maxZoom: 14 });
    }
  }

  private drawShapes(shapes: MapShape[], clickable: boolean): void {
    this.shapeLayer.clearLayers();

    for (const shape of shapes) {
      const color = SHAPE_COLORS[shape.tone] ?? SHAPE_COLORS['closure'];
      const className = `atlas-shape ${shape.kind} ${shape.tone}`;
      const path = shape.points.map(toLatLng);

      const layer =
        shape.kind === 'zone'
          ? L.polygon(path, { className, color, weight: 2, fillOpacity: 0.18, interactive: clickable })
          : L.polyline(path, { className, color, weight: 6, opacity: 0.9, interactive: clickable });

      layer
        .bindTooltip(text(shape.label), { sticky: true })
        .on('click', (event: L.LeafletMouseEvent) => {
          L.DomEvent.stopPropagation(event);
          this.shapeClick.emit(shape);
        })
        .addTo(this.shapeLayer);
    }
  }

  private drawDraft(draft: MapDraft | null): void {
    this.draftLayer.clearLayers();
    if (draft === null || draft.points.length === 0) {
      return;
    }

    const path = draft.points.map(toLatLng);
    const style = { className: 'atlas-draft', color: DRAFT_COLOR, weight: 3, dashArray: '6 6', interactive: false };

    if (draft.kind === 'area' && path.length >= 3) {
      L.polygon(path, { ...style, fillOpacity: 0.12 }).addTo(this.draftLayer);
    } else if (draft.kind !== 'point' && path.length >= 2) {
      L.polyline(path, style).addTo(this.draftLayer);
    }

    for (const point of path) {
      const icon = L.divIcon({ className: 'atlas-marker vertex', iconSize: [12, 12] });
      L.marker(point, { icon, interactive: false, keyboard: false }).addTo(this.draftLayer);
    }
  }

  private drawPicked(picked: MapPoint | null): void {
    if (!this.map) {
      return;
    }

    if (picked === null) {
      this.pickedMarker?.remove();
      this.pickedMarker = null;
      return;
    }

    if (this.pickedMarker === null) {
      const icon = L.divIcon({ className: 'atlas-marker picked', iconSize: [22, 22] });
      this.pickedMarker = L.marker([picked.latitude, picked.longitude], { icon, interactive: false }).addTo(this.map);
    } else {
      this.pickedMarker.setLatLng([picked.latitude, picked.longitude]);
    }

    if (!this.map.getBounds().contains([picked.latitude, picked.longitude])) {
      this.map.panTo([picked.latitude, picked.longitude]);
    }
  }
}

function toLatLng(point: MapPoint): [number, number] {
  return [point.latitude, point.longitude];
}

function text(value: string): HTMLElement {
  const label = document.createElement('span');
  label.textContent = value;
  return label;
}

function round(value: number): number {
  return Math.round(value * 1_000_000) / 1_000_000;
}
