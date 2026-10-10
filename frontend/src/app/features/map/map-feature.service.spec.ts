import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MapFeatureService } from './map-feature.service';

const triangle = [
  { latitude: 42.1, longitude: 24.7 },
  { latitude: 42.1, longitude: 24.8 },
  { latitude: 42.2, longitude: 24.75 },
];

describe('MapFeatureService', () => {
  let service: MapFeatureService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(MapFeatureService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('reads facilities, zones and road closures', () => {
    service.facilities().subscribe();
    service.zones().subscribe();
    service.roadClosures().subscribe();

    expect(http.expectOne('/api/facilities').request.method).toBe('GET');
    expect(http.expectOne('/api/zones').request.method).toBe('GET');
    expect(http.expectOne('/api/road-closures').request.method).toBe('GET');
  });

  it('creates a facility with a POST carrying the form', () => {
    const request = {
      name: 'City Hospital',
      type: 'HOSPITAL',
      address: null,
      latitude: 42.14,
      longitude: 24.75,
      capacity: 400,
    };

    service.createFacility(request).subscribe();

    const sent = http.expectOne('/api/facilities').request;
    expect(sent.method).toBe('POST');
    expect(sent.body).toEqual(request);
  });

  it('creates a zone with its corners', () => {
    const request = { name: 'Riverside', type: 'EVACUATION', boundary: triangle };

    service.createZone(request).subscribe();

    const sent = http.expectOne('/api/zones').request;
    expect(sent.method).toBe('POST');
    expect(sent.body).toEqual(request);
  });

  it('creates a road closure with its path', () => {
    const request = { roadName: 'bul. Maritsa', reason: 'Flooded', path: triangle.slice(0, 2) };

    service.createRoadClosure(request).subscribe();

    const sent = http.expectOne('/api/road-closures').request;
    expect(sent.method).toBe('POST');
    expect(sent.body).toEqual(request);
  });

  it('lifts a zone and reopens a road with a POST', () => {
    service.liftZone('z1').subscribe();
    service.reopenRoad('c1').subscribe();

    expect(http.expectOne('/api/zones/z1/lift').request.method).toBe('POST');
    expect(http.expectOne('/api/road-closures/c1/reopen').request.method).toBe('POST');
  });
});
