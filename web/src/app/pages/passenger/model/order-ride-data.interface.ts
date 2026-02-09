export interface RideEstimation {
  estimatedDistance: number;
  estimatedTimeInMinutes: number;
  estimatedCost: number;
}

export interface RideOrderData {
  pickupAddress: string;
  destinationAddress: string;
  intermediateStops: string[];
  vehicleType: string;
  petTransport: boolean;
  babyTransport: boolean;
  scheduledTime: string;
}

export interface RideEstimation {
  estimatedDistance: number;
  estimatedTimeInMinutes: number;
  estimatedCost: number;
}