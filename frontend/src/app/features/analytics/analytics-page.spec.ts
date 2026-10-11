import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { MapView } from '../../shared/map/map-view';
import { MapViewStub } from '../../shared/map/map-view.testing';
import { AnalyticsPage } from './analytics-page';
import { AnalyticsOverview } from './analytics.models';

const overview: AnalyticsOverview = {
  days: 30,
  includeSimulated: false,
  totals: { reported: 1240, open: 12, closed: 1228, simulated: 40 },
  byCategory: [
    { name: 'FLOOD', count: 700 },
    { name: 'GAS_LEAK', count: 540 },
  ],
  bySeverity: [
    { name: 'LOW', count: 600 },
    { name: 'CRITICAL', count: 140 },
    { name: 'HIGH', count: 500 },
  ],
  byStatus: [
    { name: 'RESOLVED', count: 1228 },
    { name: 'ACTIVE', count: 12 },
  ],
  perDay: [
    { day: '2026-10-10', count: 3 },
    { day: '2026-10-11', count: 5 },
  ],
  responseTimes: {
    toVerify: { samples: 900, averageMinutes: 9.2, medianMinutes: 4.4 },
    toFirstDispatch: { samples: 1, averageMinutes: 130, medianMinutes: 130 },
    toResolve: { samples: 0, averageMinutes: null, medianMinutes: null },
  },
  resourcesByStatus: [{ name: 'AVAILABLE', count: 20 }],
  busiestResources: [{ name: 'AMBULANCE-17', count: 9 }],
  hotspots: [
    { latitude: 42.14, longitude: 24.75, incidents: 12 },
    { latitude: 42.2, longitude: 24.8, incidents: 2 },
  ],
};

describe('AnalyticsPage', () => {
  let fixture: ComponentFixture<AnalyticsPage>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnalyticsPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    })
      .overrideComponent(AnalyticsPage, { remove: { imports: [MapView] }, add: { imports: [MapViewStub] } })
      .compileComponents();

    fixture = TestBed.createComponent(AnalyticsPage);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function element(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function request(): TestRequest {
    return http.expectOne((candidate) => candidate.url === '/api/analytics/overview');
  }

  async function answer(data: AnalyticsOverview = overview): Promise<void> {
    request().flush(data);
    await fixture.whenStable();
  }

  function tile(label: string): string {
    const found = Array.from(element().querySelectorAll('.tile')).find((candidate) =>
      candidate.textContent?.includes(label),
    )!;
    return found.textContent!.replace(/\s+/g, ' ').trim();
  }

  it('asks for the last 30 days without simulated incidents to begin with', () => {
    const sent = request().request;

    expect(sent.params.get('days')).toBe('30');
    expect(sent.params.get('includeSimulated')).toBe('false');
  });

  it('leads with the headline figures', async () => {
    await answer();

    expect(tile('Incidents reported')).toContain('1,240');
    expect(tile('Incidents reported')).toContain('in the last 30 days, not counting 40 simulated');
    expect(tile('Still open')).toContain('12');
    expect(tile('Median time to verify')).toContain('4 min');
    expect(tile('Median time to verify')).toContain('from 900 incidents');
    expect(tile('Median time to first dispatch')).toContain('2 h 10 min');
    expect(tile('Median time to first dispatch')).toContain('from 1 incident');
    expect(tile('Median time to resolve')).toContain('–');
    expect(tile('Median time to resolve')).toContain('no incidents yet');
  });

  it('charts the breakdowns with readable labels in a sensible order', async () => {
    await answer();

    const labels = (heading: string) =>
      Array.from(
        Array.from(element().querySelectorAll('.panel'))
          .find((panel) => panel.querySelector('h2')?.textContent?.includes(heading))!
          .querySelectorAll('.bar-label'),
      ).map((label) => label.textContent);

    expect(labels('By category')).toEqual(['Flood', 'Gas leak']);
    expect(labels('By severity')).toEqual(['Critical', 'High', 'Low']);
    expect(labels('By current status')).toEqual(['Active', 'Resolved']);
    expect(labels('Most dispatched resources')).toEqual(['AMBULANCE-17']);
    expect(element().querySelector('.range')!.textContent).toContain('10 Oct');
    expect(element().querySelector('.range')!.textContent).toContain('11 Oct');
  });

  it('marks the areas where incidents cluster', async () => {
    await answer();

    const map = fixture.debugElement.query(By.directive(MapViewStub)).componentInstance as MapViewStub;
    expect(map.markers().map((marker) => [marker.tone, marker.label])).toEqual([
      ['critical', '12 incidents in this area'],
      ['medium', '2 incidents in this area'],
    ]);
    expect(map.fitToMarkers()).toBe(true);
  });

  it('says so when no area has a cluster', async () => {
    await answer({ ...overview, hotspots: [] });

    expect(fixture.debugElement.query(By.directive(MapViewStub))).toBeNull();
    expect(element().textContent).toContain('No area had two or more incidents');
  });

  it('reloads for another period or with simulated incidents', async () => {
    await answer();

    Array.from(element().querySelectorAll<HTMLButtonElement>('mat-button-toggle button'))
      .find((button) => button.textContent?.includes('7 days'))!
      .click();
    expect(request().request.params.get('days')).toBe('7');

    element().querySelector<HTMLInputElement>('input[type=checkbox]')!.click();
    const withSimulated = request();
    expect(withSimulated.request.params.get('days')).toBe('7');
    expect(withSimulated.request.params.get('includeSimulated')).toBe('true');
    withSimulated.flush({ ...overview, days: 7, includeSimulated: true });
    await fixture.whenStable();

    expect(tile('Incidents reported')).toContain('in the last 7 days');
    expect(tile('Incidents reported')).not.toContain('simulated');
  });

  it('shows an error when the figures cannot be loaded', async () => {
    request().flush(null, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    expect(element().textContent).toContain('could not be loaded');
  });
});
