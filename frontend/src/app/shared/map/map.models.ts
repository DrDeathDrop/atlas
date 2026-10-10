export interface MapPoint {
  latitude: number;
  longitude: number;
}

export interface MapMarker extends MapPoint {
  id: string;
  kind: 'incident' | 'resource' | 'facility';
  tone: string;
  label: string;
}

export interface MapShape {
  id: string;
  kind: 'zone' | 'closure';
  tone: string;
  label: string;
  points: MapPoint[];
}

export interface MapDraft {
  kind: 'area' | 'line' | 'point';
  points: MapPoint[];
}

export const PLOVDIV: MapPoint = { latitude: 42.1354, longitude: 24.7453 };
