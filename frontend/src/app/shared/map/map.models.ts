export interface MapPoint {
  latitude: number;
  longitude: number;
}

export interface MapMarker extends MapPoint {
  id: string;
  kind: 'incident' | 'resource';
  tone: string;
  label: string;
}

export const PLOVDIV: MapPoint = { latitude: 42.1354, longitude: 24.7453 };
