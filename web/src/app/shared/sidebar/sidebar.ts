import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';

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
export class Sidebar {
  @Input() isOpen = false;
  @Output() closeSidebar = new EventEmitter<void>();

  items: SidebarItem[] = [
    { icon: 'dashboard', label: 'Dashboard', route: '/driver/dashboard', active: false },
    { icon: 'earnings', label: 'Overview', route: '/driver/overview', active: true },
    { icon: 'profile', label: 'Profile', route: '/driver/profile', active: false },
    { icon: 'support', label: 'Support', route: '/driver/support', active: false },
    { icon: 'logout', label: 'Logout', route: '/logout', variant: 'danger' }
  ];

  constructor(private router: Router) {}

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
