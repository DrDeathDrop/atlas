import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Router, provideRouter } from '@angular/router';
import { Role } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';
import { LiveUpdates } from '../../core/live/live-updates';
import { LiveUpdatesStub } from '../../core/live/live-updates.testing';
import { MapView } from '../../shared/map/map-view';
import { MapViewStub } from '../../shared/map/map-view.testing';
import { Incident } from '../incidents/incident.models';
import { Resource } from '../resources/resource.models';
import { Facility, RoadClosure, Zone } from './map-feature.models';
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

const hospital: Facility = {
  id: 'f1',
  name: 'City Hospital',
  type: 'HOSPITAL',
  address: 'Main street 1',
  latitude: 42.15,
  longitude: 24.75,
  capacity: 400,
  occupancy: 120,
};

const riverside: Zone = {
  id: 'z1',
  name: 'Riverside',
  type: 'EVACUATION',
  boundary: [
    { latitude: 42.1, longitude: 24.7 },
    { latitude: 42.1, longitude: 24.8 },
    { latitude: 42.2, longitude: 24.75 },
  ],
  createdAt: '2026-10-10T10:00:00Z',
  liftedAt: null,
};

const underpass: RoadClosure = {
  id: 'c1',
  roadName: 'bul. Maritsa',
  reason: 'Flooded underpass',
  path: [
    { latitude: 42.14, longitude: 24.74 },
    { latitude: 42.15, longitude: 24.75 },
  ],
  createdAt: '2026-10-10T10:00:00Z',
  reopenedAt: null,
};

