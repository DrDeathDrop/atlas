import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { IncidentService } from './incident.service';

describe('IncidentService', () => {
  let service: IncidentService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(IncidentService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists incidents', () => {
    service.list().subscribe();

    expect(http.expectOne('/api/incidents').request.method).toBe('GET');
  });

  it('reads one incident', () => {
    service.get('abc').subscribe();

    expect(http.expectOne('/api/incidents/abc').request.method).toBe('GET');
  });

  it('reports an incident with a POST carrying the form', () => {
    const request = {
      title: 'Flood in Plovdiv',
      description: 'The Maritsa has burst its banks.',
      category: 'FLOOD',
      severity: 'CRITICAL' as const,
      latitude: 42.1354,
      longitude: 24.7453,
      affectedPeople: 25000,
    };

    service.report(request).subscribe();

    const sent = http.expectOne('/api/incidents').request;
    expect(sent.method).toBe('POST');
    expect(sent.body).toEqual(request);
  });

  it('asks which status changes the current user may make', () => {
    service.allowedTransitions('abc').subscribe();

    expect(http.expectOne('/api/incidents/abc/allowed-transitions').request.method).toBe('GET');
  });

  it('reads the history of an incident', () => {
    service.history('abc').subscribe();

    expect(http.expectOne('/api/audit/incidents/abc').request.method).toBe('GET');
  });

  it('changes a status with a PATCH carrying the new status', () => {
    service.changeStatus('abc', 'VERIFIED').subscribe();

    const request = http.expectOne('/api/incidents/abc/status').request;
    expect(request.method).toBe('PATCH');
    expect(request.body).toEqual({ status: 'VERIFIED' });
  });
});
