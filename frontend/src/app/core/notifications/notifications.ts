import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface AppNotification {
  id: string;
  type: 'INCIDENT_REPORTED' | 'INCIDENT_STATUS_CHANGED' | 'RESOURCE_ASSIGNED' | 'RESOURCE_RELEASED';
  title: string;
  incidentId: string | null;
  createdAt: string;
  read: boolean;
}

@Injectable({ providedIn: 'root' })
export class Notifications {
  private readonly http = inject(HttpClient);

  latest(): Observable<AppNotification[]> {
    return this.http.get<AppNotification[]>('/api/notifications');
  }

  unreadCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>('/api/notifications/unread-count');
  }

  markRead(notificationId: string): Observable<void> {
    return this.http.post<void>(`/api/notifications/${notificationId}/read`, null);
  }

  markAllRead(): Observable<void> {
    return this.http.post<void>('/api/notifications/read-all', null);
  }
}
