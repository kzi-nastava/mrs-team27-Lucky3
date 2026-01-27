import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { RideService } from '../rest/ride.service';
import { AuthService } from './auth.service';
import { map, catchError, of } from 'rxjs';

export const rideAccessGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const rideService = inject(RideService);
  const authService = inject(AuthService);

  const rideId = Number(route.paramMap.get('id'));
  const userId = authService.getUserId();
  const role = authService.getRole();

  if (!userId || role !== 'DRIVER') {
    // If not a driver or not logged in, redirect to login or home
    // But per requirements, "If the user is not the assigned driver, redirect them to /driver/dashboard."
    // If not logged in at all, probably login.
    if (!userId) {
        router.navigate(['/login']);
        return false;
    }
    // If logged in but not driver?
    router.navigate(['/driver/dashboard']);
    return false;
  }

  // If ID is malformed
  if (!rideId || isNaN(rideId)) {
    router.navigate(['/driver/dashboard']);
    return false;
  }

  // Fetch ride to check ownership and status
  return rideService.getRide(rideId).pipe(
    map(ride => {
      // Check if driver matches
      const rideDriverId = ride.driver?.id ?? ride.driverId;
      if (rideDriverId !== userId) {
        // Not the assigned driver
        router.navigate(['/driver/dashboard']);
        return false;
      }

      // Check ride status - only allow PENDING, ACCEPTED, SCHEDULED, or IN_PROGRESS
      const allowedStatuses = ['PENDING', 'ACCEPTED', 'SCHEDULED', 'IN_PROGRESS', 'ACTIVE'];
      const status = ride.status?.toUpperCase() || '';
      
      if (!allowedStatuses.includes(status)) {
        // Ride is finished/cancelled - redirect to dashboard
        router.navigate(['/driver/dashboard']);
        return false;
      }

      return true;
    }),
    catchError((err) => {
      // If ride not found or error
      router.navigate(['/driver/dashboard']);
      return of(false);
    })
  );
};
