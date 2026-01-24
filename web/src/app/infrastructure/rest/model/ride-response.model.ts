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

  // reference_backend shape
  driver?: { id?: number } | null;
  passengers?: Array<{ id?: number; name?: string; email?: string }> | null;
  departure?: LocationDto;
  destination?: LocationDto;
  scheduledTime?: string;

  // Legacy / alternate shapes used by earlier mocks
  driverId?: number;
  passengerId?: number;
  start?: LocationDto;
  stops?: LocationDto[];
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

  // Timestamps
  startTime?: string;
  endTime?: string;

  // End ride flags
  passengersExited?: boolean;
  paid?: boolean;
  
  // Additional status info
  panicPressed?: boolean;
  rejectionReason?: string;
}

export interface EndRideRequest {
  passengersExited: boolean;
  paid: boolean;
}

export interface RideCancellationRequest {
  reason: string;
}
