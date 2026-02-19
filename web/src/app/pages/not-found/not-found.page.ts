import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../infrastructure/auth/auth.service';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="fixed inset-0 z-50 bg-black flex items-center justify-center p-4">
      <div class="w-full max-w-lg bg-gray-900 rounded-2xl p-10 border border-gray-800 shadow-2xl text-center">
        <!-- 404 Icon -->
        <div class="w-28 h-28 bg-red-500/20 rounded-2xl flex items-center justify-center mx-auto mb-8 border border-red-500/30">
          <svg xmlns="http://www.w3.org/2000/svg" width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-red-500">
            <circle cx="12" cy="12" r="10"/>
            <path d="m15 9-6 6"/>
            <path d="m9 9 6 6"/>
          </svg>
        </div>

        <!-- Error Message -->
        <h1 class="text-7xl font-bold text-white mb-4">404</h1>
        <h2 class="text-2xl font-semibold text-white mb-4">Page Not Found</h2>
        <p class="text-gray-400 mb-10 text-lg">
          Oops! The page you're looking for doesn't exist or you don't have permission to access it.
        </p>

        <!-- Action Buttons -->
        <div class="flex flex-col gap-4 max-w-xs mx-auto">
          <a 
            *ngIf="!isLoggedIn" 
            routerLink="/" 
            class="w-full bg-yellow-500 hover:bg-yellow-400 text-black font-bold py-4 px-6 rounded-xl transition-all duration-200 inline-flex items-center justify-center gap-3 shadow-lg hover:shadow-yellow-500/25 hover:scale-[1.02]"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
              <polyline points="9 22 9 12 15 12 15 22"/>
            </svg>
            Go to Home
          </a>

          <a 
            *ngIf="isLoggedIn" 
            [routerLink]="dashboardRoute" 
            class="w-full bg-yellow-500 hover:bg-yellow-400 text-black font-bold py-4 px-6 rounded-xl transition-all duration-200 inline-flex items-center justify-center gap-3 shadow-lg hover:shadow-yellow-500/25 hover:scale-[1.02]"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <rect width="7" height="9" x="3" y="3" rx="1"/>
              <rect width="7" height="5" x="14" y="3" rx="1"/>
              <rect width="7" height="9" x="14" y="12" rx="1"/>
              <rect width="7" height="5" x="3" y="16" rx="1"/>
            </svg>
            Go to Dashboard
          </a>

          <button 
            (click)="goBack()" 
            class="w-full py-4 px-6 bg-gray-800 hover:bg-gray-700 text-white font-semibold rounded-xl transition-all duration-200 inline-flex items-center justify-center gap-3 border border-gray-700 hover:border-gray-600 hover:scale-[1.02]"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="m12 19-7-7 7-7"/>
              <path d="M19 12H5"/>
            </svg>
            Go Back
          </button>
        </div>

        <!-- Decorative Element -->
        <div class="mt-12 pt-8 border-t border-gray-800">
          <div class="flex items-center justify-center gap-3 text-gray-500">
            <span class="text-3xl">🚗</span>
            <span class="text-base font-medium">Lucky3 Ride Service</span>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class NotFoundPage {
  isLoggedIn = false;
  dashboardRoute = '/';

  constructor(private authService: AuthService) {
    this.isLoggedIn = this.authService.isLoggedIn();
    
    if (this.isLoggedIn) {
      const role = this.authService.getRole();
      switch (role) {
        case 'DRIVER':
          this.dashboardRoute = '/driver/dashboard';
          break;
        case 'ADMIN':
          this.dashboardRoute = '/admin/dashboard';
          break;
        case 'PASSENGER':
        default:
          this.dashboardRoute = '/passenger/home';
          break;
      }
    }
  }

  goBack(): void {
    window.history.back();
  }
}
