import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { MapView } from '../../shared/map/map-view';
import { MapViewStub } from '../../shared/map/map-view.testing';
import { ResourceEdit } from './resource-edit';
import { Resource } from './resource.models';

const ambulance: Resource = {
  id: 'r1',
  callSign: 'AMBULANCE-17',
  kind: 'VEHICLE',
  type: 'AMBULANCE',
  status: 'AVAILABLE',
  latitude: 42.14,
  longitude: 24.75,
  teamId: null,
};

describe('ResourceEdit', () => {
  let fixture: ComponentFixture<ResourceEdit>;
  let http: HttpTestingController;

  async function open(resource: Resource | null): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [ResourceEdit],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    })
      .overrideComponent(ResourceEdit, { remove: { imports: [MapView] }, add: { imports: [MapViewStub] } })
      .compileComponents();

    fixture = TestBed.createComponent(ResourceEdit);
    http = TestBed.inject(HttpTestingController);
    fixture.componentRef.setInput('id', 'r1');
    fixture.detectChanges();

    const request = http.expectOne('/api/resources/r1');
    if (resource === null) {
      request.flush(null, { status: 404, statusText: 'Not Found' });
    } else {
      request.flush(resource);
    }
    await fixture.whenStable();
  }

  afterEach(() => http.verify());

  function element(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function map(): MapViewStub {
    return fixture.debugElement.query(By.directive(MapViewStub)).componentInstance as MapViewStub;
  }

  function button(label: string): HTMLButtonElement {
    return Array.from(element().querySelectorAll('button')).find((candidate) =>
      candidate.textContent?.includes(label),
    )!;
  }

  it('shows the resource where it is now', async () => {
    await open(ambulance);

    expect(element().querySelector('h1')!.textContent).toContain('AMBULANCE-17');
    expect(element().querySelector<HTMLInputElement>('[formControlName=latitude]')!.value).toBe('42.14');
    expect(map().picked()).toEqual({ latitude: 42.14, longitude: 24.75 });
    expect(button('Save location').disabled).toBe(true);
  });

  it('moves the resource to the point clicked on the map', async () => {
    await open(ambulance);

    map().pick.emit({ latitude: 42.2, longitude: 24.8 });
    await fixture.whenStable();
    button('Save location').click();

    const request = http.expectOne('/api/resources/r1/location');
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ latitude: 42.2, longitude: 24.8 });
    request.flush({ ...ambulance, latitude: 42.2, longitude: 24.8 });
    await fixture.whenStable();

    expect(element().querySelector('[role=status]')!.textContent).toContain('Location saved');
    expect(button('Save location').disabled).toBe(true);
  });

  it('takes a resource out of service and offers to return it', async () => {
    await open(ambulance);

    button('Take out of service').click();

    const request = http.expectOne('/api/resources/r1/service');
    expect(request.request.body).toEqual({ inService: false });
    request.flush({ ...ambulance, status: 'OUT_OF_SERVICE' });
    await fixture.whenStable();

    expect(element().textContent).toContain('Out of service');
    expect(button('Return to service')).toBeDefined();
  });

  it('does not offer to take an assigned resource out of service', async () => {
    await open({ ...ambulance, status: 'EN_ROUTE' });

    expect(button('Take out of service').disabled).toBe(true);
    expect(element().textContent).toContain('Assigned to an incident');
  });

  it('shows the reason when the server refuses', async () => {
    await open(ambulance);

    button('Take out of service').click();
    http
      .expectOne('/api/resources/r1/service')
      .flush({ detail: 'AMBULANCE-17 is assigned to an incident.' }, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();

    expect(element().querySelector('[role=alert]')!.textContent).toContain('assigned to an incident');
  });

  it('says so when the resource does not exist', async () => {
    await open(null);

    expect(element().textContent).toContain('Resource not found');
  });
});
