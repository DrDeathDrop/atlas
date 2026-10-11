import { InjectionToken } from '@angular/core';

export interface SessionChannel {
  listen(handler: (message: unknown) => void): void;
  postMessage(message: unknown): void;
  close(): void;
}

export type CrossTabLock = <T>(work: () => Promise<T>) => Promise<T>;

export const SESSION_CHANGED = 'session-changed';

export const SESSION_CHANNEL = new InjectionToken<SessionChannel | null>('SESSION_CHANNEL', {
  providedIn: 'root',
  factory: () => {
    if (typeof BroadcastChannel === 'undefined') {
      return null;
    }

    const channel = new BroadcastChannel('atlas-session');
    return {
      listen: (handler) => {
        channel.onmessage = (event) => handler(event.data);
      },
      postMessage: (message) => channel.postMessage(message),
      close: () => channel.close(),
    };
  },
});

export const CROSS_TAB_LOCK = new InjectionToken<CrossTabLock | null>('CROSS_TAB_LOCK', {
  providedIn: 'root',
  factory: () => {
    const locks = typeof navigator === 'undefined' ? undefined : navigator.locks;
    return locks ? (work) => locks.request('atlas-token-refresh', work) : null;
  },
});

export const PAGE_RELOAD = new InjectionToken<() => void>('PAGE_RELOAD', {
  providedIn: 'root',
  factory: () => () => window.location.reload(),
});
