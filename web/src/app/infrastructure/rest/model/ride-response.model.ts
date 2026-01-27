import { LocationDto } from './location.model';

export type RideStatus =
  | 'PENDING'
  | 'ACCEPTED'
  | 'IN_PROGRESS'
  | 'FINISHED'
  | 'CANCELLED'
  | 'CANCELLED_BY_DRIVER'
  | 'CANCELLED_BY_PASSENGER'
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

  // reference_backend shape - full driver info from DriverResponse
  driver?: {
    id?: number;
    name?: string;
    surname?: string;
    email?: string;
    profilePicture?: string;
    phoneNumber?: string;
    address?: string;
    vehicle?: {
      model?: string;
      vehicleType?: string;
      licensePlates?: string;
      seatCount?: number;
      babyTransport?: boolean;
      petTransport?: boolean;
    };
  } | null;
  passengers?: Array<{ id?: number; name?: string; surname?: string; email?: string }> | null;
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
  model?: string;
  licensePlates?: string;
  vehicleLocation?: LocationDto;
  babyTransport?: boolean;
  petTransport?: boolean;

  // Timestamps
  startTime?: string;
  endTime?: string;

  // End ride flags
  passengersExited?: boolean;
  paid?: boolean;
  
  // Stop tracking
  completedStopIndexes?: number[];
  
  // Additional status info
  panicPressed?: boolean;
  rejectionReason?: string;
  
  // Distance tracking (updated by backend)
  distanceTraveled?: number;
}

export interface EndRideRequest {
  passengersExited: boolean;
  paid: boolean;
}

export interface RideCancellationRequest {
  reason: string;
}
