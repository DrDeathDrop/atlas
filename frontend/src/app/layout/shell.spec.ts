import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { LiveUpdates } from '../core/live/live-updates';
import { LiveUpdatesStub } from '../core/live/live-updates.testing';
import { Shell } from './shell';

describe('Shell', () => {
  let fixture: ComponentFixture<Shell>;
  let live: LiveUpdatesStub;

  beforeEach(async () => {
    live = new LiveUpdatesStub();
    await TestBed.configureTestingModule({
      imports: [Shell],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: LiveUpdates, useValue: live },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Shell);
    fixture.detectChanges();
  });

  function status(): string {
    return (fixture.nativeElement as HTMLElement).querySelector('[role=status]')!.textContent!.trim();
  }

  it('opens the live connection while it is shown and closes it afterwards', () => {
    expect(live.started).toBe(1);
    expect(live.stopped).toBe(0);

    fixture.destroy();

    expect(live.stopped).toBe(1);
  });

  it('shows whether updates are arriving live', async () => {
    expect(status()).toBe('Offline');

    live.connected.set(true);
    await fixture.whenStable();

    expect(status()).toBe('Live');
  });
});
