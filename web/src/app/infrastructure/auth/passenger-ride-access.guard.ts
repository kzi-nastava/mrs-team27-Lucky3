import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { RideService } from '../rest/ride.service';
import { AuthService } from './auth.service';
import { map, catchError, of } from 'rxjs';

export const passengerRideAccessGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const rideService = inject(RideService);
  const authService = inject(AuthService);

  const rideId = Number(route.paramMap.get('id'));
  const userId = authService.getUserId();
  const role = authService.getRole();

  if (!userId) {
    router.navigate(['/login']);
    return false;
  }

  if (role !== 'PASSENGER') {
    // Wrong role - redirect to 404
    router.navigate(['/404']);
    return false;
  }

  // If ID is malformed
  if (!rideId || isNaN(rideId)) {
    router.navigate(['/passenger/home']);
    return false;
  }

  // Fetch ride to check ownership and status
  return rideService.getRide(rideId).pipe(
    map(ride => {
      // Check if user is a passenger in this ride
      const isPassenger = ride.passengers?.some(p => p.id === userId);
      
      if (!isPassenger) {
        router.navigate(['/passenger/home']);
        return false;
      }

      // Check ride status - only allow PENDING, ACCEPTED, SCHEDULED, or IN_PROGRESS
      const allowedStatuses = ['PENDING', 'ACCEPTED', 'SCHEDULED', 'IN_PROGRESS', 'ACTIVE'];
      const status = ride.status?.toUpperCase() || '';
      
      if (!allowedStatuses.includes(status)) {
        // Ride is finished/cancelled - redirect to home
        router.navigate(['/passenger/home']);
        return false;
      }

      return true;
    }),
    catchError((err) => {
      // If ride not found or error
      router.navigate(['/passenger/home']);
      return of(false);
    })
  );
};
