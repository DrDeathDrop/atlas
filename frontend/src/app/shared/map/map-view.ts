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
import { MapMarker, MapPoint, PLOVDIV } from './map.models';

@Component({
  selector: 'app-map-view',
  template: '<div #host class="atlas-map" [class.pickable]="pickable()"></div>',
  styleUrl: './map-view.scss',
  encapsulation: ViewEncapsulation.None,
})
export class MapView implements OnDestroy {
  readonly markers = input<MapMarker[]>([]);
  readonly center = input<MapPoint>(PLOVDIV);
  readonly zoom = input(12);
  readonly pickable = input(false);
  readonly picked = input<MapPoint | null>(null);
  readonly fitToMarkers = input(false);

  readonly pick = output<MapPoint>();
  readonly markerClick = output<MapMarker>();

  private readonly host = viewChild.required<ElementRef<HTMLDivElement>>('host');
  private readonly ready = signal(false);
  private readonly markerLayer = L.layerGroup();
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

    const map = L.map(this.host().nativeElement, { zoomControl: true, attributionControl: true }).setView(
      [start.latitude, start.longitude],
      this.zoom(),
    );

    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    }).addTo(map);

    this.markerLayer.addTo(map);

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
      const size = marker.kind === 'incident' ? 22 : 14;
      const icon = L.divIcon({
        className: `atlas-marker ${marker.kind} ${marker.tone}`,
        iconSize: [size, size],
      });

      const label = document.createElement('span');
      label.textContent = marker.label;

      L.marker([marker.latitude, marker.longitude], { icon, title: marker.label, keyboard: true })
        .bindTooltip(label, { direction: 'top', offset: [0, -size / 2] })
        .on('click', () => this.markerClick.emit(marker))
        .addTo(this.markerLayer);
    }

    if (fit && markers.length > 0 && this.map) {
      const bounds = L.latLngBounds(markers.map((marker) => [marker.latitude, marker.longitude] as [number, number]));
      this.map.fitBounds(bounds, { padding: [40, 40], maxZoom: 14 });
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

function round(value: number): number {
  return Math.round(value * 1_000_000) / 1_000_000;
}
