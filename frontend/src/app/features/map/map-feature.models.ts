import { MapPoint } from '../../shared/map/map.models';

export const FACILITY_TYPES = ['HOSPITAL', 'SHELTER'] as const;
export const ZONE_TYPES = ['EVACUATION', 'AFFECTED'] as const;

export type FacilityType = (typeof FACILITY_TYPES)[number];
export type ZoneType = (typeof ZONE_TYPES)[number];

export interface Facility {
  id: string;
  name: string;
  type: FacilityType;
  address: string | null;
  latitude: number;
  longitude: number;
  capacity: number;
  occupancy: number;
}

export interface CreateFacilityRequest {
  name: string;
  type: string;
  address: string | null;
  latitude: number;
  longitude: number;
  capacity: number;
}

export interface Zone {
  id: string;
  name: string;
  type: ZoneType;
  boundary: MapPoint[];
  createdAt: string;
  liftedAt: string | null;
}

export interface CreateZoneRequest {
  name: string;
  type: string;
  boundary: MapPoint[];
}

export interface RoadClosure {
  id: string;
  roadName: string;
  reason: string | null;
  path: MapPoint[];
  createdAt: string;
  reopenedAt: string | null;
}

export interface CreateRoadClosureRequest {
  roadName: string;
  reason: string | null;
  path: MapPoint[];
}
