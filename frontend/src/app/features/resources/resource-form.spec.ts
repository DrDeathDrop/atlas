import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { ResourceForm } from './resource-form';

describe('ResourceForm', () => {
  let fixture: ComponentFixture<ResourceForm>;
  let http: HttpTestingController;
  let navigate: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ResourceForm],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(ResourceForm);
    http = TestBed.inject(HttpTestingController);
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  function element(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function set(control: string, value: string): void {
    const field = element().querySelector<HTMLInputElement | HTMLSelectElement>(`[formControlName=${control}]`)!;
    field.value = value;
    field.dispatchEvent(new Event(field.tagName === 'SELECT' ? 'change' : 'input'));
  }

  function fillIn(): void {
    set('callSign', ' ambulance-40 ');
    set('type', 'AMBULANCE');
    set('latitude', '42.14');
    set('longitude', '24.75');
  }

  function submit(): void {
    element().querySelector('form')!.dispatchEvent(new Event('submit'));
  }

  it('sends nothing while the form is incomplete', async () => {
    set('callSign', 'AMBULANCE-40');
    submit();
    await fixture.whenStable();

    http.expectNone('/api/resources');
  });

  it('adds the resource with an upper-case call sign and returns to the list', () => {
    fillIn();
    submit();

    const request = http.expectOne('/api/resources');
    expect(request.request.body).toEqual({
      callSign: 'AMBULANCE-40',
      type: 'AMBULANCE',
      latitude: 42.14,
      longitude: 24.75,
    });
    request.flush({ id: 'r40' });

    expect(navigate).toHaveBeenCalledWith(['/resources']);
  });

  it('shows the reason when the call sign is taken', async () => {
    fillIn();
    submit();
    http
      .expectOne('/api/resources')
      .flush(
        { detail: 'A resource with call sign AMBULANCE-40 already exists' },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();

    expect(element().querySelector('[role=alert]')?.textContent).toContain('already exists');
    expect(navigate).not.toHaveBeenCalled();
  });
});
