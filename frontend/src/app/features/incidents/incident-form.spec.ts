import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Router, provideRouter } from '@angular/router';
import { MapView } from '../../shared/map/map-view';
import { MapViewStub } from '../../shared/map/map-view.testing';
import { IncidentForm } from './incident-form';

describe('IncidentForm', () => {
  let fixture: ComponentFixture<IncidentForm>;
  let http: HttpTestingController;
  let navigate: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IncidentForm],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    })
      .overrideComponent(IncidentForm, { remove: { imports: [MapView] }, add: { imports: [MapViewStub] } })
      .compileComponents();

    fixture = TestBed.createComponent(IncidentForm);
    http = TestBed.inject(HttpTestingController);
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  function element(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function set(control: string, value: string): void {
    const field = element().querySelector<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>(
      `[formControlName=${control}]`,
    )!;
    field.value = value;
    field.dispatchEvent(new Event(field.tagName === 'SELECT' ? 'change' : 'input'));
  }

  function fillIn(): void {
    set('title', '  Flood in Plovdiv ');
    set('description', 'The Maritsa has burst its banks.');
    set('category', 'FLOOD');
    set('severity', 'CRITICAL');
    set('latitude', '42.1354');
    set('longitude', '24.7453');
    set('affectedPeople', '25000');
  }

  function submit(): void {
    element().querySelector('form')!.dispatchEvent(new Event('submit'));
  }

  it('offers every category and severity', () => {
    expect(element().querySelectorAll('[formControlName=category] option').length).toBe(21);
    expect(element().querySelectorAll('[formControlName=severity] option').length).toBe(5);
    expect(element().textContent).toContain('Structure fire');
  });

  it('sends nothing while the form is incomplete', async () => {
    set('title', 'Flood in Plovdiv');
    submit();
    await fixture.whenStable();

    http.expectNone('/api/incidents');
  });

  it('refuses a latitude outside the valid range', async () => {
    fillIn();
    set('latitude', '120');
    submit();
    await fixture.whenStable();

    http.expectNone('/api/incidents');
  });

  it('fills in the coordinates when a point is clicked on the map', async () => {
    const map = fixture.debugElement.query(By.directive(MapViewStub)).componentInstance as MapViewStub;
    expect(map.pickable()).toBe(true);
    expect(map.picked()).toBeNull();

    map.pick.emit({ latitude: 42.15, longitude: 24.76 });
    await fixture.whenStable();

    expect(element().querySelector<HTMLInputElement>('[formControlName=latitude]')!.value).toBe('42.15');
    expect(element().querySelector<HTMLInputElement>('[formControlName=longitude]')!.value).toBe('24.76');
    expect(map.picked()).toEqual({ latitude: 42.15, longitude: 24.76 });
  });

  it('moves the map point when coordinates are typed', async () => {
    const map = fixture.debugElement.query(By.directive(MapViewStub)).componentInstance as MapViewStub;

    set('latitude', '42.2');
    set('longitude', '24.8');
    await fixture.whenStable();

    expect(map.picked()).toEqual({ latitude: 42.2, longitude: 24.8 });
  });

  it('reports the incident and opens its page', () => {
    fillIn();
    submit();

    const request = http.expectOne('/api/incidents');
    expect(request.request.body).toEqual({
      title: 'Flood in Plovdiv',
      description: 'The Maritsa has burst its banks.',
      category: 'FLOOD',
      severity: 'CRITICAL',
      latitude: 42.1354,
      longitude: 24.7453,
      affectedPeople: 25000,
    });
    request.flush({ id: 'new-id', reference: 'INC-2026-0009' });

    expect(navigate).toHaveBeenCalledWith(['/incidents', 'new-id']);
  });

  it('shows a message and keeps the form when the server refuses', async () => {
    fillIn();
    submit();
    http.expectOne('/api/incidents').flush(null, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    expect(element().querySelector('[role=alert]')?.textContent).toContain('could not be reported');
    expect(element().querySelector<HTMLInputElement>('[formControlName=title]')!.value).toContain('Flood in Plovdiv');
    expect(navigate).not.toHaveBeenCalled();
  });
});
