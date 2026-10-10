import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';

describe('authGuard', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
  });

  function runGuard(url: string) {
    const state = { url } as RouterStateSnapshot;
    return TestBed.runInInjectionContext(() => authGuard({} as ActivatedRouteSnapshot, state));
  }

  it('sends a visitor to the login page and remembers where they were going', () => {
    const result = runGuard('/incidents');

    expect(result).toBeInstanceOf(UrlTree);
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/login?returnUrl=%2Fincidents');
  });

  it('lets a logged-in user through', () => {
    const http = TestBed.inject(HttpTestingController);
    TestBed.inject(AuthService).login('dispatcher@atlas.local', 'secret123').subscribe();
    http
      .expectOne('/api/auth/login')
      .flush({ accessToken: 'access-1', tokenType: 'Bearer', expiresIn: 900 });
    http.expectOne('/api/auth/me').flush({
      id: '11111111-1111-1111-1111-111111111111',
      email: 'dispatcher@atlas.local',
      fullName: 'Test Dispatcher',
      role: 'DISPATCHER',
      enabled: true,
    });

    expect(runGuard('/incidents')).toBe(true);
  });
});
