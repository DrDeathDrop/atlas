import { Component, input, output } from '@angular/core';
import { MapDraft, MapMarker, MapPoint, MapShape, PLOVDIV } from './map.models';

@Component({ selector: 'app-map-view', template: '' })
export class MapViewStub {
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
}
