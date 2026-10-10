import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { Role } from './auth.models';
import { AuthService } from './auth.service';
import { roleGuard } from './role.guard';

describe('roleGuard', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
  });

  function logInAs(role: Role): void {
    const http = TestBed.inject(HttpTestingController);
    TestBed.inject(AuthService).login('user@atlas.local', 'secret123').subscribe();
    http.expectOne('/api/auth/login').flush({ accessToken: 't', tokenType: 'Bearer', expiresIn: 900 });
    http.expectOne('/api/auth/me').flush({ id: 'u1', email: 'user@atlas.local', fullName: 'Test User', role, enabled: true });
  }

  function run() {
    return TestBed.runInInjectionContext(() =>
      roleGuard('ADMIN', 'DISPATCHER')({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
  }

  it('lets a user with one of the roles through', () => {
    logInAs('DISPATCHER');

    expect(run()).toBe(true);
  });

  it('sends a user without the role back to the start page', () => {
    logInAs('VIEWER');

    const result = run();

    expect(result).toBeInstanceOf(UrlTree);
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/');
  });
});
