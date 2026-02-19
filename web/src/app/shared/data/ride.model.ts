export interface RidePassenger {
  id?: number;
  name: string;
  email?: string;
  phone?: string;
}

export interface Ride {
  id: string;
  driverId: string;
  startedAt?: string;
  requestedAt: string;
  completedAt?: string;
  status: 'all' | 'Pending' | 'Accepted' | 'Finished' | 'Rejected' | 'Cancelled';
  fare: number;
  distance: number;
  pickup: { address: string; latitude?: number; longitude?: number };
  destination: { address: string; latitude?: number; longitude?: number };
  stops?: { address: string; latitude?: number; longitude?: number }[];
  hasPanic?: boolean;
  passengerName?: string;
  passengerCount?: number;
  passengers?: RidePassenger[];
  cancelledBy?: 'passenger' | 'driver';
  cancellationReason?: string;
  vehicleType?: string;
}
