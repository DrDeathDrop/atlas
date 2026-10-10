import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Assignment, CreateResourceRequest, NearbyResource, Resource } from './resource.models';

@Injectable({ providedIn: 'root' })
export class ResourceService {
  private readonly http = inject(HttpClient);

  list(): Observable<Resource[]> {
    return this.http.get<Resource[]>('/api/resources');
  }

  create(request: CreateResourceRequest): Observable<Resource> {
    return this.http.post<Resource>('/api/resources', request);
  }

  get(resourceId: string): Observable<Resource> {
    return this.http.get<Resource>(`/api/resources/${resourceId}`);
  }

  updateLocation(resourceId: string, latitude: number, longitude: number): Observable<Resource> {
    return this.http.patch<Resource>(`/api/resources/${resourceId}/location`, { latitude, longitude });
  }

  setInService(resourceId: string, inService: boolean): Observable<Resource> {
    return this.http.patch<Resource>(`/api/resources/${resourceId}/service`, { inService });
  }

  nearby(incidentId: string, radiusKm: number): Observable<NearbyResource[]> {
    return this.http.get<NearbyResource[]>(`/api/incidents/${incidentId}/nearby-resources`, {
      params: { radiusKm },
    });
  }

  assignments(incidentId: string): Observable<Assignment[]> {
    return this.http.get<Assignment[]>(`/api/incidents/${incidentId}/assignments`);
  }

  dispatch(incidentId: string, resourceId: string): Observable<Assignment> {
    return this.http.post<Assignment>(`/api/incidents/${incidentId}/dispatch`, { resourceId });
  }

  release(assignmentId: string): Observable<Assignment> {
    return this.http.post<Assignment>(`/api/assignments/${assignmentId}/release`, null);
  }
}
