import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, input, output } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { IncidentDetail } from './incident-detail';
import { IncidentDispatch } from './incident-dispatch';
import { AuditEntry, Incident, IncidentStatus } from './incident.models';

const incident: Incident = {
  id: 'a1',
  reference: 'INC-2026-0001',
  title: 'Flood in Plovdiv',
  description: 'The Maritsa has burst its banks near the centre.',
  category: 'FLOOD',
  severity: 'CRITICAL',
  status: 'REPORTED',
  latitude: 42.1354,
  longitude: 24.7453,
  affectedPeople: 25000,
  reportedBy: 'u1',
  createdAt: '2026-10-10T10:00:00Z',
  updatedAt: '2026-10-10T10:00:00Z',
};

const reported: AuditEntry = {
  id: 'h1',
  occurredAt: '2026-10-10T10:00:00Z',
  actorId: 'u1',
  actorRole: 'DISPATCHER',
  action: 'INCIDENT_REPORTED',
  entityType: 'INCIDENT',
  entityId: 'a1',
  entityReference: 'INC-2026-0001',
  previousState: null,
  newState: 'REPORTED',
  relatedEntityId: null,
};

@Component({ selector: 'app-incident-dispatch', template: '' })
class IncidentDispatchStub {
  readonly incident = input.required<Incident>();
  readonly changed = output<void>();
}

describe('IncidentDetail', () => {
  let fixture: ComponentFixture<IncidentDetail>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IncidentDetail],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    })
      .overrideComponent(IncidentDetail, {
        remove: { imports: [IncidentDispatch] },
        add: { imports: [IncidentDispatchStub] },
      })
      .compileComponents();

    fixture = TestBed.createComponent(IncidentDetail);
    fixture.componentRef.setInput('id', 'a1');
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function element(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function buttons(): string[] {
    return Array.from(element().querySelectorAll('.actions button')).map((button) => button.textContent!.trim());
  }

  async function answer(
    current: Incident,
    transitions: IncidentStatus[],
    history: AuditEntry[] | 'forbidden' = [reported],
  ): Promise<void> {
    http.expectOne('/api/incidents/a1').flush(current);
    http.expectOne('/api/incidents/a1/allowed-transitions').flush(transitions);
    const historyRequest = http.expectOne('/api/audit/incidents/a1');
    if (history === 'forbidden') {
      historyRequest.flush(null, { status: 403, statusText: 'Forbidden' });
    } else {
      historyRequest.flush(history);
    }
    await fixture.whenStable();
  }

  it('shows the incident and its history', async () => {
    await answer(incident, ['VERIFIED', 'REJECTED']);

    const text = element().textContent ?? '';
    expect(text).toContain('INC-2026-0001');
    expect(text).toContain('Flood in Plovdiv');
    expect(text).toContain('The Maritsa has burst its banks');
    expect(text).toContain('Incident reported');
    expect(text).toContain('Dispatcher');
  });

  it('offers exactly the status changes the server allows', async () => {
    await answer(incident, ['VERIFIED', 'REJECTED']);

    expect(buttons()).toEqual(['Verify', 'Reject as false report']);
  });

  it('calls a move back to active a reopening', async () => {
    await answer({ ...incident, status: 'CONTAINED' }, ['ACTIVE', 'RESOLVED']);

    expect(buttons()).toEqual(['Reopen', 'Resolve']);
  });

  it('offers no actions to a user who may not change the status', async () => {
    await answer(incident, []);

    expect(element().querySelector('.actions')).toBeNull();
  });

  it('hides the history from a role that may not read it', async () => {
    await answer(incident, [], 'forbidden');

    expect(element().textContent).toContain('Flood in Plovdiv');
    expect(element().textContent).not.toContain('History');
  });

  it('changes the status and reloads', async () => {
    await answer(incident, ['VERIFIED', 'REJECTED']);

    element().querySelector<HTMLButtonElement>('.actions button')!.click();

    const change = http.expectOne('/api/incidents/a1/status');
    expect(change.request.method).toBe('PATCH');
    expect(change.request.body).toEqual({ status: 'VERIFIED' });
    change.flush({ ...incident, status: 'VERIFIED' });

    await answer({ ...incident, status: 'VERIFIED' }, ['ACTIVE']);

    expect(buttons()).toEqual(['Activate']);
    expect(element().querySelector('app-status-badge')?.textContent).toContain('Verified');
  });

  it('shows the reason when the server refuses a change', async () => {
    await answer(incident, ['VERIFIED']);

    element().querySelector<HTMLButtonElement>('.actions button')!.click();
    http
      .expectOne('/api/incidents/a1/status')
      .flush(
        { detail: 'An incident cannot move from ARCHIVED to VERIFIED' },
        { status: 409, statusText: 'Conflict' },
      );
    await answer({ ...incident, status: 'ARCHIVED' }, []);

    expect(element().querySelector('[role=alert]')?.textContent).toContain('cannot move from ARCHIVED');
  });

  it('says so when the incident does not exist', async () => {
    http.expectOne('/api/incidents/a1').flush(null, { status: 404, statusText: 'Not Found' });
    expect(http.expectOne('/api/incidents/a1/allowed-transitions').cancelled).toBe(true);
    http.expectOne('/api/audit/incidents/a1').flush([]);
    await fixture.whenStable();

    expect(element().textContent).toContain('This incident does not exist');
  });
});
