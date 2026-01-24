import { LocationDto } from './location.model';

export interface RideRequirements {
  vehicleType: string;
  babyTransport: boolean;
  petTransport: boolean;
}

export interface CreateRideRequest {
  start: LocationDto;
  destination: LocationDto;
  stops: LocationDto[];
  passengerEmails: string[];
  scheduledTime: string | null;
  requirements: RideRequirements;
}

