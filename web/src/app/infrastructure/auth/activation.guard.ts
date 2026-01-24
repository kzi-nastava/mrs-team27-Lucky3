import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

export const activationGuard: CanActivateFn = (route) => {
  const router = inject(Router);
  
  // Check for token in path parameters
  const token = route.paramMap.get('token');

  if (token && token.trim().length > 0) {
    return true;
  }

  // If no token is provided (e.g. user went to /activate directly), redirect to login
  return router.createUrlTree(['/login']);
};