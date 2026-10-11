import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CurrentUser } from './auth.models';
import { AuthService } from './auth.service';
import { CROSS_TAB_LOCK, PAGE_RELOAD, SESSION_CHANNEL, SessionChannel } from './session-sync';

const dispatcher: CurrentUser = {
  id: '11111111-1111-1111-1111-111111111111',
  email: 'dispatcher@atlas.local',
  fullName: 'Test Dispatcher',
  role: 'DISPATCHER',
  enabled: true,
};

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('starts logged out', () => {
    expect(service.isLoggedIn()).toBe(false);
    expect(service.token()).toBeNull();
    expect(service.user()).toBeNull();
  });

  it('keeps the token and loads the user after a login', () => {
    let result: CurrentUser | undefined;
    service.login('dispatcher@atlas.local', 'secret123').subscribe((user) => (result = user));

    const login = http.expectOne('/api/auth/login');
    expect(login.request.method).toBe('POST');
    expect(login.request.body).toEqual({ email: 'dispatcher@atlas.local', password: 'secret123' });
    login.flush({ accessToken: 'access-1', tokenType: 'Bearer', expiresIn: 900 });

    http.expectOne('/api/auth/me').flush(dispatcher);

    expect(result).toEqual(dispatcher);
    expect(service.token()).toBe('access-1');
    expect(service.isLoggedIn()).toBe(true);
    expect(service.hasRole('DISPATCHER')).toBe(true);
    expect(service.hasRole('ADMIN')).toBe(false);
  });

  it('stays logged out when the login is rejected', () => {
    let failed = false;
    service.login('dispatcher@atlas.local', 'wrong').subscribe({ error: () => (failed = true) });

    http
      .expectOne('/api/auth/login')
      .flush({ detail: 'Invalid email or password' }, { status: 401, statusText: 'Unauthorized' });

    expect(failed).toBe(true);
    expect(service.isLoggedIn()).toBe(false);
    expect(service.token()).toBeNull();
  });

  it('restores a session from the refresh cookie', () => {
    let restored: boolean | undefined;
    service.restoreSession().subscribe((value) => (restored = value));

    http
      .expectOne('/api/auth/refresh')
      .flush({ accessToken: 'access-2', tokenType: 'Bearer', expiresIn: 900 });
    http.expectOne('/api/auth/me').flush(dispatcher);

    expect(restored).toBe(true);
    expect(service.token()).toBe('access-2');
    expect(service.user()).toEqual(dispatcher);
  });

  it('reports no session when the refresh is rejected', () => {
    let restored: boolean | undefined;
    service.restoreSession().subscribe((value) => (restored = value));

    http.expectOne('/api/auth/refresh').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(restored).toBe(false);
    expect(service.isLoggedIn()).toBe(false);
  });

  it('sends one refresh request when several callers ask at once', () => {
    const tokens: string[] = [];
    service.refresh().subscribe((token) => tokens.push(token));
    service.refresh().subscribe((token) => tokens.push(token));

    http
      .expectOne('/api/auth/refresh')
      .flush({ accessToken: 'access-3', tokenType: 'Bearer', expiresIn: 900 });

    expect(tokens).toEqual(['access-3', 'access-3']);
  });

  it('forgets the user on logout, even when the request fails', () => {
    service.login('dispatcher@atlas.local', 'secret123').subscribe();
    http
      .expectOne('/api/auth/login')
      .flush({ accessToken: 'access-1', tokenType: 'Bearer', expiresIn: 900 });
    http.expectOne('/api/auth/me').flush(dispatcher);

    service.logout().subscribe();
    http.expectOne('/api/auth/logout').flush(null, { status: 500, statusText: 'Server Error' });

    expect(service.isLoggedIn()).toBe(false);
    expect(service.token()).toBeNull();
  });
});

describe('AuthService across browser tabs', () => {
  let service: AuthService;
  let http: HttpTestingController;
  let channel: SessionChannel & { sent: unknown[]; closed: boolean; receive: (message: unknown) => void };
  let reloads: number;
  let lockUses: number;

  function tokenFor(userId: string): string {
    return `header.${btoa(JSON.stringify({ sub: userId }))}.signature`;
  }

  beforeEach(() => {
    reloads = 0;
    lockUses = 0;
    channel = {
      sent: [],
      closed: false,
      receive: () => undefined,
      listen(handler: (message: unknown) => void) {
        this.receive = handler;
      },
      postMessage(message: unknown) {
        this.sent.push(message);
      },
      close() {
        this.closed = true;
      },
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: SESSION_CHANNEL, useValue: channel },
        { provide: PAGE_RELOAD, useValue: () => reloads++ },
        {
          provide: CROSS_TAB_LOCK,
          useValue: <T>(work: () => Promise<T>) => {
            lockUses++;
            return work();
          },
        },
      ],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function logIn(): void {
    service.login('dispatcher@atlas.local', 'secret123').subscribe();
    http.expectOne('/api/auth/login').flush({ accessToken: tokenFor(dispatcher.id), tokenType: 'Bearer', expiresIn: 900 });
    http.expectOne('/api/auth/me').flush(dispatcher);
  }

  it('tells the other tabs when someone logs in or out', () => {
    logIn();
    expect(channel.sent).toEqual(['session-changed']);

    service.logout().subscribe();
    http.expectOne('/api/auth/logout').flush(null);

    expect(channel.sent).toEqual(['session-changed', 'session-changed']);
  });

  it('says nothing to other tabs when it only restores its own session', () => {
    service.restoreSession().subscribe();

    return Promise.resolve().then(() => {
      http.expectOne('/api/auth/refresh').flush({ accessToken: 'access-2', tokenType: 'Bearer', expiresIn: 900 });
      return Promise.resolve().then(() => Promise.resolve()).then(() => {
        http.expectOne('/api/auth/me').flush(dispatcher);
        expect(channel.sent).toEqual([]);
      });
    });
  });

  it('reloads when another tab changes who is logged in', () => {
    logIn();

    channel.receive('session-changed');
    channel.receive('something else');

    expect(reloads).toBe(1);
  });

  it('reloads when a refreshed token belongs to a different user', async () => {
    logIn();

    service.refresh().subscribe();
    await Promise.resolve();
    http
      .expectOne('/api/auth/refresh')
      .flush({ accessToken: tokenFor('22222222-2222-2222-2222-222222222222'), tokenType: 'Bearer', expiresIn: 900 });
    await Promise.resolve();
    await Promise.resolve();

    expect(reloads).toBe(1);
  });

  it('does not reload when a refreshed token belongs to the same user', async () => {
    logIn();

    service.refresh().subscribe();
    await Promise.resolve();
    http
      .expectOne('/api/auth/refresh')
      .flush({ accessToken: tokenFor(dispatcher.id), tokenType: 'Bearer', expiresIn: 900 });
    await Promise.resolve();
    await Promise.resolve();

    expect(reloads).toBe(0);
    expect(lockUses).toBe(1);
  });

  it('stops listening when it is destroyed', () => {
    TestBed.resetTestingModule();

    expect(channel.closed).toBe(true);
  });
});
