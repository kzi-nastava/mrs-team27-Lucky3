export interface FutureRideStop {
  address: string;
}

export interface FutureRide {
  id: string;
  type: 'ECONOMY' | 'STANDARD' | 'PREMIUM' | string;
  price: number;
  distance: string;
  time: string;
  pickup: string;
  dropoff: string;
  stops?: FutureRideStop[];
}

// Mock data used by the driver dashboard + active ride page.
export const mockFutureRides: FutureRide[] = [
  {
    id: '101',
    type: 'ECONOMY',
    price: 15.0,
    distance: '4.1km',
    time: '12min',
    pickup: '333 Bulevar Oslobodjenja, Novi Sad',
    dropoff: '444 Bulevar Mihajla Pupina, Novi Sad',
    stops: [{ address: 'Trg Slobode, Novi Sad' }]
  },
  {
    id: '102',
    type: 'STANDARD',
    price: 22.5,
    distance: '6.8km',
    time: '18min',
    pickup: 'Zeleznicka stanica, Novi Sad',
    dropoff: 'Promenada, Novi Sad',
    stops: []
  },
  {
    id: '103',
    type: 'PREMIUM',
    price: 34.0,
    distance: '10.2km',
    time: '26min',
    pickup: 'Spens, Novi Sad',
    dropoff: 'Petrovaradinska tvrdjava, Novi Sad',
    stops: [{ address: 'Dunavski park, Novi Sad' }, { address: 'Srpsko narodno pozoriste, Novi Sad' }]
  },
  {
    id: '104',
    type: 'ECONOMY',
    price: 12.0,
    distance: '3.5km',
    time: '10min',
    pickup: 'Futoska pijaca, Novi Sad',
    dropoff: 'Limanski park, Novi Sad',
    stops: []
  }
];
