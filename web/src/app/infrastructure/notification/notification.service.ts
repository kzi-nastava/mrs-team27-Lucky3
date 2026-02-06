import { Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject, Subscription } from 'rxjs';
import { AppNotification } from '../../model/notification.model';
import { SocketService } from '../rest/socket.service';
import { AuthService } from '../auth/auth.service';
import { PanicResponse } from '../rest/panic.service';

@Injectable({
  providedIn: 'root'
})
export class NotificationService implements OnDestroy {
  private readonly STORAGE_KEY = 'app_notifications';
  private readonly MAX_NOTIFICATIONS = 50;

  private notificationsSubject = new BehaviorSubject<AppNotification[]>([]);
  private unreadCountSubject = new BehaviorSubject<number>(0);

  notifications$ = this.notificationsSubject.asObservable();
  unreadCount$ = this.unreadCountSubject.asObservable();

  /** Emits each time a brand-new notification arrives (for sound / visual cues). */
  private latestSubject = new BehaviorSubject<AppNotification | null>(null);
  latest$ = this.latestSubject.asObservable();

  private panicSub: Subscription | null = null;
  private audioContext: AudioContext | null = null;
  private initialized = false;

  constructor(
    private socketService: SocketService,
    private authService: AuthService
  ) {
    this.loadFromStorage();
  }

  // ------------------------------------------------------------------
  // Lifecycle
  // ------------------------------------------------------------------

  /**
   * Call once after the user is known to be authenticated.
   * Sets up role-specific WebSocket subscriptions.
   */
  init(): void {
    if (this.initialized) return;
    this.initialized = true;

    const role = this.authService.getRole();
    if (role === 'ADMIN') {
      this.subscribeToAdminNotifications();
    }
    // Future: add DRIVER / PASSENGER subscriptions here
  }

  /** Tear down subscriptions and clear in-memory state (e.g. on logout). */
  teardown(): void {
    this.panicSub?.unsubscribe();
    this.panicSub = null;
    this.initialized = false;
  }

  ngOnDestroy(): void {
    this.teardown();
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
    this.saveToStorage(updated);
    this.latestSubject.next(notification);
  }

  markAsRead(id: string): void {
    const updated = this.notificationsSubject.value.map(n =>
      n.id === id ? { ...n, read: true } : n
    );
    this.notificationsSubject.next(updated);
    this.updateUnreadCount(updated);
    this.saveToStorage(updated);
  }

  markAllAsRead(): void {
    const updated = this.notificationsSubject.value.map(n => ({ ...n, read: true }));
    this.notificationsSubject.next(updated);
    this.updateUnreadCount(updated);
    this.saveToStorage(updated);
  }

  clearAll(): void {
    this.notificationsSubject.next([]);
    this.unreadCountSubject.next(0);
    this.saveToStorage([]);
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

      // Two-tone "ding" — pleasant and short
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

        this.addNotification({
          id: `panic-${panic.id}`,
          type: 'panic',
          title: 'Panic Alert',
          message: `Ride #${rideId} — ${who} pressed panic`,
          timestamp: panic.time || new Date().toISOString(),
          read: false,
          route: '/admin/panic',
          data: { panicId: panic.id, rideId: panic.ride?.id }
        });
        this.playNotificationSound();
      },
      error: (err) => console.error('Panic notification subscription error:', err)
    });
  }

  private updateUnreadCount(notifications: AppNotification[]): void {
    this.unreadCountSubject.next(notifications.filter(n => !n.read).length);
  }

  private loadFromStorage(): void {
    try {
      const stored = localStorage.getItem(this.STORAGE_KEY);
      if (stored) {
        const parsed: AppNotification[] = JSON.parse(stored);
        this.notificationsSubject.next(parsed);
        this.updateUnreadCount(parsed);
      }
    } catch {
      // Corrupt storage — start fresh
      localStorage.removeItem(this.STORAGE_KEY);
    }
  }

  private saveToStorage(notifications: AppNotification[]): void {
    try {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(notifications));
    } catch {
      // Storage full or unavailable — silent fail
    }
  }
}
