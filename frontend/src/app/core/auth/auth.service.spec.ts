import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CurrentUser } from './auth.models';
import { AuthService } from './auth.service';

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
