import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Guard that prevents authenticated users from accessing guest-only pages
 * (login, register, forgot-password, home, etc.)
 * 
 * Redirects to the appropriate dashboard based on user role:
 * - DRIVER -> /driver/dashboard
 * - ADMIN -> /admin/dashboard
 * - PASSENGER -> /passenger/home
 */
export const noAuthGuard: CanActivateFn = (route, state): boolean | UrlTree => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isLoggedIn()) {
    // Not logged in, allow access to guest pages
    return true;
  }

  // User is logged in - redirect to their dashboard based on role
  const role = authService.getRole();
  
  switch (role) {
    case 'DRIVER':
      return router.createUrlTree(['/driver/dashboard']);
    case 'ADMIN':
      return router.createUrlTree(['/admin/dashboard']);
    case 'PASSENGER':
    default:
      return router.createUrlTree(['/passenger/home']);
  }
};
