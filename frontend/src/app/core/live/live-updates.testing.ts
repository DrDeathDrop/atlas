import { signal } from '@angular/core';
import { Observable, Subject, filter } from 'rxjs';
import { LiveUpdate } from './live-updates';

export class LiveUpdatesStub {
  private readonly updates = new Subject<LiveUpdate>();

  readonly connected = signal(false);
  started = 0;
  stopped = 0;

  when(matches: (update: LiveUpdate) => boolean): Observable<LiveUpdate> {
    return this.updates.pipe(filter((update) => update.kind === 'RESYNC' || matches(update)));
  }

  start(): void {
    this.started++;
  }

  stop(): void {
    this.stopped++;
  }

  push(update: LiveUpdate): void {
    this.updates.next(update);
  }
}
