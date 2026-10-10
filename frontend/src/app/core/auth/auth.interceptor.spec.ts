import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  let client: HttpClient;
  let http: HttpTestingController;
  let auth: AuthService;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
  });

  afterEach(() => http.verify());

  function logIn(token: string): void {
    auth.login('dispatcher@atlas.local', 'secret123').subscribe();
    http.expectOne('/api/auth/login').flush({ accessToken: token, tokenType: 'Bearer', expiresIn: 900 });
    http.expectOne('/api/auth/me').flush({
      id: '11111111-1111-1111-1111-111111111111',
      email: 'dispatcher@atlas.local',
      fullName: 'Test Dispatcher',
      role: 'DISPATCHER',
      enabled: true,
    });
  }

  it('adds the access token to API requests', () => {
    logIn('access-1');

    client.get('/api/incidents').subscribe();

    const request = http.expectOne('/api/incidents');
    expect(request.request.headers.get('Authorization')).toBe('Bearer access-1');
    request.flush([]);
  });

  it('sends no token to the login, refresh and logout endpoints', () => {
    logIn('access-1');

    client.post('/api/auth/refresh', null).subscribe();
    client.post('/api/auth/logout', null).subscribe();

    const refresh = http.expectOne('/api/auth/refresh');
    const logout = http.expectOne('/api/auth/logout');
    expect(refresh.request.headers.has('Authorization')).toBe(false);
    expect(logout.request.headers.has('Authorization')).toBe(false);
    refresh.flush({ accessToken: 'access-2', tokenType: 'Bearer', expiresIn: 900 });
    logout.flush(null);
  });

  it('leaves requests to other hosts alone', () => {
    logIn('access-1');

    client.get('https://tile.openstreetmap.org/1/1/1.png').subscribe();

    const request = http.expectOne('https://tile.openstreetmap.org/1/1/1.png');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush(null);
  });

  it('refreshes the token and repeats the request after a 401', () => {
    logIn('expired');
    let result: unknown;

    client.get('/api/incidents').subscribe((body) => (result = body));

    http.expectOne('/api/incidents').flush(null, { status: 401, statusText: 'Unauthorized' });
    http
      .expectOne('/api/auth/refresh')
      .flush({ accessToken: 'fresh', tokenType: 'Bearer', expiresIn: 900 });

    const retried = http.expectOne('/api/incidents');
    expect(retried.request.headers.get('Authorization')).toBe('Bearer fresh');
    retried.flush([{ reference: 'INC-2026-0001' }]);

    expect(result).toEqual([{ reference: 'INC-2026-0001' }]);
    expect(auth.token()).toBe('fresh');
  });

  it('logs the user out when the refresh fails too', () => {
    logIn('expired');
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    let failed = false;

    client.get('/api/incidents').subscribe({ error: () => (failed = true) });

    http.expectOne('/api/incidents').flush(null, { status: 401, statusText: 'Unauthorized' });
    http.expectOne('/api/auth/refresh').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(failed).toBe(true);
    expect(auth.isLoggedIn()).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });

  it('passes other errors through without refreshing', () => {
    logIn('access-1');
    let status = 0;

    client.get('/api/incidents').subscribe({ error: (error) => (status = error.status) });

    http.expectOne('/api/incidents').flush(null, { status: 403, statusText: 'Forbidden' });

    expect(status).toBe(403);
  });
});
