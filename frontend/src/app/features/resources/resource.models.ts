export type ResourceStatus = 'AVAILABLE' | 'EN_ROUTE' | 'ON_SCENE' | 'OUT_OF_SERVICE';

export const RESOURCE_TYPES = [
  'MEDICAL_TEAM',
  'FIRE_TEAM',
  'RESCUE_TEAM',
  'POLICE_UNIT',
  'TECHNICAL_TEAM',
  'AMBULANCE',
  'FIRE_TRUCK',
  'RESCUE_HELICOPTER',
  'RESCUE_BOAT',
  'POLICE_CAR',
  'UTILITY_VEHICLE',
] as const;

export interface CreateResourceRequest {
  callSign: string;
  type: string;
  latitude: number;
  longitude: number;
}

export interface Resource {
  id: string;
  callSign: string;
  kind: 'TEAM' | 'VEHICLE';
  type: string;
  status: ResourceStatus;
  latitude: number;
  longitude: number;
  teamId: string | null;
}

export interface NearbyResource {
  resource: Resource;
  distanceMeters: number;
}

export interface Assignment {
  id: string;
  resourceId: string;
  callSign: string;
  incidentId: string;
  assignedAt: string;
  releasedAt: string | null;
}
