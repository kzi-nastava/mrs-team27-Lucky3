import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

export const activationGuard: CanActivateFn = (route) => {
  const router = inject(Router);
  
  const token = route.paramMap.get('token') || route.queryParamMap.get('token');

  if (token && token.trim().length > 0) {
    return true;
  }

  // If no token is provided (e.g. user went to /activate directly), redirect to 404
  return router.createUrlTree(['/404']);
};