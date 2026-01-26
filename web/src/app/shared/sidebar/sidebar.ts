import { ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationEnd, Router, RouterModule } from '@angular/router';
import { filter, take } from 'rxjs/operators';
import { AuthService } from '../../infrastructure/auth/auth.service';

interface SidebarItem {
  icon: string;
  label: string;
  route: string;
  active?: boolean;
  variant?: string;
  badge?: string;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css'
})
export class Sidebar implements OnInit {
  @Input() isOpen = false;
  @Output() closeSidebar = new EventEmitter<void>();

  // Logout error modal
  showLogoutError = false;
  logoutErrorMessage = '';

  items: SidebarItem[] = [];

  driverItems: SidebarItem[] = [
    { icon: 'dashboard', label: 'Dashboard', route: '/driver/dashboard', active: false },
    { icon: 'earnings', label: 'Overview', route: '/driver/overview', active: false },
    { icon: 'profile', label: 'Profile', route: '/driver/profile', active: false },
    { icon: 'support', label: 'Support', route: '/driver/support', active: false },
    { icon: 'logout', label: 'Logout', route: '/login', variant: 'danger' }
  ];

  passengerItems: SidebarItem[] = [
    { icon: 'home', label: 'Home', route: '/passenger/home', active: false },
    { icon: 'history', label: 'Ride History', route: '/passenger/ride-history', active: false },
    { icon: 'profile', label: 'Profile', route: '/passenger/profile', active: false },
    { icon: 'support', label: 'Support', route: '/passenger/support', active: false },
    { icon: 'favorite', label: 'Favorites', route: '/passenger/favorites', active: false},
    { icon: 'logout', label: 'Logout', route: '/login', variant: 'danger' }
  ];

  adminItems: SidebarItem[] = [
    { icon: 'dashboard', label: 'Dashboard', route: '/admin/dashboard', active: false },
    { icon: 'reports', label: 'Reports', route: '/admin/reports', active: false },
    { icon: 'drivers', label: 'Drivers', route: '/admin/drivers', active: false },
    { icon: 'pricing', label: 'Pricing', route: '/admin/pricing', active: false },
    { icon: 'profile', label: 'Profile', route: '/admin/profile', active: false },
    { icon: 'support', label: 'Support', route: '/admin/support', active: false },
    { icon: 'logout', label: 'Logout', route: '/login', variant: 'danger' }
  ];

  constructor(
    private router: Router,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {
    this.items = this.driverItems;
  }

  ngOnInit() {
    this.checkActiveRoute();
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      this.checkActiveRoute();
    });
  }

  private checkActiveRoute() {
    const currentUrl = this.router.url;

    if (currentUrl.startsWith('/passenger')) {
      this.items = this.passengerItems;
    } else if (currentUrl.startsWith('/admin')) {
      this.items = this.adminItems;
    } else {
      this.items = this.driverItems;
    }

    this.items.forEach(item => {
      if (item.route !== '/logout') {
        item.active = currentUrl.includes(item.route);
      }
    });
  }

  close() {
    this.closeSidebar.emit();
  }

  closeLogoutError() {
    this.showLogoutError = false;
    this.logoutErrorMessage = '';
  }

  goToDashboard() {
    this.closeLogoutError();
    this.router.navigate(['/driver/dashboard']);
  }

  onItemClick(item: SidebarItem) {
    // Handle logout separately to clear localStorage
    if (item.icon === 'logout') {
      this.authService.logout().pipe(take(1)).subscribe(result => {
        if (!result.success && result.error) {
          // Driver has an active ride or is online - show modal
          this.logoutErrorMessage = result.error;
          this.showLogoutError = true;
          // Manually trigger change detection to show modal immediately
          this.cdr.detectChanges();
        }
        // If successful, the logout method already handles navigation
      });
      return;
    }
    // On mobile, close sidebar when item is clicked
    if (window.innerWidth < 1024) {
      this.close();
    }
  }
}