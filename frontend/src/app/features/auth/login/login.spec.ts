import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { Login } from './login';

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  let http: HttpTestingController;
  let navigateByUrl: ReturnType<typeof vi.spyOn>;

  async function setUp(queryParams: Record<string, string> = {}): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(queryParams) } } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Login);
    http = TestBed.inject(HttpTestingController);
    navigateByUrl = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    await fixture.whenStable();
  }

  function element(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function type(selector: string, value: string): void {
    const input = element().querySelector<HTMLInputElement>(selector)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  function submit(): void {
    element().querySelector('form')!.dispatchEvent(new Event('submit'));
  }

  function acceptLogin(): void {
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
  }

  afterEach(() => http.verify());

  it('sends nothing while the form is incomplete', async () => {
    await setUp();

    type('input[type=email]', 'not-an-email');
    submit();
    await fixture.whenStable();

    http.expectNone('/api/auth/login');
    expect(navigateByUrl).not.toHaveBeenCalled();
  });

  it('logs in and goes to the incidents page', async () => {
    await setUp();

    type('input[type=email]', 'dispatcher@atlas.local');
    type('input[type=password]', 'secret123');
    submit();
    acceptLogin();

    expect(navigateByUrl).toHaveBeenCalledWith('/incidents');
  });

  it('returns to the page the user was trying to reach', async () => {
    await setUp({ returnUrl: '/resources' });

    type('input[type=email]', 'dispatcher@atlas.local');
    type('input[type=password]', 'secret123');
    submit();
    acceptLogin();

    expect(navigateByUrl).toHaveBeenCalledWith('/resources');
  });

  it('ignores a return address that points to another site', async () => {
    await setUp({ returnUrl: '//evil.example.com' });

    type('input[type=email]', 'dispatcher@atlas.local');
    type('input[type=password]', 'secret123');
    submit();
    acceptLogin();

    expect(navigateByUrl).toHaveBeenCalledWith('/incidents');
  });

  it('shows a message when the credentials are rejected', async () => {
    await setUp();

    type('input[type=email]', 'dispatcher@atlas.local');
    type('input[type=password]', 'wrong');
    submit();
    http
      .expectOne('/api/auth/login')
      .flush({ detail: 'Invalid email or password' }, { status: 401, statusText: 'Unauthorized' });
    await fixture.whenStable();

    expect(element().querySelector('[role=alert]')?.textContent).toContain('Invalid email or password');
    expect(element().querySelector<HTMLButtonElement>('button[type=submit]')!.disabled).toBe(false);
    expect(navigateByUrl).not.toHaveBeenCalled();
  });

  it('shows a general message when the server cannot be reached', async () => {
    await setUp();

    type('input[type=email]', 'dispatcher@atlas.local');
    type('input[type=password]', 'secret123');
    submit();
    http.expectOne('/api/auth/login').flush(null, { status: 503, statusText: 'Service Unavailable' });
    await fixture.whenStable();

    expect(element().querySelector('[role=alert]')?.textContent).toContain('Could not log in');
  });
});