describe('MapPage', () => {
  let fixture: ComponentFixture<MapPage>;
  let http: HttpTestingController;
  let live: LiveUpdatesStub;

  async function setUp(role: Role = 'DISPATCHER'): Promise<void> {
    live = new LiveUpdatesStub();
    await TestBed.configureTestingModule({
      imports: [MapPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: LiveUpdates, useValue: live },
      ],
    })
      .overrideComponent(MapPage, { remove: { imports: [MapView] }, add: { imports: [MapViewStub] } })
      .compileComponents();

    http = TestBed.inject(HttpTestingController);
    TestBed.inject(AuthService).login('user@atlas.local', 'secret123').subscribe();
    http.expectOne('/api/auth/login').flush({ accessToken: 't', tokenType: 'Bearer', expiresIn: 900 });
    http.expectOne('/api/auth/me').flush({
      id: 'u1',
      email: 'user@atlas.local',
      fullName: 'Test User',
      role,
      enabled: true,
    });

    fixture = TestBed.createComponent(MapPage);
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  function element(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function map(): MapViewStub {
    return fixture.debugElement.query(By.directive(MapViewStub)).componentInstance as MapViewStub;
  }

  function button(label: string): HTMLButtonElement | undefined {
    return Array.from(element().querySelectorAll('button')).find((candidate) =>
      candidate.textContent?.includes(label),
    );
  }

  async function loadData(): Promise<void> {
    http.expectOne('/api/incidents').flush([flood, closed]);
    http.expectOne('/api/resources').flush([ambulance]);
    http.expectOne('/api/facilities').flush([hospital]);
    http.expectOne('/api/zones').flush([riverside]);
    http.expectOne('/api/road-closures').flush([underpass]);
    await fixture.whenStable();
  }

  it('puts open incidents, resources and facilities on the map', async () => {
    await setUp();
    await loadData();

    const markers = map().markers();
    expect(markers.map((marker) => marker.id).sort()).toEqual(['f1', 'i1', 'r1']);

    const incident = markers.find((marker) => marker.id === 'i1')!;
    expect(incident.kind).toBe('incident');
    expect(incident.tone).toBe('critical');
    expect(incident.label).toContain('INC-2026-0001');

    const resource = markers.find((marker) => marker.id === 'r1')!;
    expect(resource.tone).toBe('en_route');
    expect(resource.label).toBe('AMBULANCE-17 · en route');

    const facility = markers.find((marker) => marker.id === 'f1')!;
    expect(facility.kind).toBe('facility');
    expect(facility.tone).toBe('hospital');
    expect(facility.label).toBe('City Hospital · 120 / 400');
  });

  it('draws zones and closed roads as shapes', async () => {
    await setUp();
    await loadData();

    const shapes = map().shapes();
    expect(shapes.map((shape) => [shape.id, shape.kind, shape.tone])).toEqual([
      ['z1', 'zone', 'evacuation'],
      ['c1', 'closure', 'closure'],
    ]);
    expect(shapes[0].points).toEqual(riverside.boundary);
    expect(shapes[1].label).toBe('bul. Maritsa · road closed');
  });

  it('hides a layer when its checkbox is cleared', async () => {
    await setUp();
    await loadData();
    const boxes = element().querySelectorAll<HTMLInputElement>('input[type=checkbox]');

    boxes[1].click();
    boxes[2].click();
    await fixture.whenStable();
    expect(map().markers().map((marker) => marker.id)).toEqual(['i1']);

    boxes[0].click();
    boxes[3].click();
    await fixture.whenStable();
    expect(map().markers()).toEqual([]);
    expect(map().shapes()).toEqual([]);
  });

  it('opens an incident when its marker is clicked, and ignores resources', async () => {
    await setUp();
    await loadData();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const markers = map().markers();

    map().markerClick.emit(markers.find((marker) => marker.id === 'r1')!);
    expect(navigate).not.toHaveBeenCalled();

    map().markerClick.emit(markers.find((marker) => marker.id === 'i1')!);
    expect(navigate).toHaveBeenCalledWith(['/incidents', 'i1']);
  });

  it('shows how full a facility is when its marker is clicked', async () => {
    await setUp();
    await loadData();

    map().markerClick.emit(map().markers().find((marker) => marker.id === 'f1')!);
    await fixture.whenStable();

    const selection = element().querySelector('.selection')!;
    expect(selection.textContent).toContain('City Hospital');
    expect(selection.textContent).toContain('120 of 400 places taken');
    expect(button('Lift zone')).toBeUndefined();
  });

  it('collects clicked points while a zone is being drawn', async () => {
    await setUp();
    await loadData();
    expect(map().pickable()).toBe(false);

    button('Draw zone')!.click();
    await fixture.whenStable();
    expect(map().pickable()).toBe(true);

    map().pick.emit({ latitude: 42.1, longitude: 24.7 });
    map().pick.emit({ latitude: 42.1, longitude: 24.8 });
    await fixture.whenStable();
    expect(map().draft()).toEqual({
      kind: 'area',
      points: [
        { latitude: 42.1, longitude: 24.7 },
        { latitude: 42.1, longitude: 24.8 },
      ],
    });

    button('Undo point')!.click();
    await fixture.whenStable();
    expect(map().draft()!.points.length).toBe(1);

    button('Cancel')!.click();
    await fixture.whenStable();
    expect(map().draft()).toBeNull();
    expect(map().pickable()).toBe(false);
  });

  it('keeps only the last clicked point for a facility', async () => {
    await setUp();
    await loadData();

    button('Add facility')!.click();
    await fixture.whenStable();
    map().pick.emit({ latitude: 42.1, longitude: 24.7 });
    map().pick.emit({ latitude: 42.2, longitude: 24.8 });
    await fixture.whenStable();

    expect(map().draft()).toEqual({ kind: 'point', points: [{ latitude: 42.2, longitude: 24.8 }] });
  });

  it('reloads the map after a zone is lifted', async () => {
    await setUp();
    await loadData();

    map().shapeClick.emit(map().shapes()[0]);
    await fixture.whenStable();
    expect(element().querySelector('.selection')!.textContent).toContain('Riverside');

    button('Lift zone')!.click();
    http.expectOne('/api/zones/z1/lift').flush({ ...riverside, liftedAt: '2026-10-10T12:00:00Z' });

    http.expectOne('/api/incidents').flush([flood]);
    http.expectOne('/api/resources').flush([ambulance]);
    http.expectOne('/api/facilities').flush([hospital]);
    http.expectOne('/api/zones').flush([]);
    http.expectOne('/api/road-closures').flush([underpass]);
    await fixture.whenStable();

    expect(map().shapes().map((shape) => shape.id)).toEqual(['c1']);
    expect(element().querySelector('.selection')).toBeNull();
  });

  it('reopens a road from its details', async () => {
    await setUp();
    await loadData();

    map().shapeClick.emit(map().shapes()[1]);
    await fixture.whenStable();
    expect(element().querySelector('.selection')!.textContent).toContain('Flooded underpass');

    button('Reopen road')!.click();
    http.expectOne('/api/road-closures/c1/reopen').flush({ ...underpass, reopenedAt: '2026-10-10T12:00:00Z' });
    await loadData();
  });

  it('lets a viewer look but not change anything', async () => {
    await setUp('VIEWER');
    await loadData();

    expect(button('Draw zone')).toBeUndefined();
    expect(button('Close road')).toBeUndefined();
    expect(button('Add facility')).toBeUndefined();

    map().shapeClick.emit(map().shapes()[0]);
    await fixture.whenStable();

    expect(element().querySelector('.selection')!.textContent).toContain('Riverside');
    expect(button('Lift zone')).toBeUndefined();
  });

  it('shows an error when the data cannot be loaded', async () => {
    await setUp();
    http.expectOne('/api/incidents').flush(null, { status: 500, statusText: 'Server Error' });
    expect(http.expectOne('/api/resources').cancelled).toBe(true);
    expect(http.expectOne('/api/facilities').cancelled).toBe(true);
    expect(http.expectOne('/api/zones').cancelled).toBe(true);
    expect(http.expectOne('/api/road-closures').cancelled).toBe(true);
    await fixture.whenStable();

    expect(element().textContent).toContain('could not be loaded');
  });

  it('reloads when anything on the map changes somewhere else', async () => {
    await setUp();
    await loadData();

    live.push({ kind: 'ZONE', id: 'z2' });
    await loadData();

    expect(map().shapes().length).toBe(2);
  });
});
