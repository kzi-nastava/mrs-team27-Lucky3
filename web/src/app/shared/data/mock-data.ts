export interface Ride {
  id: string;
  driverId: string;
  startedAt?: string;
  requestedAt: string;
  completedAt?: string;
  status: 'completed' | 'cancelled' | 'active';
  fare: number;
  distance: number;
  pickup: { address: string };
  destination: { address: string };
  hasPanic?: boolean;
  passengerName?: string;
  cancelledBy?: 'passenger' | 'driver';
  cancellationReason?: string;
}

export const mockRides: Ride[] = [
  {
    id: '1',
    driverId: 'd1',
    startedAt: '2025-12-21T16:00:00',
    requestedAt: '2025-12-21T15:55:00',
    completedAt: '2025-12-21T16:25:00',
    status: 'completed',
    fare: 25.50,
    distance: 6.3,
    pickup: { address: '111 Pine St, San Francisco, CA 94111' },
    destination: { address: '222 Oak St, San Francisco, CA 94112' },
    hasPanic: false,
    passengerName: 'Sarah Johnson'
  },
  {
    id: '2',
    driverId: 'd1',
    startedAt: '2025-12-21T11:35:00',
    requestedAt: '2025-12-21T11:30:00',
    completedAt: '2025-12-21T11:50:00',
    status: 'completed',
    fare: 18.75,
    distance: 5.2,
    pickup: { address: '123 Market St, San Francisco, CA 94102' },
    destination: { address: '456 Mission St, San Francisco, CA 94105' },
    hasPanic: true,
    passengerName: 'Michael Chen'
  },
  {
    id: '3',
    driverId: 'd1',
    startedAt: '2025-12-20T12:15:00',
    requestedAt: '2025-12-20T12:10:00',
    completedAt: '2025-12-20T12:45:00',
    status: 'completed',
    fare: 32.00,
    distance: 5.8,
    pickup: { address: '567 California St, San Francisco, CA 94104' },
    destination: { address: '890 Stockton St, San Francisco, CA 94108' },
    hasPanic: false,
    passengerName: 'Emma Wilson'
  },
  {
    id: '4',
    driverId: 'd1',
    startedAt: '2025-12-18T17:45:00',
    requestedAt: '2025-12-18T17:40:00',
    completedAt: undefined,
    status: 'cancelled',
    fare: 0,
    distance: 0,
    pickup: { address: '1234 Valencia St, San Francisco, CA 94110' },
    destination: { address: '5678 Divisadero St, San Francisco, CA 94115' },
    hasPanic: false,
    passengerName: 'John Doe',
    cancelledBy: 'driver',
    cancellationReason: 'Vehicle malfunction'
  },
  {
    id: '5',
    driverId: 'd1',
    startedAt: '2025-12-15T09:00:00',
    requestedAt: '2025-12-15T08:55:00',
    completedAt: '2025-12-15T09:30:00',
    status: 'completed',
    fare: 45.00,
    distance: 12.5,
    pickup: { address: '999 Baker St, San Francisco, CA 94115' },
    destination: { address: '101 1st St, San Francisco, CA 94105' },
    hasPanic: false,
    passengerName: 'Lisa Anderson'
  }
];
