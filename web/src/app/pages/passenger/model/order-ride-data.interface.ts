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
}

export interface RideEstimation {
  estimatedDistance: number;
  estimatedTimeInMinutes: number;
  estimatedCost: number;
}