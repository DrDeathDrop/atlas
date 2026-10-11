import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Simulation, StartSimulationRequest } from './simulator.models';

@Injectable({ providedIn: 'root' })
export class SimulatorService {
  private readonly http = inject(HttpClient);

  latest(): Observable<Simulation[]> {
    return this.http.get<Simulation[]>('/api/simulations');
  }

  start(request: StartSimulationRequest): Observable<Simulation> {
    return this.http.post<Simulation>('/api/simulations', request);
  }

  resume(simulationId: string): Observable<Simulation> {
    return this.http.post<Simulation>(`/api/simulations/${simulationId}/resume`, null);
  }

  stop(simulationId: string): Observable<Simulation> {
    return this.http.post<Simulation>(`/api/simulations/${simulationId}/stop`, null);
  }
}
