import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationEnd, Router, RouterModule } from '@angular/router';
import { filter } from 'rxjs/operators';

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
    { icon: 'history', label: 'Ride History', route: '/passenger/history', active: false },
    { icon: 'profile', label: 'Profile', route: '/passenger/profile', active: false },
    { icon: 'support', label: 'Support', route: '/passenger/support', active: false },
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

  constructor(private router: Router) {
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

  onItemClick(clickedItem: SidebarItem) {
    // Set all items to inactive, then activate the clicked one
    this.items.forEach(item => item.active = false);
    clickedItem.active = true;

    // On mobile, close sidebar when item is clicked
    if (window.innerWidth < 1024) {
      this.close();
    }
  }
}