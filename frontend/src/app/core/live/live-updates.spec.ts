import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { StompConfig } from '@stomp/stompjs';
import { LIVE_CLIENT_FACTORY, LiveClient, LiveUpdate, LiveUpdates } from './live-updates';

class FakeClient implements LiveClient {
  connectHeaders: Record<string, string> = {};
  activated = 0;
  deactivated = 0;
  destinations: string[] = [];
  private callback: ((message: { body: string }) => void) | null = null;

  constructor(readonly config: StompConfig) {}

  activate(): void {
    this.activated++;
  }

  deactivate(): Promise<void> {
    this.deactivated++;
    return Promise.resolve();
  }

  subscribe(destination: string, callback: (message: { body: string }) => void): unknown {
    this.destinations.push(destination);
    this.callback = callback;
    return {};
  }

  connect(): void {
    void this.config.beforeConnect?.(undefined as never);
    this.config.onConnect?.(undefined as never);
  }

  deliver(body: string): void {
    this.callback?.({ body });
  }
}

describe('LiveUpdates', () => {
  let live: LiveUpdates;
  let http: HttpTestingController;
  let clients: FakeClient[];

  beforeEach(() => {
    vi.useFakeTimers();
    clients = [];
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: LIVE_CLIENT_FACTORY,
          useValue: (config: StompConfig) => {
            const client = new FakeClient(config);
            clients.push(client);
            return client;
          },
        },
      ],
    });
    live = TestBed.inject(LiveUpdates);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    vi.useRealTimers();
  });

  function collect(matches: (update: LiveUpdate) => boolean = () => true): LiveUpdate[] {
    const received: LiveUpdate[] = [];
    live.when(matches).subscribe((update) => received.push(update));
    return received;
  }

  it('opens one connection to the updates topic', () => {
    live.start();
    live.start();

    expect(clients.length).toBe(1);
    expect(clients[0].activated).toBe(1);
    expect(clients[0].config.brokerURL).toMatch(/^ws:\/\/.+\/ws$/);
    expect(live.connected()).toBe(false);

    clients[0].connect();

    expect(clients[0].destinations).toEqual(['/topic/updates']);
    expect(clients[0].connectHeaders['Authorization']).toMatch(/^Bearer /);
    expect(live.connected()).toBe(true);
  });

  it('passes on the updates a page asked for', () => {
    live.start();
    clients[0].connect();
    const received = collect((update) => update.kind === 'INCIDENT');

    clients[0].deliver(JSON.stringify({ kind: 'RESOURCE', id: 'r1' }));
    vi.advanceTimersByTime(300);
    expect(received).toEqual([]);

    clients[0].deliver(JSON.stringify({ kind: 'INCIDENT', id: 'i1' }));
    vi.advanceTimersByTime(300);
    expect(received).toEqual([{ kind: 'INCIDENT', id: 'i1' }]);
  });

  it('turns a burst of updates into one', () => {
    live.start();
    clients[0].connect();
    const received = collect();

    clients[0].deliver(JSON.stringify({ kind: 'INCIDENT', id: 'i1' }));
    clients[0].deliver(JSON.stringify({ kind: 'RESOURCE', id: 'r1' }));
    clients[0].deliver(JSON.stringify({ kind: 'INCIDENT', id: 'i2' }));
    vi.advanceTimersByTime(300);

    expect(received.length).toBe(1);
  });

  it('ignores messages it cannot read', () => {
    live.start();
    clients[0].connect();
    const received = collect();

    clients[0].deliver('not json');
    clients[0].deliver(JSON.stringify({ something: 'else' }));
    vi.advanceTimersByTime(300);

    expect(received).toEqual([]);
  });

  it('asks every page to reload after the connection comes back', () => {
    live.start();
    clients[0].connect();
    const received = collect((update) => update.kind === 'ZONE');

    vi.advanceTimersByTime(300);
    expect(received).toEqual([]);

    clients[0].config.onWebSocketClose?.(undefined as never);
    expect(live.connected()).toBe(false);

    clients[0].connect();
    vi.advanceTimersByTime(300);

    expect(live.connected()).toBe(true);
    expect(received).toEqual([{ kind: 'RESYNC', id: null }]);
  });

  it('refreshes the access token when the server refuses the connection', () => {
    live.start();

    clients[0].config.onStompError?.(undefined as never);

    http.expectOne('/api/auth/refresh').flush({ accessToken: 'new', tokenType: 'Bearer', expiresIn: 900 });
    clients[0].connect();
    expect(clients[0].connectHeaders['Authorization']).toBe('Bearer new');
  });

  it('closes the connection when stopped', () => {
    live.start();
    clients[0].connect();

    live.stop();

    expect(clients[0].deactivated).toBe(1);
    expect(live.connected()).toBe(false);

    live.start();
    expect(clients.length).toBe(2);
  });
});
