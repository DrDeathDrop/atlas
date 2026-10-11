import { HttpClient } from '@angular/common/http';
import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import {
  Observable,
  catchError,
  defer,
  finalize,
  firstValueFrom,
  from,
  map,
  of,
  shareReplay,
  switchMap,
  tap,
} from 'rxjs';
import { CurrentUser, Role, TokenResponse } from './auth.models';
import { CROSS_TAB_LOCK, PAGE_RELOAD, SESSION_CHANGED, SESSION_CHANNEL } from './session-sync';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly channel = inject(SESSION_CHANNEL);
  private readonly crossTabLock = inject(CROSS_TAB_LOCK);
  private readonly reloadPage = inject(PAGE_RELOAD);

  private readonly accessToken = signal<string | null>(null);
  private readonly currentUser = signal<CurrentUser | null>(null);
  private refreshInFlight: Observable<string> | null = null;

  readonly user = this.currentUser.asReadonly();
  readonly isLoggedIn = computed(() => this.currentUser() !== null);

  constructor() {
    if (this.channel !== null) {
      const channel = this.channel;
      channel.listen((message) => {
        if (message === SESSION_CHANGED) {
          this.reloadPage();
        }
      });
      inject(DestroyRef).onDestroy(() => channel.close());
    }
  }

  token(): string | null {
    return this.accessToken();
  }

  hasRole(...roles: Role[]): boolean {
    const user = this.currentUser();
    return user !== null && roles.includes(user.role);
  }

  login(email: string, password: string): Observable<CurrentUser> {
    return this.http.post<TokenResponse>('/api/auth/login', { email, password }).pipe(
      tap((response) => this.accessToken.set(response.accessToken)),
      switchMap(() => this.loadCurrentUser()),
      tap(() => this.announceChange()),
    );
  }

  refresh(): Observable<string> {
    if (this.refreshInFlight === null) {
      this.refreshInFlight = this.oneTabAtATime(() => this.http.post<TokenResponse>('/api/auth/refresh', null)).pipe(
        map((response) => response.accessToken),
        tap((token) => this.accept(token)),
        finalize(() => (this.refreshInFlight = null)),
        shareReplay(1),
      );
    }
    return this.refreshInFlight;
  }

  restoreSession(): Observable<boolean> {
    return this.refresh().pipe(
      switchMap(() => this.loadCurrentUser()),
      map(() => true),
      catchError(() => {
        this.clear();
        return of(false);
      }),
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>('/api/auth/logout', null).pipe(
      catchError(() => of(undefined)),
      tap(() => {
        this.clear();
        this.announceChange();
      }),
    );
  }

  clear(): void {
    this.accessToken.set(null);
    this.currentUser.set(null);
  }

  private accept(token: string): void {
    this.accessToken.set(token);

    const current = this.currentUser();
    const subject = subjectOf(token);
    if (current !== null && subject !== null && subject !== current.id) {
      this.reloadPage();
    }
  }

  private oneTabAtATime<T>(request: () => Observable<T>): Observable<T> {
    const lock = this.crossTabLock;
    return lock === null ? defer(request) : defer(() => from(lock(() => firstValueFrom(request()))));
  }

  private announceChange(): void {
    this.channel?.postMessage(SESSION_CHANGED);
  }

  private loadCurrentUser(): Observable<CurrentUser> {
    return this.http.get<CurrentUser>('/api/auth/me').pipe(tap((user) => this.currentUser.set(user)));
  }
}

function subjectOf(token: string): string | null {
  const payload = token.split('.')[1];
  if (!payload) {
    return null;
  }

  try {
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    const subject = (JSON.parse(json) as { sub?: unknown }).sub;
    return typeof subject === 'string' ? subject : null;
  } catch {
    return null;
  }
}
