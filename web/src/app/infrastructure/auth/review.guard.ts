import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { AuthService } from './auth.service';

export const reviewGuard: CanActivateFn = (route, state): boolean | UrlTree => {
  const authService = inject(AuthService);
  const router = inject(Router);
  
  // Check if token or rideId is present in query params
  const token = route.queryParamMap.get('token');
  const rideId = route.queryParamMap.get('rideId');

  // For authenticated mode (rideId), user must be logged in as a passenger
  if (rideId) {
    if (authService.isLoggedIn() && authService.getRole() === 'PASSENGER') {
      return true;
    }
    return router.createUrlTree(['/404']);
  }

  if (!token) {
    // No token and no rideId - redirect to 404
    return router.createUrlTree(['/404']);
  }

  // If user is logged in as a driver, redirect to 404
  // Drivers cannot access review page
  if (authService.isLoggedIn()) {
    const userRole = authService.getRole();
    if (userRole === 'DRIVER') {
      return router.createUrlTree(['/404']);
    }
  }

  // Token present and not a driver, allow access (token validation happens in the component)
  return true;
};
