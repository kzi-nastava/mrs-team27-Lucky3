import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';

export const resetPasswordTokenGuard: CanActivateFn = (route) => {
  const router = inject(Router);
  const authService = inject(AuthService);

  const tokenFromParam = route.paramMap.get('token');
  const tokenFromQuery = route.queryParamMap.get('token');
  const token = (tokenFromParam || tokenFromQuery || '').trim();

  if (!token) {
    return router.createUrlTree(['/forgot-password'], {
      queryParams: { reason: 'missing-token' }
    });
  }

  return authService.validatePasswordResetToken(token).pipe(
    map((result) => {
      if (result.valid) return true;

      const reason = result.reason === 'expired' ? 'expired-token' : 'invalid-token';
      return router.createUrlTree(['/forgot-password'], {
        queryParams: { reason }
      });
    }),
    catchError(() =>
      of(
        router.createUrlTree(['/forgot-password'], {
          queryParams: { reason: 'invalid-token' }
        })
      )
    )
  );
};
