export interface CreateDriverRequest {
  name: string;
  surname: string;
  email: string;
  address: string;
  phone: string;
  vehicle: VehicleInformation;
}

export interface VehicleInformation {
  model: string;
  licenseNumber: string;
  year: number;
  vehicleType: 'LUXURY' | 'VAN' | 'STANDARD';
  passengerSeats: number;
  babyTransport: boolean;
  petTransport: boolean;
}