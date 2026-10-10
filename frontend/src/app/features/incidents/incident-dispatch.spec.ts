import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AuthService } from '../../core/auth/auth.service';
import { Role } from '../../core/auth/auth.models';
import { Assignment, NearbyResource } from '../resources/resource.models';
import { IncidentDispatch } from './incident-dispatch';
import { Incident, IncidentStatus } from './incident.models';

const ambulance: NearbyResource = {
  resource: {
    id: 'r1',
    callSign: 'AMBULANCE-17',
    kind: 'VEHICLE',
    type: 'AMBULANCE',
    status: 'AVAILABLE',
    latitude: 42.15,
    longitude: 24.75,
    teamId: null,
  },
  distanceMeters: 1840,
};

const assignment: Assignment = {
  id: 'as1',
  resourceId: 'r2',
  callSign: 'FIRETRUCK-04',
  incidentId: 'a1',
  assignedAt: '2026-10-10T10:06:00Z',
  releasedAt: null,
};

function incidentIn(status: IncidentStatus): Incident {
  return {
    id: 'a1',
    reference: 'INC-2026-0001',
    title: 'Flood in Plovdiv',
    description: 'The Maritsa has burst its banks near the centre.',
    category: 'FLOOD',
    severity: 'CRITICAL',
    status,
    latitude: 42.1354,
    longitude: 24.7453,
    affectedPeople: 25000,
    reportedBy: 'u1',
    createdAt: '2026-10-10T10:00:00Z',
    updatedAt: '2026-10-10T10:00:00Z',
  };
}

describe('IncidentDispatch', () => {
  let fixture: ComponentFixture<IncidentDispatch>;
  let http: HttpTestingController;
  let changes: number;

  async function setUp(role: Role, status: IncidentStatus): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [IncidentDispatch],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

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

    fixture = TestBed.createComponent(IncidentDispatch);
    fixture.componentRef.setInput('incident', incidentIn(status));
    changes = 0;
    fixture.componentInstance.changed.subscribe(() => changes++);
    fixture.detectChanges();
  }

  function nearbyRequest(): TestRequest {
    return http.expectOne((request) => request.url === '/api/incidents/a1/nearby-resources');
  }

  function element(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function button(label: string): HTMLButtonElement | undefined {
    return Array.from(element().querySelectorAll('button')).find((candidate) => candidate.textContent!.trim() === label);
  }

  afterEach(() => http.verify());

  it('lists what is assigned and what is available nearby for a dispatcher', async () => {
    await setUp('DISPATCHER', 'ACTIVE');
    http.expectOne('/api/incidents/a1/assignments').flush([assignment]);
    const nearby = nearbyRequest();
    expect(nearby.request.params.get('radiusKm')).toBe('15');
    nearby.flush([ambulance]);
    await fixture.whenStable();

    const text = element().textContent ?? '';
    expect(text).toContain('FIRETRUCK-04');
    expect(text).toContain('AMBULANCE-17');
    expect(text).toContain('1.8 km away');
    expect(button('Release')).toBeDefined();
    expect(button('Dispatch')).toBeDefined();
  });

  it('does not search or offer dispatch to a role that may not dispatch', async () => {
    await setUp('VIEWER', 'ACTIVE');
    http.expectOne('/api/incidents/a1/assignments').flush([assignment]);
    await fixture.whenStable();

    expect(element().textContent).toContain('FIRETRUCK-04');
    expect(element().textContent).not.toContain('Available nearby');
    expect(button('Release')).toBeUndefined();
  });

  it('does not offer dispatch for an incident that cannot receive resources', async () => {
    await setUp('DISPATCHER', 'REPORTED');
    http.expectOne('/api/incidents/a1/assignments').flush([]);
    await fixture.whenStable();

    expect(element().textContent).toContain('Nothing has been sent to this incident');
    expect(element().textContent).not.toContain('Available nearby');
  });

  it('searches again when another radius is chosen', async () => {
    await setUp('DISPATCHER', 'ACTIVE');
    http.expectOne('/api/incidents/a1/assignments').flush([]);
    nearbyRequest().flush([]);
    await fixture.whenStable();

    expect(element().textContent).toContain('No available resources within 15 km');

    const wider = Array.from(element().querySelectorAll<HTMLButtonElement>('mat-button-toggle button')).find(
      (candidate) => candidate.textContent!.includes('50 km'),
    )!;
    wider.click();

    const search = nearbyRequest();
    expect(search.request.params.get('radiusKm')).toBe('50');
    search.flush([ambulance]);
    await fixture.whenStable();

    expect(element().textContent).toContain('AMBULANCE-17');
    expect(element().querySelector('.count')?.textContent).toContain('1 within 50 km');
  });

  it('dispatches a resource and tells the page to reload', async () => {
    await setUp('DISPATCHER', 'VERIFIED');
    http.expectOne('/api/incidents/a1/assignments').flush([]);
    nearbyRequest().flush([ambulance]);
    await fixture.whenStable();

    button('Dispatch')!.click();

    const request = http.expectOne('/api/incidents/a1/dispatch');
    expect(request.request.body).toEqual({ resourceId: 'r1' });
    request.flush({ ...assignment, id: 'as2', resourceId: 'r1', callSign: 'AMBULANCE-17' });

    expect(changes).toBe(1);
  });

  it('releases a resource and tells the page to reload', async () => {
    await setUp('DISPATCHER', 'ACTIVE');
    http.expectOne('/api/incidents/a1/assignments').flush([assignment]);
    nearbyRequest().flush([]);
    await fixture.whenStable();

    button('Release')!.click();

    http.expectOne('/api/assignments/as1/release').flush({ ...assignment, releasedAt: '2026-10-10T11:00:00Z' });

    expect(changes).toBe(1);
  });

  it('shows the reason when the resource was taken by someone else', async () => {
    await setUp('DISPATCHER', 'ACTIVE');
    http.expectOne('/api/incidents/a1/assignments').flush([]);
    nearbyRequest().flush([ambulance]);
    await fixture.whenStable();

    button('Dispatch')!.click();
    http
      .expectOne('/api/incidents/a1/dispatch')
      .flush(
        { detail: 'AMBULANCE-17 cannot be assigned because it is EN_ROUTE' },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();

    expect(element().querySelector('[role=alert]')?.textContent).toContain('AMBULANCE-17 cannot be assigned');
    expect(changes).toBe(1);
  });

  it('reloads its lists when the incident changes', async () => {
    await setUp('DISPATCHER', 'VERIFIED');
    http.expectOne('/api/incidents/a1/assignments').flush([]);
    nearbyRequest().flush([ambulance]);
    await fixture.whenStable();

    fixture.componentRef.setInput('incident', incidentIn('RESOLVED'));
    fixture.detectChanges();

    http.expectOne('/api/incidents/a1/assignments').flush([{ ...assignment, releasedAt: '2026-10-10T11:00:00Z' }]);
    await fixture.whenStable();

    expect(element().textContent).toContain('released');
    expect(element().textContent).not.toContain('Available nearby');
  });
});
