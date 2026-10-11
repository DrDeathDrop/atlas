import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AnalyticsOverview } from './analytics.models';

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly http = inject(HttpClient);

  overview(days: number, includeSimulated: boolean): Observable<AnalyticsOverview> {
    return this.http.get<AnalyticsOverview>('/api/analytics/overview', {
      params: { days, includeSimulated },
    });
  }
}
