export const SCENARIOS = ['FLOOD', 'EARTHQUAKE', 'WILDFIRE', 'INDUSTRIAL_ACCIDENT', 'SEVERE_STORM', 'RANDOM'] as const;

export type Scenario = (typeof SCENARIOS)[number];
export type SimulationStatus = 'RUNNING' | 'COMPLETED' | 'STOPPED';

export interface Simulation {
  id: string;
  scenario: Scenario;
  latitude: number;
  longitude: number;
  radiusKm: number;
  planned: number;
  created: number;
  intervalSeconds: number;
  relatedIncidents: boolean;
  status: SimulationStatus;
  startedAt: string;
  finishedAt: string | null;
}

export interface StartSimulationRequest {
  scenario: string;
  latitude: number;
  longitude: number;
  radiusKm: number;
  incidents: number;
  intervalSeconds: number;
  relatedIncidents: boolean;
}
