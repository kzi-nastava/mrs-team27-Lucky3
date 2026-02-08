import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { RideTrackingService } from '../rest/ride-tracking.service';
import { catchError, map, of } from 'rxjs';
import { Observable } from 'rxjs';

/**
 * Guard for the ride tracking page.
 * Validates the tracking token from query params.
 * If token is invalid or ride is not trackable, redirects to 404.
 */
export const rideTrackingGuard: CanActivateFn = (route, state): Observable<boolean | UrlTree> => {
  const router = inject(Router);
  const rideTrackingService = inject(RideTrackingService);

  const token = route.queryParamMap.get('token');
  
  if (!token) {
    return of(router.createUrlTree(['/404']));
  }

  return rideTrackingService.validateToken(token).pipe(
    map(response => {
      if (response.valid) {
        return true;
      } else {
        // Token is not valid - redirect to 404 with reason
        console.warn('Tracking token invalid:', response.reason);
        return router.createUrlTree(['/404']);
      }
    }),
    catchError(error => {
      console.error('Error validating tracking token:', error);
      return of(router.createUrlTree(['/404']));
    })
  );
};
