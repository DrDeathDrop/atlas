export interface Count {
  name: string;
  count: number;
}

export interface DayCount {
  day: string;
  count: number;
}

export interface TimeStat {
  samples: number;
  averageMinutes: number | null;
  medianMinutes: number | null;
}

export interface Hotspot {
  latitude: number;
  longitude: number;
  incidents: number;
}

export interface AnalyticsOverview {
  days: number;
  includeSimulated: boolean;
  totals: { reported: number; open: number; closed: number; simulated: number };
  byCategory: Count[];
  bySeverity: Count[];
  byStatus: Count[];
  perDay: DayCount[];
  responseTimes: { toVerify: TimeStat; toFirstDispatch: TimeStat; toResolve: TimeStat };
  resourcesByStatus: Count[];
  busiestResources: Count[];
  hotspots: Hotspot[];
}
