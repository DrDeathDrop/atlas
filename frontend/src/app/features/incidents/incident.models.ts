export type IncidentStatus =
  | 'REPORTED'
  | 'VERIFIED'
  | 'ACTIVE'
  | 'CONTAINED'
  | 'RESOLVED'
  | 'REJECTED'
  | 'ARCHIVED';

export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export const INCIDENT_CATEGORIES = [
  'FLOOD',
  'WILDFIRE',
  'EARTHQUAKE',
  'LANDSLIDE',
  'SINKHOLE',
  'AVALANCHE',
  'SEVERE_WEATHER',
  'STRUCTURE_FIRE',
  'EXPLOSION',
  'CHEMICAL_LEAK',
  'GAS_LEAK',
  'ROAD_ACCIDENT',
  'RAIL_ACCIDENT',
  'AVIATION_ACCIDENT',
  'BUILDING_COLLAPSE',
  'POWER_OUTAGE',
  'WATER_SUPPLY_FAILURE',
  'MEDICAL_EMERGENCY',
  'MISSING_PERSON',
  'OTHER',
] as const;

export const SEVERITIES: Severity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

export interface ReportIncidentRequest {
  title: string;
  description: string;
  category: string;
  severity: Severity;
  latitude: number;
  longitude: number;
  affectedPeople: number;
}

export interface AuditEntry {
  id: string;
  occurredAt: string;
  actorId: string;
  actorRole: string;
  action: 'INCIDENT_REPORTED' | 'INCIDENT_STATUS_CHANGED' | 'RESOURCE_ASSIGNED' | 'RESOURCE_RELEASED';
  entityType: string;
  entityId: string;
  entityReference: string | null;
  previousState: string | null;
  newState: string | null;
  relatedEntityId: string | null;
}

export interface Incident {
  id: string;
  reference: string;
  title: string;
  description: string;
  category: string;
  severity: Severity;
  status: IncidentStatus;
  latitude: number;
  longitude: number;
  affectedPeople: number;
  reportedBy: string;
  createdAt: string;
  updatedAt: string;
}
