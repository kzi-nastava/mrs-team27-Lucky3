import { LocationDto } from './location.model';
import { RideRequirements } from './create-ride.model';
import { RoutePointResponse } from '../model/ride-response.model';

export enum RideStatus {
  PENDING = 'PENDING',
  ACCEPTED = 'ACCEPTED',
  SCHEDULED = 'SCHEDULED',
  ACTIVE = 'ACTIVE',
  IN_PROGRESS = 'IN_PROGRESS',
  FINISHED = 'FINISHED',
  REJECTED = 'REJECTED',
  PANIC = 'PANIC',
  CANCELLED = 'CANCELLED'
}

export enum VehicleType {
  STANDARD = 'STANDARD',
  VAN = 'VAN',
  LUXURY = 'LUXURY'
}

export interface RideCreated {
  id: number;
  status: RideStatus;
  // Driver details
  driverId: number;
  driverName: string;
  driverSurname: string;
  driverEmail: string;
  driverProfilePictureUrl: string;

  // Vehicle details
  vehicleModel: string;
  vehicleLicensePlate: string;
  babyTransport: boolean;
  petTransport: boolean;
  requestedVehicleType: VehicleType;
  
  // Route details
  departure: LocationDto;
  destination: LocationDto;
  stops: LocationDto[];
  routePoints?: RoutePointResponse[];
  
  passengersEmails: string[];
  
  // Timing and cost details
  scheduledTime: string | null;  // ISO 8601 string format
  distanceKm: number;
  estimatedTimeInMinutes: number;
  estimatedCost: number;
  
  rejectionReason: string | null;
}