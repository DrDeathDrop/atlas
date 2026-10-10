import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ResourceService } from './resource.service';

describe('ResourceService', () => {
  let service: ResourceService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ResourceService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists resources', () => {
    service.list().subscribe();

    expect(http.expectOne('/api/resources').request.method).toBe('GET');
  });

  it('creates a resource with a POST carrying the form', () => {
    const request = { callSign: 'AMBULANCE-40', type: 'AMBULANCE', latitude: 42.14, longitude: 24.75 };

    service.create(request).subscribe();

    const sent = http.expectOne('/api/resources').request;
    expect(sent.method).toBe('POST');
    expect(sent.body).toEqual(request);
  });

  it('searches near an incident with the chosen radius', () => {
    service.nearby('inc-1', 15).subscribe();

    const request = http.expectOne((candidate) => candidate.url === '/api/incidents/inc-1/nearby-resources').request;
    expect(request.method).toBe('GET');
    expect(request.params.get('radiusKm')).toBe('15');
  });

  it('reads the assignments of an incident', () => {
    service.assignments('inc-1').subscribe();

    expect(http.expectOne('/api/incidents/inc-1/assignments').request.method).toBe('GET');
  });

  it('dispatches a resource with a POST naming it', () => {
    service.dispatch('inc-1', 'res-1').subscribe();

    const request = http.expectOne('/api/incidents/inc-1/dispatch').request;
    expect(request.method).toBe('POST');
    expect(request.body).toEqual({ resourceId: 'res-1' });
  });

  it('releases an assignment', () => {
    service.release('asg-1').subscribe();

    expect(http.expectOne('/api/assignments/asg-1/release').request.method).toBe('POST');
  });

  it('reads one resource', () => {
    service.get('res-1').subscribe();

    expect(http.expectOne('/api/resources/res-1').request.method).toBe('GET');
  });

  it('moves a resource with a PATCH carrying the new coordinates', () => {
    service.updateLocation('res-1', 42.2, 24.8).subscribe();

    const request = http.expectOne('/api/resources/res-1/location').request;
    expect(request.method).toBe('PATCH');
    expect(request.body).toEqual({ latitude: 42.2, longitude: 24.8 });
  });

  it('takes a resource out of service with a PATCH', () => {
    service.setInService('res-1', false).subscribe();

    const request = http.expectOne('/api/resources/res-1/service').request;
    expect(request.method).toBe('PATCH');
    expect(request.body).toEqual({ inService: false });
  });
});
