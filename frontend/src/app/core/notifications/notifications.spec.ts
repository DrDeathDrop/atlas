import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Notifications } from './notifications';

describe('Notifications', () => {
  let service: Notifications;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(Notifications);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('reads the latest notifications and the unread count', () => {
    service.latest().subscribe();
    service.unreadCount().subscribe();

    expect(http.expectOne('/api/notifications').request.method).toBe('GET');
    expect(http.expectOne('/api/notifications/unread-count').request.method).toBe('GET');
  });

  it('marks one notification or all of them as read with a POST', () => {
    service.markRead('n1').subscribe();
    service.markAllRead().subscribe();

    expect(http.expectOne('/api/notifications/n1/read').request.method).toBe('POST');
    expect(http.expectOne('/api/notifications/read-all').request.method).toBe('POST');
  });
});
