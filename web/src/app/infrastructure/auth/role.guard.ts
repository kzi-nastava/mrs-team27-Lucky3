import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { AuthService } from './auth.service';

export const roleGuard: CanActivateFn = (route, state): boolean | UrlTree => {
  const authService = inject(AuthService);
  const router = inject(Router);
  
  const userRole = authService.getRole();
  const expectedRoles = route.data['roles'] as Array<string>;

  // Check if user has one of the allowed roles
  if (userRole && expectedRoles.includes(userRole)) {
    return true;
  }

  // If wrong role, redirect to 404 page
  return router.createUrlTree(['/404']);
};