import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { IncidentList } from './incident-list';
import { Incident } from './incident.models';

const flood: Incident = {
  id: 'a1',
  reference: 'INC-2026-0001',
  title: 'Flood in Plovdiv',
  description: 'The Maritsa has burst its banks near the centre.',
  category: 'FLOOD',
  severity: 'CRITICAL',
  status: 'ACTIVE',
  latitude: 42.1354,
  longitude: 24.7453,
  affectedPeople: 25000,
  reportedBy: 'u1',
  createdAt: '2026-10-10T10:00:00Z',
  updatedAt: '2026-10-10T10:05:00Z',
};

const fire: Incident = {
  ...flood,
  id: 'a2',
  reference: 'INC-2026-0002',
  title: 'Warehouse fire',
  category: 'STRUCTURE_FIRE',
  severity: 'HIGH',
  status: 'REPORTED',
  affectedPeople: 40,
};

describe('IncidentList', () => {
  let fixture: ComponentFixture<IncidentList>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IncidentList],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(IncidentList);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function text(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  it('shows one row per incident with readable labels', async () => {
    http.expectOne('/api/incidents').flush([flood, fire]);
    await fixture.whenStable();

    const rows = (fixture.nativeElement as HTMLElement).querySelectorAll('tr[mat-row]');
    expect(rows.length).toBe(2);
    expect(text()).toContain('INC-2026-0001');
    expect(text()).toContain('Flood in Plovdiv');
    expect(text()).toContain('Structure fire');
    expect(text()).toContain('Critical');
    expect(text()).toContain('Active');
    expect(rows[0].querySelector('a')?.getAttribute('href')).toBe('/incidents/a1');
  });

  it('says so when there are no incidents', async () => {
    http.expectOne('/api/incidents').flush([]);
    await fixture.whenStable();

    expect(text()).toContain('No incidents have been reported yet');
    expect((fixture.nativeElement as HTMLElement).querySelector('table')).toBeNull();
  });

  it('shows an error and can retry when loading fails', async () => {
    http.expectOne('/api/incidents').flush(null, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    expect(text()).toContain('could not be loaded');

    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button')!.click();
    http.expectOne('/api/incidents').flush([flood]);
    await fixture.whenStable();

    expect(text()).not.toContain('could not be loaded');
    expect(text()).toContain('INC-2026-0001');
  });
});
