import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { RideService } from '../rest/ride.service';
import { AuthService } from './auth.service';
import { map, catchError, of } from 'rxjs';

/**
 * Guard that ensures only the driver who completed the ride can view its history.
 * This is for viewing historical/completed rides in the driver overview section.
 */
export const driverRideHistoryGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const rideService = inject(RideService);
  const authService = inject(AuthService);

  const rideId = Number(route.paramMap.get('id'));
  const userId = authService.getUserId();
  const role = authService.getRole();

  // Must be logged in as a driver
  if (!userId || role !== 'DRIVER') {
    if (!userId) {
      router.navigate(['/login']);
      return false;
    }
    // If logged in but not driver - redirect to 404
    router.navigate(['/404']);
    return false;
  }

  // If ID is malformed
  if (!rideId || isNaN(rideId)) {
    router.navigate(['/driver/overview']);
    return false;
  }

  // Fetch ride to check ownership
  return rideService.getRide(rideId).pipe(
    map(ride => {
      // Check if driver matches
      const rideDriverId = ride.driver?.id ?? ride.driverId;
      if (rideDriverId !== userId) {
        // Not the assigned driver - redirect to 404
        router.navigate(['/404']);
        return false;
      }

      return true;
    }),
    catchError((err) => {
      // If ride not found or error - redirect to 404
      router.navigate(['/404']);
      return of(false);
    })
  );
};
