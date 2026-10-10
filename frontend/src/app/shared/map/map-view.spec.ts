import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MapView } from './map-view';
import { MapMarker, MapPoint, MapShape } from './map.models';

const flood: MapMarker = {
  id: 'i1',
  kind: 'incident',
  tone: 'critical',
  label: 'INC-2026-0001 · Flood in Plovdiv',
  latitude: 42.1354,
  longitude: 24.7453,
};

const ambulance: MapMarker = {
  id: 'r1',
  kind: 'resource',
  tone: 'available',
  label: 'AMBULANCE-17',
  latitude: 42.1408,
  longitude: 24.7626,
};

const zone: MapShape = {
  id: 'z1',
  kind: 'zone',
  tone: 'evacuation',
  label: 'Riverside · evacuation zone',
  points: [
    { latitude: 42.1, longitude: 24.7 },
    { latitude: 42.1, longitude: 24.8 },
    { latitude: 42.2, longitude: 24.75 },
  ],
};

const closure: MapShape = {
  id: 'c1',
  kind: 'closure',
  tone: 'closure',
  label: 'bul. Maritsa · road closed',
  points: [
    { latitude: 42.14, longitude: 24.74 },
    { latitude: 42.15, longitude: 24.75 },
  ],
};

describe('MapView', () => {
  let fixture: ComponentFixture<MapView>;

  async function render(inputs: Record<string, unknown> = {}): Promise<void> {
    await TestBed.configureTestingModule({ imports: [MapView] }).compileComponents();
    fixture = TestBed.createComponent(MapView);
    for (const [name, value] of Object.entries(inputs)) {
      fixture.componentRef.setInput(name, value);
    }
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function element(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  afterEach(() => fixture.destroy());

  it('draws a map with one marker per item', async () => {
    await render({ markers: [flood, ambulance] });

    expect(element().querySelector('.leaflet-container')).not.toBeNull();
    expect(element().querySelectorAll('.atlas-marker.incident.critical').length).toBe(1);
    expect(element().querySelectorAll('.atlas-marker.resource.available').length).toBe(1);
  });

  it('redraws when the markers change', async () => {
    await render({ markers: [flood, ambulance] });

    fixture.componentRef.setInput('markers', [flood]);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(element().querySelectorAll('.atlas-marker.incident').length).toBe(1);
    expect(element().querySelectorAll('.atlas-marker.resource').length).toBe(0);
  });

  it('reports which marker was clicked', async () => {
    await render({ markers: [flood, ambulance] });
    const clicked: MapMarker[] = [];
    fixture.componentInstance.markerClick.subscribe((marker) => clicked.push(marker));

    element().querySelector<HTMLElement>('.atlas-marker.resource')!.click();

    expect(clicked).toEqual([ambulance]);
  });

  it('shows the chosen point and moves it when the point changes', async () => {
    await render({ pickable: true, picked: { latitude: 42.14, longitude: 24.75 } });

    expect(element().querySelectorAll('.atlas-marker.picked').length).toBe(1);

    fixture.componentRef.setInput('picked', null);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(element().querySelectorAll('.atlas-marker.picked').length).toBe(0);
  });

  it('reports a clicked location only when picking is switched on', async () => {
    await render({ pickable: false });
    const picks: MapPoint[] = [];
    fixture.componentInstance.pick.subscribe((point) => picks.push(point));
    const map = element().querySelector<HTMLElement>('.leaflet-container')!;

    map.dispatchEvent(new MouseEvent('click', { bubbles: true, clientX: 10, clientY: 10 }));
    expect(picks.length).toBe(0);

    fixture.componentRef.setInput('pickable', true);
    fixture.detectChanges();
    map.dispatchEvent(new MouseEvent('click', { bubbles: true, clientX: 10, clientY: 10 }));

    expect(picks.length).toBe(1);
    expect(Number.isFinite(picks[0].latitude)).toBe(true);
    expect(Number.isFinite(picks[0].longitude)).toBe(true);
  });

  it('draws zones and closed roads and reports which one was clicked', async () => {
    await render({ shapes: [zone, closure] });
    const clicked: MapShape[] = [];
    fixture.componentInstance.shapeClick.subscribe((shape) => clicked.push(shape));

    expect(element().querySelectorAll('path.atlas-shape.zone.evacuation').length).toBe(1);
    expect(element().querySelectorAll('path.atlas-shape.closure').length).toBe(1);

    element()
      .querySelector('path.atlas-shape.closure')!
      .dispatchEvent(new MouseEvent('click', { bubbles: true, clientX: 10, clientY: 10 }));

    expect(clicked).toEqual([closure]);
  });

  it('shows the shape being drawn with one handle per point', async () => {
    await render({ pickable: true, draft: { kind: 'area', points: zone.points } });

    expect(element().querySelectorAll('path.atlas-draft').length).toBe(1);
    expect(element().querySelectorAll('.atlas-marker.vertex').length).toBe(3);

    fixture.componentRef.setInput('draft', null);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(element().querySelectorAll('path.atlas-draft').length).toBe(0);
    expect(element().querySelectorAll('.atlas-marker.vertex').length).toBe(0);
  });
});
