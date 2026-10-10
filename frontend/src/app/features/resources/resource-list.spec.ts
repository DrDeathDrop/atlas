import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ResourceList } from './resource-list';
import { Resource } from './resource.models';

const ambulance: Resource = {
  id: 'r1',
  callSign: 'AMBULANCE-17',
  kind: 'VEHICLE',
  type: 'AMBULANCE',
  status: 'EN_ROUTE',
  latitude: 42.15,
  longitude: 24.75,
  teamId: null,
};

describe('ResourceList', () => {
  let fixture: ComponentFixture<ResourceList>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ResourceList],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(ResourceList);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function text(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  it('shows each resource with readable labels', async () => {
    http.expectOne('/api/resources').flush([ambulance]);
    await fixture.whenStable();

    expect(text()).toContain('AMBULANCE-17');
    expect(text()).toContain('Vehicle');
    expect(text()).toContain('En route');
    expect(text()).toContain('42.1500, 24.7500');
  });

  it('says so when there are no resources', async () => {
    http.expectOne('/api/resources').flush([]);
    await fixture.whenStable();

    expect(text()).toContain('No teams or vehicles have been added yet');
  });

  it('shows an error when loading fails', async () => {
    http.expectOne('/api/resources').flush(null, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    expect(text()).toContain('could not be loaded');
  });
});
