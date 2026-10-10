import { Injectable, InjectionToken, inject, signal } from '@angular/core';
import { Client, StompConfig } from '@stomp/stompjs';
import { Observable, Subject, debounceTime, filter } from 'rxjs';
import { AuthService } from '../auth/auth.service';

export type LiveUpdateKind = 'INCIDENT' | 'RESOURCE' | 'FACILITY' | 'ZONE' | 'NOTIFICATION' | 'RESYNC';

export interface LiveUpdate {
  kind: LiveUpdateKind;
  id: string | null;
}

export interface LiveClient {
  connectHeaders: Record<string, string>;
  activate(): void;
  deactivate(): Promise<void>;
  subscribe(destination: string, callback: (message: { body: string }) => void): unknown;
}

export const LIVE_CLIENT_FACTORY = new InjectionToken<(config: StompConfig) => LiveClient>('LIVE_CLIENT_FACTORY', {
  providedIn: 'root',
  factory: () => (config) => new Client(config),
});

const TOPIC = '/topic/updates';
const QUIET_MILLIS = 250;

@Injectable({ providedIn: 'root' })
export class LiveUpdates {
  private readonly auth = inject(AuthService);
  private readonly createClient = inject(LIVE_CLIENT_FACTORY);

  private readonly updates = new Subject<LiveUpdate>();
  private client: LiveClient | null = null;
  private connectedBefore = false;

  readonly connected = signal(false);

  when(matches: (update: LiveUpdate) => boolean): Observable<LiveUpdate> {
    return this.updates.pipe(
      filter((update) => update.kind === 'RESYNC' || matches(update)),
      debounceTime(QUIET_MILLIS),
    );
  }

  start(): void {
    if (this.client !== null) {
      return;
    }

    const client = this.createClient({
      brokerURL: socketUrl(),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      beforeConnect: () => {
        client.connectHeaders = { Authorization: `Bearer ${this.auth.token() ?? ''}` };
      },
      onConnect: () => {
        client.subscribe(TOPIC, (message) => this.receive(message.body));
        this.connected.set(true);
        if (this.connectedBefore) {
          this.updates.next({ kind: 'RESYNC', id: null });
        }
        this.connectedBefore = true;
      },
      onWebSocketClose: () => this.connected.set(false),
      onStompError: () => {
        this.connected.set(false);
        this.auth.refresh().subscribe({ error: () => undefined });
      },
    });

    this.client = client;
    client.activate();
  }

  stop(): void {
    void this.client?.deactivate();
    this.client = null;
    this.connectedBefore = false;
    this.connected.set(false);
  }

  private receive(body: string): void {
    try {
      const update = JSON.parse(body) as LiveUpdate;
      if (typeof update?.kind === 'string') {
        this.updates.next(update);
      }
    } catch {
      return;
    }
  }
}

function socketUrl(): string {
  const scheme = window.location.protocol === 'https:' ? 'wss' : 'ws';
  return `${scheme}://${window.location.host}/ws`;
}
