import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AuditEntry, Incident, IncidentStatus, ReportIncidentRequest } from './incident.models';

@Injectable({ providedIn: 'root' })
export class IncidentService {
  private readonly http = inject(HttpClient);

  list(): Observable<Incident[]> {
    return this.http.get<Incident[]>('/api/incidents');
  }

  get(id: string): Observable<Incident> {
    return this.http.get<Incident>(`/api/incidents/${id}`);
  }

  report(request: ReportIncidentRequest): Observable<Incident> {
    return this.http.post<Incident>('/api/incidents', request);
  }

  allowedTransitions(id: string): Observable<IncidentStatus[]> {
    return this.http.get<IncidentStatus[]>(`/api/incidents/${id}/allowed-transitions`);
  }

  history(id: string): Observable<AuditEntry[]> {
    return this.http.get<AuditEntry[]>(`/api/audit/incidents/${id}`);
  }

  changeStatus(id: string, status: IncidentStatus): Observable<Incident> {
    return this.http.patch<Incident>(`/api/incidents/${id}/status`, { status });
  }
}
