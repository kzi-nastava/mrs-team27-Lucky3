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

  items: SidebarItem[] = [
    { icon: 'dashboard', label: 'Dashboard', route: '/driver/dashboard', active: false },
    { icon: 'earnings', label: 'Overview', route: '/driver/overview', active: false },
    { icon: 'profile', label: 'Profile', route: '/driver/profile', active: false },
    { icon: 'support', label: 'Support', route: '/driver/support', active: false },
    { icon: 'logout', label: 'Logout', route: '/logout', variant: 'danger' }
  ];

  constructor(private router: Router) {}

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
    this.items.forEach(item => {
      if (item.route !== '/logout') {
        item.active = currentUrl.includes(item.route);
      }
    });
  }

  close() {
    this.closeSidebar.emit();
  }

  onItemClick() {
    // On mobile, close sidebar when item is clicked
    if (window.innerWidth < 1024) {
      this.close();
    }
  }
}
