import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { AuthService } from './auth.service';

export const reviewGuard: CanActivateFn = (route, state): boolean | UrlTree => {
  const authService = inject(AuthService);
  const router = inject(Router);
  
  // Check if token is present in query params first
  const token = route.queryParamMap.get('token');
  if (!token) {
    // No token - redirect to home
    return router.createUrlTree(['/home']);
  }

  // If user is logged in as a driver, redirect to dashboard
  // Drivers cannot access review page
  if (authService.isLoggedIn()) {
    const userRole = authService.getRole();
    if (userRole === 'DRIVER') {
      return router.createUrlTree(['/driver/dashboard']);
    }
  }

  // Token present and not a driver, allow access (token validation happens in the component)
  return true;
};
