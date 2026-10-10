import { DatePipe } from '@angular/common';
import { Component, ElementRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { Router } from '@angular/router';
import { LiveUpdates } from '../core/live/live-updates';
import { AppNotification, Notifications } from '../core/notifications/notifications';

@Component({
  selector: 'app-notification-bell',
  imports: [DatePipe, MatButtonModule],
  templateUrl: './notification-bell.html',
  styleUrl: './notification-bell.scss',
  host: {
    '(document:click)': 'closeIfOutside($event)',
    '(document:keydown.escape)': 'open.set(false)',
  },
})
export class NotificationBell implements OnInit {
  private readonly notifications = inject(Notifications);
  private readonly live = inject(LiveUpdates);
  private readonly router = inject(Router);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly unread = signal(0);
  protected readonly items = signal<AppNotification[] | null>(null);
  protected readonly open = signal(false);

  constructor() {
    this.live
      .when((update) => update.kind === 'NOTIFICATION')
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.refresh());
  }

  ngOnInit(): void {
    this.refresh();
  }

  protected toggle(): void {
    this.open.update((open) => !open);
    if (this.open()) {
      this.loadItems();
    }
  }

  protected select(notification: AppNotification): void {
    this.open.set(false);

    if (!notification.read) {
      this.notifications.markRead(notification.id).subscribe({
        next: () => this.refresh(),
        error: () => undefined,
      });
    }

    if (notification.incidentId !== null) {
      void this.router.navigate(['/incidents', notification.incidentId]);
    }
  }

  protected markAllRead(): void {
    this.notifications.markAllRead().subscribe({
      next: () => this.refresh(),
      error: () => undefined,
    });
  }

  protected closeIfOutside(event: Event): void {
    if (this.open() && !this.host.nativeElement.contains(event.target as Node)) {
      this.open.set(false);
    }
  }

  private refresh(): void {
    this.notifications.unreadCount().subscribe({
      next: (unread) => this.unread.set(unread.count),
      error: () => undefined,
    });

    if (this.open()) {
      this.loadItems();
    }
  }

  private loadItems(): void {
    this.notifications.latest().subscribe({
      next: (items) => this.items.set(items),
      error: () => this.items.set([]),
    });
  }
}
