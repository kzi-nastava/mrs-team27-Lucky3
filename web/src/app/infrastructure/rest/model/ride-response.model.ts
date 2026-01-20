import { LocationDto } from './location.model';

export type RideStatus =
  | 'PENDING'
  | 'ACCEPTED'
  | 'IN_PROGRESS'
  | 'FINISHED'
  | 'CANCELLED'
  | string;

export interface RoutePointResponse {
  location: LocationDto;
  order: number;
}

// This is a best-effort shape based on the backend controller.
// Fields are optional to stay compatible with DummyData variations.
export interface RideResponse {
  id: number;
  status?: RideStatus;

  driverId?: number;
  passengerId?: number;

  start?: LocationDto;
  destination?: LocationDto;
  stops?: LocationDto[];

  // Some backends expose these fields with different names.
  startLocation?: LocationDto;
  endLocation?: LocationDto;

  routePoints?: RoutePointResponse[];

  // Pricing / live state
  estimatedCost?: number;
  totalCost?: number;
  distanceKm?: number;
  estimatedDistance?: number;
  estimatedTimeInMinutes?: number;

  vehicleType?: string;

  // End ride flags
  passengersExited?: boolean;
  paid?: boolean;
}

export interface EndRideRequest {
  passengersExited: boolean;
  paid: boolean;
}

export interface RideCancellationRequest {
  reason: string;
}
