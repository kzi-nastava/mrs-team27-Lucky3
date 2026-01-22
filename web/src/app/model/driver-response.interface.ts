import { VehicleInformation } from './create-driver-request.interface';

export interface DriverResponse {
  id: number;
  name: string;
  surname: string;
  email: string;
  profilePictureUrl: string;
  role: string;
  phoneNumber: string;
  address: string;
  vehicle: VehicleInformation;
  active: boolean;
  blocked: boolean;
  active24h: string;
}