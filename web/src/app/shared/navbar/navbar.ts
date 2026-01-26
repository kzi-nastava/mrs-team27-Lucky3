import { ChangeDetectorRef, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { take } from 'rxjs/operators';
import { AuthService } from '../../infrastructure/auth/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css'
})
export class Navbar {
  @Input() title: string = 'Dashboard';
  @Output() toggleSidebar = new EventEmitter<void>();
  isDropdownOpen = false;

  // Logout error modal
  showLogoutError = false;
  logoutErrorMessage = '';

  constructor(
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  toggle() {
    this.toggleSidebar.emit();
  }

  toggleDropdown() {
    this.isDropdownOpen = !this.isDropdownOpen;
  }

  closeLogoutError() {
    this.showLogoutError = false;
    this.logoutErrorMessage = '';
  }

  goToDashboard() {
    this.closeLogoutError();
    this.router.navigate(['/driver/dashboard']);
  }

  logout() {
    this.isDropdownOpen = false;
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
  }
}
