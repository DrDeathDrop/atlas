import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Router, provideRouter } from '@angular/router';
import { MapView } from '../../shared/map/map-view';
import { MapViewStub } from '../../shared/map/map-view.testing';
import { Incident } from '../incidents/incident.models';
import { Resource } from '../resources/resource.models';
import { MapPage } from './map-page';

const flood: Incident = {
  id: 'i1',
  reference: 'INC-2026-0001',
  title: 'Flood in Plovdiv',
  description: 'x',
  category: 'FLOOD',
  severity: 'CRITICAL',
  status: 'ACTIVE',
  latitude: 42.1354,
  longitude: 24.7453,
  affectedPeople: 25000,
  reportedBy: 'u1',
  createdAt: '2026-10-10T10:00:00Z',
  updatedAt: '2026-10-10T10:00:00Z',
};

const closed: Incident = { ...flood, id: 'i2', reference: 'INC-2026-0002', status: 'ARCHIVED' };

const ambulance: Resource = {
  id: 'r1',
  callSign: 'AMBULANCE-17',
  kind: 'VEHICLE',
  type: 'AMBULANCE',
  status: 'EN_ROUTE',
  latitude: 42.1408,
  longitude: 24.7626,
  teamId: null,
};

describe('MapPage', () => {
  let fixture: ComponentFixture<MapPage>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MapPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    })
      .overrideComponent(MapPage, { remove: { imports: [MapView] }, add: { imports: [MapViewStub] } })
      .compileComponents();

    fixture = TestBed.createComponent(MapPage);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function map(): MapViewStub {
    return fixture.debugElement.query(By.directive(MapViewStub)).componentInstance as MapViewStub;
  }

  async function loadData(): Promise<void> {
    http.expectOne('/api/incidents').flush([flood, closed]);
    http.expectOne('/api/resources').flush([ambulance]);
    await fixture.whenStable();
  }

  it('puts open incidents and all resources on the map', async () => {
    await loadData();

    const markers = map().markers();
    expect(markers.map((marker) => marker.id).sort()).toEqual(['i1', 'r1']);

    const incident = markers.find((marker) => marker.id === 'i1')!;
    expect(incident.kind).toBe('incident');
    expect(incident.tone).toBe('critical');
    expect(incident.label).toContain('INC-2026-0001');

    const resource = markers.find((marker) => marker.id === 'r1')!;
    expect(resource.tone).toBe('en_route');
    expect(resource.label).toBe('AMBULANCE-17 · en route');
  });

  it('hides a layer when its checkbox is cleared', async () => {
    await loadData();
    const boxes = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLInputElement>('input[type=checkbox]');

    boxes[1].click();
    await fixture.whenStable();
    expect(map().markers().map((marker) => marker.id)).toEqual(['i1']);

    boxes[0].click();
    await fixture.whenStable();
    expect(map().markers()).toEqual([]);
  });

  it('opens an incident when its marker is clicked, and ignores resources', async () => {
    await loadData();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const markers = map().markers();

    map().markerClick.emit(markers.find((marker) => marker.id === 'r1')!);
    expect(navigate).not.toHaveBeenCalled();

    map().markerClick.emit(markers.find((marker) => marker.id === 'i1')!);
    expect(navigate).toHaveBeenCalledWith(['/incidents', 'i1']);
  });

  it('shows an error when the data cannot be loaded', async () => {
    http.expectOne('/api/incidents').flush(null, { status: 500, statusText: 'Server Error' });
    expect(http.expectOne('/api/resources').cancelled).toBe(true);
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('could not be loaded');
  });
});
