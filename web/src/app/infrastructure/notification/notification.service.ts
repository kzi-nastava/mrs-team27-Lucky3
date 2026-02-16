import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Subscription } from 'rxjs';
import { AppNotification, BackendNotificationResponse, NotificationType } from '../../model/notification.model';
import { SocketService } from '../rest/socket.service';
import { AuthService } from '../auth/auth.service';
import { PanicResponse } from '../rest/panic.service';
import { environment } from '../../../env/environment';

/** Paginated response shape from Spring Data */
interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService implements OnDestroy {
  private readonly MAX_NOTIFICATIONS = 50;
  private readonly API_URL = environment.apiHost + 'notification';

  private notificationsSubject = new BehaviorSubject<AppNotification[]>([]);
  private unreadCountSubject = new BehaviorSubject<number>(0);

  notifications$ = this.notificationsSubject.asObservable();
  unreadCount$ = this.unreadCountSubject.asObservable();

  /** Emits each time a brand-new notification arrives (for sound / visual cues). */
  private latestSubject = new BehaviorSubject<AppNotification | null>(null);
  latest$ = this.latestSubject.asObservable();

  private panicSub: Subscription | null = null;
  private userNotifSub: Subscription | null = null;
  private audioContext: AudioContext | null = null;
  private initialized = false;
  private authSub: Subscription | null = null;

  constructor(
    private http: HttpClient,
    private socketService: SocketService,
    private authService: AuthService
  ) {
    // Auto-teardown when user logs out (user$ emits null)
    this.authSub = this.authService.user$.subscribe((role) => {
      if (!role && this.initialized) {
        this.teardown();
      }
    });
  }

  // ------------------------------------------------------------------
  // Lifecycle
  // ------------------------------------------------------------------

  /**
   * Call once after the user is known to be authenticated.
   * Loads persisted notifications from the backend and sets up
   * role-specific WebSocket subscriptions.
   */
  init(): void {
    if (this.initialized) return;
    this.initialized = true;

    // 1. Fetch persisted notification history from the database
    this.fetchNotificationsFromBackend();

    // 2. Fetch unread count for badge
    this.fetchUnreadCount();

    // 3. Subscribe to the per-user WebSocket queue for real-time pushes
    const userId = this.authService.getUserId();
    if (userId) {
      this.userNotifSub = this.socketService
        .subscribeToUserNotifications(userId)
        .subscribe({
          next: (backendNotif: BackendNotificationResponse) => {
            const mapped = this.mapBackendToApp(backendNotif);
            this.addNotification(mapped);

            // Play siren for CRITICAL priority (PANIC), normal ding otherwise
            if (backendNotif.priority === 'CRITICAL') {
              this.playSirenSound();
            } else {
              this.playNotificationSound();
            }
          },
          error: (err) => console.error('User notification subscription error:', err)
        });
    }

    // 4. Admins also subscribe to the existing /topic/panic for backward compat
    const role = this.authService.getRole();
    if (role === 'ADMIN') {
      this.subscribeToAdminNotifications();
    }
  }

  /** Tear down subscriptions and clear in-memory state (e.g. on logout). */
  teardown(): void {
    this.panicSub?.unsubscribe();
    this.panicSub = null;
    this.userNotifSub?.unsubscribe();
    this.userNotifSub = null;
    this.initialized = false;
    this.notificationsSubject.next([]);
    this.unreadCountSubject.next(0);
  }

  ngOnDestroy(): void {
    this.teardown();
  }

  // ------------------------------------------------------------------
  // Backend REST calls
  // ------------------------------------------------------------------

  /** Loads the first page of notifications from GET /api/notification */
  private fetchNotificationsFromBackend(): void {
    const params = new HttpParams()
      .set('page', '0')
      .set('size', String(this.MAX_NOTIFICATIONS));

    this.http
      .get<PageResponse<BackendNotificationResponse>>(this.API_URL, { params })
      .subscribe({
        next: (page) => {
          const mapped = page.content.map(n => this.mapBackendToApp(n));
          this.notificationsSubject.next(mapped);
          this.updateUnreadCount(mapped);
        },
        error: (err) => console.error('Failed to fetch notifications:', err)
      });
  }

  /** Fetches the unread count from the backend for the badge */
  private fetchUnreadCount(): void {
    this.http
      .get<{ unreadCount: number }>(`${this.API_URL}/unread`)
      .subscribe({
        next: (res) => this.unreadCountSubject.next(res.unreadCount),
        error: (err) => console.error('Failed to fetch unread count:', err)
      });
  }

  // ------------------------------------------------------------------
  // Public API
  // ------------------------------------------------------------------

  addNotification(notification: AppNotification): void {
    const current = this.notificationsSubject.value;
    if (current.some(n => n.id === notification.id)) return; // dedupe
    const updated = [notification, ...current].slice(0, this.MAX_NOTIFICATIONS);
    this.notificationsSubject.next(updated);
    this.updateUnreadCount(updated);
    this.latestSubject.next(notification);
  }

  markAsRead(id: string): void {
    // Optimistic UI update
    const updated = this.notificationsSubject.value.map(n =>
      n.id === id ? { ...n, read: true } : n
    );
    this.notificationsSubject.next(updated);
    this.updateUnreadCount(updated);

    // Persist to backend (extract numeric ID)
    const numericId = this.extractNumericId(id);
    if (numericId !== null) {
      this.http.put(`${this.API_URL}/${numericId}/read`, {}).subscribe({
        error: (err) => console.error('Failed to mark notification as read:', err)
      });
    }
  }

  markAllAsRead(): void {
    const updated = this.notificationsSubject.value.map(n => ({ ...n, read: true }));
    this.notificationsSubject.next(updated);
    this.updateUnreadCount(updated);

    // Persist to backend
    this.http.put(`${this.API_URL}/read-all`, {}).subscribe({
      error: (err) => console.error('Failed to mark all notifications as read:', err)
    });
  }

  clearAll(): void {
    this.notificationsSubject.next([]);
    this.unreadCountSubject.next(0);
  }

  // ------------------------------------------------------------------
  // Sound
  // ------------------------------------------------------------------

  playNotificationSound(): void {
    try {
      if (!this.audioContext) {
        this.audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
      }
      const ctx = this.audioContext;
      const now = ctx.currentTime;

      // Two-tone "ding"
      const osc1 = ctx.createOscillator();
      const gain1 = ctx.createGain();
      osc1.connect(gain1);
      gain1.connect(ctx.destination);
      osc1.frequency.value = 880;
      osc1.type = 'sine';
      gain1.gain.setValueAtTime(0.25, now);
      gain1.gain.exponentialRampToValueAtTime(0.001, now + 0.35);
      osc1.start(now);
      osc1.stop(now + 0.35);

      const osc2 = ctx.createOscillator();
      const gain2 = ctx.createGain();
      osc2.connect(gain2);
      gain2.connect(ctx.destination);
      osc2.frequency.value = 1320;
      osc2.type = 'sine';
      gain2.gain.setValueAtTime(0, now);
      gain2.gain.setValueAtTime(0.2, now + 0.15);
      gain2.gain.exponentialRampToValueAtTime(0.001, now + 0.5);
      osc2.start(now + 0.15);
      osc2.stop(now + 0.5);
    } catch (e) {
      console.warn('Could not play notification sound', e);
    }
  }

  /** Loud siren for PANIC / CRITICAL notifications */
  playSirenSound(): void {
    try {
      if (!this.audioContext) {
        this.audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
      }
      const ctx = this.audioContext;
      const now = ctx.currentTime;
      const duration = 2.0;

      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.type = 'sawtooth';
      gain.gain.setValueAtTime(0.3, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + duration);

      // Sweep between 600Hz and 1200Hz for siren effect
      for (let t = 0; t < duration; t += 0.4) {
        osc.frequency.setValueAtTime(600, now + t);
        osc.frequency.linearRampToValueAtTime(1200, now + t + 0.2);
        osc.frequency.linearRampToValueAtTime(600, now + t + 0.4);
      }

      osc.start(now);
      osc.stop(now + duration);
    } catch (e) {
      console.warn('Could not play siren sound', e);
    }
  }

  // ------------------------------------------------------------------
  // Private helpers
  // ------------------------------------------------------------------

  private subscribeToAdminNotifications(): void {
    this.panicSub = this.socketService.subscribeToPanicAlerts().subscribe({
      next: (panic: PanicResponse) => {
        const who = panic.user
          ? `${panic.user.name} ${panic.user.surname}`
          : 'Unknown user';
        const rideId = panic.ride?.id ?? '?';

        // Only add if not already received via the user notification queue
        const panicAppId = `panic-${panic.id}`;
        if (this.notificationsSubject.value.some(n => n.id === panicAppId)) return;

        this.addNotification({
          id: panicAppId,
          type: 'panic',
          title: 'Panic Alert',
          message: `Ride #${rideId} — ${who} pressed panic`,
          timestamp: panic.time || new Date().toISOString(),
          read: false,
          route: '/admin/panic',
          priority: 'CRITICAL',
          data: { panicId: panic.id, rideId: panic.ride?.id }
        });
        this.playSirenSound();
      },
      error: (err) => console.error('Panic notification subscription error:', err)
    });
  }

  /**
   * Maps a backend NotificationResponse DTO to the frontend AppNotification model.
   */
  private mapBackendToApp(n: BackendNotificationResponse): AppNotification {
    return {
      id: `notif-${n.id}`,
      type: this.mapNotificationType(n.type),
      title: this.mapTitle(n.type),
      message: n.text,
      timestamp: n.timestamp,
      read: n.read,
      route: this.mapRoute(n.type, n.relatedEntityId),
      priority: n.priority,
      data: n.relatedEntityId ? { relatedEntityId: n.relatedEntityId } : undefined
    };
  }

  private mapNotificationType(backendType: string): NotificationType {
    switch (backendType) {
      case 'PANIC':           return 'panic';
      case 'RIDE_STATUS':     return 'ride';
      case 'RIDE_INVITE':     return 'ride_invite';
      case 'RIDE_FINISHED':   return 'ride_finished';
      case 'RIDE_CREATED':    return 'ride_created';
      case 'RIDE_CANCELLED':  return 'ride_cancelled';
      case 'RIDE_SCHEDULED_REMINDER': return 'ride_scheduled_reminder';
      case 'SUPPORT':         return 'support';
      case 'DRIVER_ASSIGNMENT': return 'driver_assignment';
      default:                return 'system';
    }
  }

  private mapTitle(backendType: string): string {
    switch (backendType) {
      case 'PANIC':             return 'Panic Alert';
      case 'RIDE_STATUS':       return 'Ride Update';
      case 'RIDE_INVITE':       return 'Ride Invitation';
      case 'RIDE_FINISHED':     return 'Ride Completed';
      case 'RIDE_CREATED':      return 'Ride Created';
      case 'RIDE_CANCELLED':    return 'Ride Cancelled';
      case 'RIDE_SCHEDULED_REMINDER': return 'Ride Reminder';
      case 'SUPPORT':           return 'Support Message';
      case 'DRIVER_ASSIGNMENT': return 'New Ride Assignment';
      default:                  return 'Notification';
    }
  }

  private mapRoute(backendType: string, relatedEntityId: number | null): string | undefined {
    if (!relatedEntityId) return undefined;
    const role = this.authService.getRole();
    switch (backendType) {
      case 'PANIC':             return '/admin/panic';
      case 'RIDE_FINISHED':
      case 'RIDE_CANCELLED': {
        if (role === 'DRIVER') {
          return `/driver/overview/ride/${relatedEntityId}`;
        }
        return `/passenger/ride-history?rideId=${relatedEntityId}`;
      }
      case 'RIDE_STATUS':
      case 'RIDE_INVITE':
      case 'RIDE_CREATED':
      case 'RIDE_SCHEDULED_REMINDER': {
        if (role === 'DRIVER') {
          return `/driver/ride/${relatedEntityId}`;
        }
        return `/passenger/ride/${relatedEntityId}`;
      }
      case 'DRIVER_ASSIGNMENT': return `/driver/dashboard`;
      case 'SUPPORT': {
        if (role === 'ADMIN') {
          return `/admin/support?chatId=${relatedEntityId}`;
        }
        return `/support`;
      }
      default:                  return undefined;
    }
  }

  /** Extracts the numeric portion from IDs like "notif-42" or "panic-7" */
  private extractNumericId(id: string): number | null {
    const match = id.match(/^notif-(\d+)$/);
    return match ? parseInt(match[1], 10) : null;
  }

  private updateUnreadCount(notifications: AppNotification[]): void {
    this.unreadCountSubject.next(notifications.filter(n => !n.read).length);
  }
}
