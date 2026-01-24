import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

export const resetPasswordTokenGuard: CanActivateFn = (route) => {
  const router = inject(Router);

  const tokenFromParam = route.paramMap.get('token');
  const tokenFromQuery = route.queryParamMap.get('token');
  const token = (tokenFromParam || tokenFromQuery || '').trim();

  if (!token) {
    return router.createUrlTree(['/forgot-password'], {
      queryParams: { reason: 'missing-token' }
    });
  }

  // Let the component validate the token with the backend. This allows the reset-password
  // page to render immediately (with a loader) instead of keeping the previous page visible
  // while we wait for the validation request.
  return true;
};
