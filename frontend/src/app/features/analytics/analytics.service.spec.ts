import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AnalyticsService } from './analytics.service';

describe('AnalyticsService', () => {
  it('asks for the overview of a period', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);

    TestBed.inject(AnalyticsService).overview(30, true).subscribe();

    const request = http.expectOne((candidate) => candidate.url === '/api/analytics/overview').request;
    expect(request.method).toBe('GET');
    expect(request.params.get('days')).toBe('30');
    expect(request.params.get('includeSimulated')).toBe('true');
    http.verify();
  });
});
