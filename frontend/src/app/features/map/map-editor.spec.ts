import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MapPoint } from '../../shared/map/map.models';
import { EditorMode, MapEditor } from './map-editor';

const triangle: MapPoint[] = [
  { latitude: 42.1, longitude: 24.7 },
  { latitude: 42.1, longitude: 24.8 },
  { latitude: 42.2, longitude: 24.75 },
];

describe('MapEditor', () => {
  let fixture: ComponentFixture<MapEditor>;
  let http: HttpTestingController;
  let saved: number;

  async function render(mode: EditorMode, points: MapPoint[]): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [MapEditor],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(MapEditor);
    http = TestBed.inject(HttpTestingController);
    fixture.componentRef.setInput('mode', mode);
    fixture.componentRef.setInput('points', points);
    saved = 0;
    fixture.componentInstance.saved.subscribe(() => saved++);
    await fixture.whenStable();
  }

  afterEach(() => http.verify());

  function element(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function set(control: string, value: string): void {
    const field = element().querySelector<HTMLInputElement | HTMLSelectElement>(`[formControlName=${control}]`)!;
    field.value = value;
    field.dispatchEvent(new Event(field.tagName === 'SELECT' ? 'change' : 'input'));
  }

  function submit(): void {
    element().querySelector('form')!.dispatchEvent(new Event('submit'));
  }

  function button(label: string): HTMLButtonElement {
    return Array.from(element().querySelectorAll('button')).find((candidate) =>
      candidate.textContent?.includes(label),
    )!;
  }

  it('saves a zone with its name, type and corners', async () => {
    await render('zone', triangle);
    set('name', ' Riverside ');
    set('type', 'AFFECTED');
    submit();

    const request = http.expectOne('/api/zones');
    expect(request.request.body).toEqual({ name: 'Riverside', type: 'AFFECTED', boundary: triangle });
    request.flush({ id: 'z1' });

    expect(saved).toBe(1);
  });

  it('sends nothing while the zone has no name', async () => {
    await render('zone', triangle);
    submit();
    await fixture.whenStable();

    http.expectNone('/api/zones');
    expect(saved).toBe(0);
  });

  it('asks for more corners before saving a zone', async () => {
    await render('zone', triangle.slice(0, 2));
    set('name', 'Riverside');
    submit();
    await fixture.whenStable();

    http.expectNone('/api/zones');
    expect(element().querySelector('[role=alert]')?.textContent).toContain('At least three corners');
  });

  it('saves a road closure and leaves out an empty reason', async () => {
    await render('closure', triangle.slice(0, 2));
    set('roadName', 'bul. Maritsa');
    submit();

    const request = http.expectOne('/api/road-closures');
    expect(request.request.body).toEqual({ roadName: 'bul. Maritsa', reason: null, path: triangle.slice(0, 2) });
    request.flush({ id: 'c1' });

    expect(saved).toBe(1);
  });

  it('saves a facility at the clicked point', async () => {
    await render('facility', [triangle[0]]);
    set('name', 'City Hospital');
    set('type', 'SHELTER');
    set('capacity', '400');
    set('address', ' Main street 1 ');
    submit();

    const request = http.expectOne('/api/facilities');
    expect(request.request.body).toEqual({
      name: 'City Hospital',
      type: 'SHELTER',
      address: 'Main street 1',
      latitude: 42.1,
      longitude: 24.7,
      capacity: 400,
    });
    request.flush({ id: 'f1' });

    expect(saved).toBe(1);
  });

  it('shows the reason when the server refuses', async () => {
    await render('zone', triangle);
    set('name', 'Riverside');
    submit();
    http
      .expectOne('/api/zones')
      .flush(
        { detail: 'The edges of an area must not cross each other' },
        { status: 400, statusText: 'Bad Request' },
      );
    await fixture.whenStable();

    expect(element().querySelector('[role=alert]')?.textContent).toContain('must not cross');
    expect(saved).toBe(0);
  });

  it('reports undo and cancel to the page', async () => {
    await render('closure', triangle.slice(0, 1));
    let undone = 0;
    let dismissed = 0;
    fixture.componentInstance.undo.subscribe(() => undone++);
    fixture.componentInstance.dismiss.subscribe(() => dismissed++);

    button('Undo point').click();
    button('Cancel').click();

    expect(undone).toBe(1);
    expect(dismissed).toBe(1);
  });
});
