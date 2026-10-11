import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { LiveUpdates } from '../../core/live/live-updates';
import { LiveUpdatesStub } from '../../core/live/live-updates.testing';
import { MapView } from '../../shared/map/map-view';
import { MapViewStub } from '../../shared/map/map-view.testing';
import { SimulatorPage } from './simulator-page';
import { Simulation } from './simulator.models';

const running: Simulation = {
  id: 's1',
  scenario: 'FLOOD',
  latitude: 42.1354,
  longitude: 24.7453,
  radiusKm: 5,
  planned: 10,
  created: 4,
  intervalSeconds: 5,
  relatedIncidents: false,
  status: 'RUNNING',
  startedAt: '2026-10-11T12:00:00Z',
  finishedAt: null,
};

const finished: Simulation = { ...running, id: 's0', scenario: 'WILDFIRE', created: 10, status: 'COMPLETED' };

describe('SimulatorPage', () => {
  let fixture: ComponentFixture<SimulatorPage>;
  let http: HttpTestingController;
  let live: LiveUpdatesStub;

  beforeEach(async () => {
    live = new LiveUpdatesStub();
    await TestBed.configureTestingModule({
      imports: [SimulatorPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: LiveUpdates, useValue: live }],
    })
      .overrideComponent(SimulatorPage, { remove: { imports: [MapView] }, add: { imports: [MapViewStub] } })
      .compileComponents();

    fixture = TestBed.createComponent(SimulatorPage);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function element(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function button(label: string): HTMLButtonElement | undefined {
    return Array.from(element().querySelectorAll('button')).find((candidate) =>
      candidate.textContent?.includes(label),
    );
  }

  function set(control: string, value: string): void {
    const field = element().querySelector<HTMLInputElement | HTMLSelectElement>(`[formControlName=${control}]`)!;
    field.value = value;
    field.dispatchEvent(new Event(field.tagName === 'SELECT' ? 'change' : 'input'));
  }

  async function runs(list: Simulation[]): Promise<void> {
    http.expectOne('/api/simulations').flush(list);
    await fixture.whenStable();
  }

  it('lists earlier runs with their progress', async () => {
    await runs([running, finished]);

    const items = element().querySelectorAll('.run');
    expect(items.length).toBe(2);
    expect(items[0].textContent).toContain('Flood');
    expect(items[0].textContent).toContain('4 of 10 incidents');
    expect(items[0].textContent).toContain('Running');
    expect(items[1].textContent).toContain('Completed');
  });

  it('starts a simulation with the chosen settings', async () => {
    await runs([finished]);

    set('scenario', 'EARTHQUAKE');
    set('incidents', '20');
    set('intervalSeconds', '2');
    set('radiusKm', '12');
    element().querySelector<HTMLInputElement>('input[type=checkbox]')!.click();
    const map = fixture.debugElement.query(By.directive(MapViewStub)).componentInstance as MapViewStub;
    map.pick.emit({ latitude: 42.2, longitude: 24.8 });
    await fixture.whenStable();

    element().querySelector('form')!.dispatchEvent(new Event('submit'));

    const request = http.expectOne((candidate) => candidate.method === 'POST' && candidate.url === '/api/simulations');
    expect(request.request.body).toEqual({
      scenario: 'EARTHQUAKE',
      latitude: 42.2,
      longitude: 24.8,
      radiusKm: 12,
      incidents: 20,
      intervalSeconds: 2,
      relatedIncidents: true,
    });
    request.flush({ ...running, id: 's2', created: 0 });
    await runs([{ ...running, id: 's2', created: 0 }, finished]);

    expect(element().querySelectorAll('.run').length).toBe(2);
  });

  it('does not start a second simulation while one is running', async () => {
    await runs([running]);

    expect(button('Start simulation')!.disabled).toBe(true);
    expect(element().textContent).toContain('A simulation is running');

    element().querySelector('form')!.dispatchEvent(new Event('submit'));
    http.expectNone((candidate) => candidate.method === 'POST');
  });

  it('sends nothing when a setting is out of range', async () => {
    await runs([]);

    set('incidents', '500');
    element().querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    http.expectNone((candidate) => candidate.method === 'POST');
    expect(element().textContent).toContain('Between 1 and 50');
  });

  it('stops a running simulation', async () => {
    await runs([running]);

    button('Stop')!.click();

    http.expectOne('/api/simulations/s1/stop').flush({ ...running, status: 'STOPPED' });
    await runs([{ ...running, status: 'STOPPED' }]);

    expect(button('Stop')).toBeUndefined();
    expect(button('Start simulation')!.disabled).toBe(false);
  });

  it('shows the reason when the server refuses to start', async () => {
    await runs([]);

    element().querySelector('form')!.dispatchEvent(new Event('submit'));
    http
      .expectOne((candidate) => candidate.method === 'POST')
      .flush({ detail: 'A simulation is already running.' }, { status: 409, statusText: 'Conflict' });
    await runs([running]);

    expect(element().querySelector('[role=alert]')!.textContent).toContain('already running');
  });

  it('follows the progress as incidents are created', async () => {
    await runs([running]);

    live.push({ kind: 'INCIDENT', id: 'i5' });
    await runs([{ ...running, created: 5 }]);

    expect(element().querySelector('.run')!.textContent).toContain('5 of 10 incidents');
  });

  it('resumes a stopped simulation that has incidents left', async () => {
    const stopped: Simulation = { ...running, status: 'STOPPED' };
    await runs([stopped, finished]);

    expect(element().querySelectorAll('.run')[1].textContent).not.toContain('Resume');
    button('Resume')!.click();

    http.expectOne('/api/simulations/s1/resume').flush(running);
    await runs([running, finished]);

    expect(button('Resume')).toBeUndefined();
    expect(button('Stop')).toBeDefined();
  });

  it('ignores an older answer that arrives after a newer one', async () => {
    live.push({ kind: 'INCIDENT', id: 'i5' });

    const [older, newer] = http.match('/api/simulations');
    newer.flush([{ ...running, status: 'STOPPED' }]);
    older.flush([running]);
    await fixture.whenStable();

    expect(element().querySelector('.run')!.textContent).toContain('Stopped');
  });

  it('offers a random scenario that needs no related-incidents choice', async () => {
    await runs([{ ...finished, scenario: 'RANDOM' }]);

    expect(element().querySelector('.run')!.textContent).toContain('any kind of incident');
    expect(element().querySelector('input[type=checkbox]')).not.toBeNull();

    set('scenario', 'RANDOM');
    await fixture.whenStable();

    expect(element().querySelector('input[type=checkbox]')).toBeNull();
    expect(element().textContent).toContain('Every incident is of a random kind');

    element().querySelector('form')!.dispatchEvent(new Event('submit'));
    const request = http.expectOne((candidate) => candidate.method === 'POST');
    expect(request.request.body.scenario).toBe('RANDOM');
    request.flush({ ...running, scenario: 'RANDOM' });
    await runs([{ ...running, scenario: 'RANDOM' }]);
  });
});
