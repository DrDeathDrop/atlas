import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CreateFacilityRequest,
  CreateRoadClosureRequest,
  CreateZoneRequest,
  Facility,
  RoadClosure,
  Zone,
} from './map-feature.models';

@Injectable({ providedIn: 'root' })
export class MapFeatureService {
  private readonly http = inject(HttpClient);

  facilities(): Observable<Facility[]> {
    return this.http.get<Facility[]>('/api/facilities');
  }

  createFacility(request: CreateFacilityRequest): Observable<Facility> {
    return this.http.post<Facility>('/api/facilities', request);
  }

  zones(): Observable<Zone[]> {
    return this.http.get<Zone[]>('/api/zones');
  }

  createZone(request: CreateZoneRequest): Observable<Zone> {
    return this.http.post<Zone>('/api/zones', request);
  }

  liftZone(zoneId: string): Observable<Zone> {
    return this.http.post<Zone>(`/api/zones/${zoneId}/lift`, null);
  }

  roadClosures(): Observable<RoadClosure[]> {
    return this.http.get<RoadClosure[]>('/api/road-closures');
  }

  createRoadClosure(request: CreateRoadClosureRequest): Observable<RoadClosure> {
    return this.http.post<RoadClosure>('/api/road-closures', request);
  }

  reopenRoad(closureId: string): Observable<RoadClosure> {
    return this.http.post<RoadClosure>(`/api/road-closures/${closureId}/reopen`, null);
  }
}
