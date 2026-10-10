import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { LiveUpdates } from '../core/live/live-updates';
import { LiveUpdatesStub } from '../core/live/live-updates.testing';
import { AppNotification } from '../core/notifications/notifications';
import { NotificationBell } from './notification-bell';

const reported: AppNotification = {
  id: 'n1',
  type: 'INCIDENT_REPORTED',
  title: 'INC-2026-0004 was reported',
  incidentId: 'i4',
  createdAt: '2026-10-10T10:00:00Z',
  read: false,
};

const verified: AppNotification = {
  id: 'n2',
  type: 'INCIDENT_STATUS_CHANGED',
  title: 'INC-2026-0003 is now verified',
  incidentId: 'i3',
  createdAt: '2026-10-10T09:00:00Z',
  read: true,
};

describe('NotificationBell', () => {
  let fixture: ComponentFixture<NotificationBell>;
  let http: HttpTestingController;
  let live: LiveUpdatesStub;
  let navigate: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    live = new LiveUpdatesStub();
    await TestBed.configureTestingModule({
      imports: [NotificationBell],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: LiveUpdates, useValue: live },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(NotificationBell);
    http = TestBed.inject(HttpTestingController);
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function element(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function bell(): HTMLButtonElement {
    return element().querySelector<HTMLButtonElement>('.bell')!;
  }

  async function count(unread: number): Promise<void> {
    http.expectOne('/api/notifications/unread-count').flush({ count: unread });
    await fixture.whenStable();
  }

  async function openPanel(items: AppNotification[]): Promise<void> {
    bell().click();
    http.expectOne('/api/notifications').flush(items);
    await fixture.whenStable();
  }

  it('shows how many notifications are unread', async () => {
    await count(3);

    expect(element().querySelector('.count')!.textContent).toBe('3');
    expect(bell().getAttribute('aria-label')).toBe('Notifications, 3 unread');
  });

  it('shows no number when everything has been read', async () => {
    await count(0);

    expect(element().querySelector('.count')).toBeNull();
    expect(bell().getAttribute('aria-label')).toBe('Notifications');
  });

  it('lists the latest notifications when opened', async () => {
    await count(1);
    await openPanel([reported, verified]);

    const items = element().querySelectorAll('.item');
    expect(items.length).toBe(2);
    expect(items[0].textContent).toContain('INC-2026-0004 was reported');
    expect(items[0].classList.contains('unread')).toBe(true);
    expect(items[1].classList.contains('unread')).toBe(false);
  });

  it('says so when there is nothing to show', async () => {
    await count(0);
    await openPanel([]);

    expect(element().textContent).toContain('Nothing yet');
  });

  it('marks a notification read and opens its incident when clicked', async () => {
    await count(1);
    await openPanel([reported, verified]);

    element().querySelector<HTMLButtonElement>('.item')!.click();

    http.expectOne('/api/notifications/n1/read').flush(null);
    await count(0);

    expect(navigate).toHaveBeenCalledWith(['/incidents', 'i4']);
    expect(element().querySelector('.panel')).toBeNull();
    expect(element().querySelector('.count')).toBeNull();
  });

  it('does not mark an already read notification again', async () => {
    await count(0);
    await openPanel([verified]);

    element().querySelector<HTMLButtonElement>('.item')!.click();

    http.expectNone('/api/notifications/n2/read');
    expect(navigate).toHaveBeenCalledWith(['/incidents', 'i3']);
  });

  it('marks everything read at once', async () => {
    await count(2);
    await openPanel([reported]);

    Array.from(element().querySelectorAll('button'))
      .find((button) => button.textContent?.includes('Mark all read'))!
      .click();

    http.expectOne('/api/notifications/read-all').flush(null);
    http.expectOne('/api/notifications/unread-count').flush({ count: 0 });
    http.expectOne('/api/notifications').flush([{ ...reported, read: true }]);
    await fixture.whenStable();

    expect(element().querySelector('.count')).toBeNull();
    expect(element().querySelector('.item')!.classList.contains('unread')).toBe(false);
  });

  it('updates the count when a new notification is announced', async () => {
    await count(0);

    live.push({ kind: 'INCIDENT', id: 'i1' });
    http.expectNone('/api/notifications/unread-count');

    live.push({ kind: 'NOTIFICATION', id: null });
    await count(1);

    expect(element().querySelector('.count')!.textContent).toBe('1');
  });

  it('closes when the user clicks somewhere else', async () => {
    await count(0);
    await openPanel([reported]);

    document.body.click();
    await fixture.whenStable();

    expect(element().querySelector('.panel')).toBeNull();
  });
});
