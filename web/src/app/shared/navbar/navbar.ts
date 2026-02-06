import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnDestroy,
  OnInit,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { take } from 'rxjs/operators';
import { AuthService } from '../../infrastructure/auth/auth.service';
import { NotificationService } from '../../infrastructure/notification/notification.service';
import { AppNotification } from '../../model/notification.model';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
})
export class Navbar implements OnInit, OnDestroy {
  @Input() title: string = 'Dashboard';
  @Output() toggleSidebar = new EventEmitter<void>();

  isDropdownOpen = false;
  isNotificationsOpen = false;

  // Notification state
  notifications: AppNotification[] = [];
  unreadCount = 0;
  hasNewPulse = false;

  // Logout error modal
  showLogoutError = false;
  logoutErrorMessage = '';

  private notifSub: Subscription | null = null;
  private unreadSub: Subscription | null = null;
  private latestSub: Subscription | null = null;
  private pulseTimer: any;

  constructor(
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private elRef: ElementRef,
    public notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    // Initialise the notification service (role-aware WebSocket subscriptions)
    this.notificationService.init();

    this.notifSub = this.notificationService.notifications$.subscribe((n) => {
      this.notifications = n;
      this.cdr.detectChanges();
    });

    this.unreadSub = this.notificationService.unreadCount$.subscribe((c) => {
      this.unreadCount = c;
      this.cdr.detectChanges();
    });

    // Trigger a brief pulse animation on the bell when a new notification arrives
    this.latestSub = this.notificationService.latest$.subscribe((n) => {
      if (!n) return;
      this.hasNewPulse = true;
      this.cdr.detectChanges();
      clearTimeout(this.pulseTimer);
      this.pulseTimer = setTimeout(() => {
        this.hasNewPulse = false;
        this.cdr.detectChanges();
      }, 2000);
    });
  }

  ngOnDestroy(): void {
    this.notifSub?.unsubscribe();
    this.unreadSub?.unsubscribe();
    this.latestSub?.unsubscribe();
    clearTimeout(this.pulseTimer);
  }

  // ---- Click outside to close panels --------------------------------
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.elRef.nativeElement.contains(event.target)) {
      this.isDropdownOpen = false;
      this.isNotificationsOpen = false;
    }
  }

  // ---- Sidebar -------------------------------------------------------
  toggle(): void {
    this.toggleSidebar.emit();
  }

  // ---- Settings dropdown ---------------------------------------------
  toggleDropdown(): void {
    this.isDropdownOpen = !this.isDropdownOpen;
    if (this.isDropdownOpen) this.isNotificationsOpen = false;
  }

  // ---- Notification panel --------------------------------------------
  toggleNotifications(): void {
    this.isNotificationsOpen = !this.isNotificationsOpen;
    if (this.isNotificationsOpen) this.isDropdownOpen = false;
  }

  onNotificationClick(notification: AppNotification): void {
    this.notificationService.markAsRead(notification.id);
    this.isNotificationsOpen = false;
    if (notification.route) {
      // Parse route and query params if present
      const [path, queryString] = notification.route.split('?');
      if (queryString) {
        const queryParams: { [key: string]: string } = {};
        queryString.split('&').forEach(param => {
          const [key, value] = param.split('=');
          if (key) queryParams[key] = value || '';
        });
        this.router.navigate([path], { queryParams });
      } else {
        this.router.navigate([path]);
      }
    }
  }

  markAllAsRead(): void {
    this.notificationService.markAllAsRead();
  }

  clearAllNotifications(): void {
    this.notificationService.clearAll();
  }

  // ---- Helpers -------------------------------------------------------
  getTimeSince(dateStr: string): string {
    if (!dateStr) return '';
    const diff = Date.now() - new Date(dateStr).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'Just now';
    if (mins < 60) return `${mins}m ago`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    return `${days}d ago`;
  }

  trackById(_index: number, item: AppNotification): string {
    return item.id;
  }

  getNotificationIcon(type: string): string {
    switch (type) {
      case 'panic':
        return '🚨';
      case 'ride':
      case 'ride_finished':
        return '🚗';
      case 'ride_invite':
        return '✉️';
      case 'driver_assignment':
        return '📍';
      case 'support':
        return '💬';
      default:
        return '🔔';
    }
  }

  // ---- Logout error modal -------------------------------------------
  closeLogoutError(): void {
    this.showLogoutError = false;
    this.logoutErrorMessage = '';
  }

  goToDashboard(): void {
    this.closeLogoutError();
    this.router.navigate(['/driver/dashboard']);
  }

  logout(): void {
    this.isDropdownOpen = false;
    this.authService.logout().pipe(take(1)).subscribe((result) => {
      if (!result.success && result.error) {
        this.logoutErrorMessage = result.error;
        this.showLogoutError = true;
        this.cdr.detectChanges();
      }
    });
  }
}
