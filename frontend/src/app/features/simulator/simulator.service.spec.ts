import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SimulatorService } from './simulator.service';

describe('SimulatorService', () => {
  let service: SimulatorService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(SimulatorService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('reads the latest runs', () => {
    service.latest().subscribe();

    expect(http.expectOne('/api/simulations').request.method).toBe('GET');
  });

  it('starts a run with a POST carrying the settings', () => {
    const request = {
      scenario: 'FLOOD',
      latitude: 42.14,
      longitude: 24.75,
      radiusKm: 5,
      incidents: 10,
      intervalSeconds: 5,
      relatedIncidents: false,
    };

    service.start(request).subscribe();

    const sent = http.expectOne('/api/simulations').request;
    expect(sent.method).toBe('POST');
    expect(sent.body).toEqual(request);
  });

  it('stops and resumes a run with a POST', () => {
    service.stop('s1').subscribe();
    service.resume('s1').subscribe();

    expect(http.expectOne('/api/simulations/s1/stop').request.method).toBe('POST');
    expect(http.expectOne('/api/simulations/s1/resume').request.method).toBe('POST');
  });
});
