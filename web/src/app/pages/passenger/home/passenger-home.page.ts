import { Component, OnInit, OnDestroy, AfterViewInit, ChangeDetectorRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import * as L from 'leaflet';
import { environment } from '../../../../env/environment';

import { RideService, RideEstimationResponse, RoutePoint } from '../../../infrastructure/rest/ride.service';
import { CreateRideRequest } from '../../../infrastructure/rest/model/create-ride.model';
import { AuthService } from '../../../infrastructure/auth/auth.service';
import { VehicleService } from '../../../infrastructure/rest/vehicle.service';
import { VehicleLocationResponse } from '../../../infrastructure/rest/model/vehicle-location.model';
import { LocationDto } from '../../../infrastructure/rest/model/location.model';
import { HttpClient } from '@angular/common/http';
import { RideOrderingFormComponent} from '../ride-ordering-form/ride-ordering-form.component';
import { RideOrderData } from '../model/order-ride-data.interface';
import { RideResponse } from '../../../infrastructure/rest/model/ride-response.model';
import { LinkPassengerFormComponent } from '../link-passenger-form/link-passenger-form.component';
import {LinkedPassengersData} from '../model/link-passengers.interface';
import { RideInfoPopupComponent } from '../ride-info-popup/ride-info-popup.component';

@Component({
  selector: 'app-passenger-home',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, RideOrderingFormComponent, LinkPassengerFormComponent, RideInfoPopupComponent],
  templateUrl: './passenger-home.page.html',
  styles: []
})
export class PassengerHomePage implements OnInit, AfterViewInit, OnDestroy  {
  @ViewChild(RideOrderingFormComponent) rideOrderingForm!: RideOrderingFormComponent;
  private map!: L.Map;
  private vehicleMarkers: L.Marker[] = [];
  private routeLayer: L.Polyline | null = null;
  private pickupMarker: L.Marker | null = null;
  private destinationMarker: L.Marker | null = null;
  private refreshInterval: any;
  showEstimationForm: boolean = false;

  // Counts for the UI
  availableCount = 0;
  occupiedCount = 0;

  prefilledStartLocation: string | null = null;
  prefilledEndLocation: string | null = null;

  // ride Form state
  showOrderingForm = false;
  pickupAddress = '';
  destinationAddress = '';
  isOrdering = false;
  orderingResult!: RideResponse;
  showPopup = false;
  orderingError = '';
  intermediateStops: string[] = [];
  selectedVehicleType: string = 'standard';
  petTransport: boolean = false;
  babyTransport: boolean = false;

  private stopMarkers: L.Marker[] = []; // ADD THIS

  // email linking form state
  showLinkForm: boolean = false;
  linkedPassengers: string[] = [];
  
  // --- ICONS ---

  private availableVehicleIcon = L.divIcon({
    className: '',
    html: `<div style="display:flex;align-items:center;justify-content:center;width:40px;height:40px;border-radius:50%;background:rgba(34,197,94,0.2);border:2px solid #22c55e;color:#22c55e;box-shadow:0 0 10px rgba(34,197,94,0.5);">
      <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M18.92 6.01C18.72 5.42 18.16 5 17.5 5h-11c-.66 0-1.21.42-1.42 1.01L3 12v8c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1h12v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-8l-2.08-5.99zM6.5 16c-.83 0-1.5-.67-1.5-1.5S5.67 13 6.5 13s1.5.67 1.5 1.5S7.33 16 6.5 16zm11 0c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zM5 11l1.5-4.5h11L19 11H5z"/></svg>
    </div>`,
    iconSize: [40, 40],
    iconAnchor: [20, 20],
  });

  private occupiedVehicleIcon = L.divIcon({
    className: '',
    html: `<div style="display:flex;align-items:center;justify-content:center;width:40px;height:40px;border-radius:50%;background:rgba(239,68,68,0.2);border:2px solid #ef4444;color:#ef4444;box-shadow:0 0 10px rgba(239,68,68,0.5);">
      <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M18.92 6.01C18.72 5.42 18.16 5 17.5 5h-11c-.66 0-1.21.42-1.42 1.01L3 12v8c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1h12v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-8l-2.08-5.99zM6.5 16c-.83 0-1.5-.67-1.5-1.5S5.67 13 6.5 13s1.5.67 1.5 1.5S7.33 16 6.5 16zm11 0c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zM5 11l1.5-4.5h11L19 11H5z"/></svg>
    </div>`,
    iconSize: [40, 40],
    iconAnchor: [20, 20],
  });

  private pickupIcon = L.divIcon({
    className: '',
    html: `<div style="display:flex;align-items:center;justify-content:center;width:36px;height:36px;border-radius:50%;background:rgba(34,197,94,0.15);border:3px solid #22c55e;box-shadow:0 0 12px rgba(34,197,94,0.4);">
      <div style="width:12px;height:12px;border-radius:50%;background:#22c55e;"></div>
    </div>`,
    iconSize: [36, 36],
    iconAnchor: [18, 18],
  });

  private destinationIcon = L.divIcon({
    className: '',
    html: `<div style="display:flex;align-items:center;justify-content:center;width:32px;height:42px;position:relative;">
      <svg xmlns="http://www.w3.org/2000/svg" width="32" height="42" viewBox="0 0 24 32" fill="none">
        <path d="M12 0C5.373 0 0 5.373 0 12c0 9 12 20 12 20s12-11 12-20c0-6.627-5.373-12-12-12z" fill="#ef4444"/>
        <circle cx="12" cy="12" r="5" fill="#1a1a2e"/>
      </svg>
    </div>`,
    iconSize: [32, 42],
    iconAnchor: [16, 42],
  });

  private stopIcon = L.divIcon({
    className: '',
    html: `<div style="display:flex;align-items:center;justify-content:center;width:32px;height:32px;border-radius:50%;background:rgba(59,130,246,0.15);border:3px solid #3b82f6;box-shadow:0 0 10px rgba(59,130,246,0.4);">
      <div style="width:10px;height:10px;border-radius:50%;background:#3b82f6;"></div>
    </div>`,
    iconSize: [32, 32],
    iconAnchor: [16, 16],
  });
  

  constructor(
    private rideService: RideService,
    private vehicleService: VehicleService,
    private authService: AuthService,
    private http: HttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    // Access navigation state in constructor
    const navigation = this.router.getCurrentNavigation();
    if (navigation?.extras.state) {
      const state = navigation.extras.state;
      if (state['fromFavorites']) {
        // Pre-fill the form with favorite route data
        this.prefillOrderingForm(
          state['startLocation'].address,  // Extract address here
          state['endLocation'].address     // Extract address here
        );
      }
    }
  }

  private prefillOrderingForm(
    startLocation: string,
    endLocation: string
  ): void {
    // Show the ordering form
    this.showOrderingForm = true;
    
    // Set the prefilled values
    this.prefilledStartLocation = startLocation || '';
    this.prefilledEndLocation = endLocation || '';
    
  }

  ngOnInit(): void {}

  ngAfterViewInit(): void {
    this.initMap();
    this.fetchVehicles();
    
    this.refreshInterval = setInterval(() => {
        this.fetchVehicles();
    }, 10000);
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
    if (this.refreshInterval) {
        clearInterval(this.refreshInterval);
    }
  }

  private initMap(): void {
    const { defaultLat, defaultLng, defaultZoom } = environment.map;

    this.map = L.map('map', {
      center: [defaultLat, defaultLng],
      zoom: defaultZoom,
      zoomControl: false,
    });

    // Dark Map Style
    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
      attribution: 'OpenStreetMap contributors',
      subdomains: 'abcd',
      maxZoom: 20
    }).addTo(this.map);

    L.control.zoom({ position: 'bottomleft' }).addTo(this.map);
  }

  private fetchVehicles(): void {
    this.vehicleService.getActiveVehicles().subscribe({
      next: (vehicles) => {
        this.updateVehicleMarkers(vehicles);
        this.updateCounts(vehicles);
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Failed to fetch vehicles', err)
    });
  }

  private updateVehicleMarkers(vehicles: VehicleLocationResponse[]): void {
    this.vehicleMarkers.forEach(marker => marker.remove());
    this.vehicleMarkers = [];

    vehicles.forEach(v => {
      const icon = v.available ? this.availableVehicleIcon : this.occupiedVehicleIcon;
      const marker = L.marker([v.latitude, v.longitude], { icon })
        .addTo(this.map)
        .bindPopup(`
          <div style="min-width:100px;">
             <b>${v.vehicleType}</b><br>
             Status: ${v.available ? 'Available' : 'Occupied'}
          </div>
        `);
      this.vehicleMarkers.push(marker);
    });
  }

  private updateCounts(vehicles: VehicleLocationResponse[]): void {
      this.availableCount = vehicles.filter(v => v.available).length;
      this.occupiedCount = vehicles.filter(v => !v.available).length;
  }

  toggleOrderingForm(): void {
    this.showOrderingForm = !this.showOrderingForm;
    if (this.showOrderingForm) {
      this.showLinkForm = false;  // Close link form when opening ordering form
    }
    if (!this.showOrderingForm) {
      this.resetOrdering();
    }
    // Clear prefilled data when closing form
    
    //if (!this.showOrderingForm) {
      // Clear parent properties
      this.prefilledStartLocation = null;
      this.prefilledEndLocation = null;
      
      this.resetOrdering();
    //}

    this.cdr.detectChanges();
  }

  toggleLinkForm(): void {
    this.showLinkForm = !this.showLinkForm;
    if (this.showLinkForm) {
      this.showOrderingForm = false;  // Close ordering form when opening link form
    }
  }

  async orderRide(rideData: RideOrderData): Promise<void> {
    // Update parent state with form data
    this.pickupAddress = rideData.pickupAddress;
    this.destinationAddress = rideData.destinationAddress;

    if (!this.pickupAddress.trim() || !this.destinationAddress.trim()) {
      this.orderingError = 'Please enter both pickup and destination addresses';
      return;
    }

    this.isOrdering = true;
    this.orderingError = '';
    //this.orderingResult = null;
    this.cdr.detectChanges();

    // Check if passenger already has an active ride
    const userId = this.authService.getUserId();
    if (userId) {
      try {
        const activeRide = await this.rideService.getActiveRide(userId).toPromise();
        if (activeRide && activeRide.id) {
          this.orderingError = 'You already have an active ride. Please complete or cancel it before ordering a new one.';
          this.isOrdering = false;
          this.cdr.detectChanges();
          return;
        }
      } catch (err) {
        // No active ride found (404 or other error), continue with ordering
      }
    }

    try {
      // Geocode pickup and destination
      const startCoords = await this.geocodeAddress(this.pickupAddress);
      const destCoords = await this.geocodeAddress(this.destinationAddress);

      if (!startCoords || !destCoords) {
        this.orderingError = 'Could not find one of the locations.';
        this.isOrdering = false;
        this.cdr.detectChanges();
        return;
      }

      // Geocode intermediate stops
      const stops: Array<{address: string, latitude: number, longitude: number}> = [];
      const stopCoords: Array<{lat: number, lng: number}> = [];
      for (const stopAddress of rideData.intermediateStops) {
        const coords = await this.geocodeAddress(stopAddress);
        if (coords) {
          stops.push({
            address: stopAddress,
            latitude: coords.lat,
            longitude: coords.lng
          });
          stopCoords.push(coords);
        } else {
          console.warn(`Could not geocode stop: ${stopAddress}`);
        }
      }

      // Build Request with form data
      const request: CreateRideRequest = {
        start: { 
          address: this.pickupAddress, 
          latitude: startCoords.lat, 
          longitude: startCoords.lng 
        },
        destination: { 
          address: this.destinationAddress, 
          latitude: destCoords.lat, 
          longitude: destCoords.lng 
        },
        stops: stops,
        passengerEmails: this.linkedPassengers,
        scheduledTime: rideData.scheduledTime,
        requirements: {
          vehicleType: rideData.vehicleType,
          babyTransport: rideData.babyTransport,
          petTransport: rideData.petTransport
        }
      };

      // Call Backend
      this.rideService.orderRide(request).subscribe({
        next: (response) => {
          console.log('FULL response:', response);

          this.orderingResult = response;

          if(response.status === 'REJECTED') {
            //TODO call popup with rejection reason
            this.isOrdering = false;
            this.showPopup = true;
            this.clearRoute();
            this.cdr.detectChanges();
            return;
          }

          // Build routePoints safely with fallback
          let routePoints: RoutePoint[];
          if (response.routePoints && response.routePoints.length > 0) {
            routePoints = response.routePoints;
          } else {
            routePoints = [
              response.departure && { location: response.departure, order: 0 },
              ...(response.stops ?? [])
                .filter(stop => stop?.latitude != null && stop?.longitude != null)
                .map((stop: LocationDto, i: number) => ({
                  location: stop,
                  order: i + 1
                })),
              response.destination && { 
                location: response.destination, 
                order: (response.stops?.length ?? 0) + 1 
              }
            ].filter(Boolean) as RoutePoint[];
          }

          const estimateResponse: RideEstimationResponse = {
            estimatedCost: response.estimatedCost ?? 0,
            estimatedTimeInMinutes: response.estimatedTimeInMinutes ?? 0,
            estimatedDriverArrivalInMinutes: 676767,
            estimatedDistance: response.distanceKm ?? 0,
            routePoints
          };

          this.displayRoute(startCoords, destCoords, stopCoords, stops, estimateResponse);
          this.isOrdering = false;
          this.showPopup = true;

          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Ordering failed', err);
          this.orderingError = 'Could not calculate ride. Server might be unreachable.';
          this.isOrdering = false;
          this.cdr.detectChanges();
        }
      });

    } catch (error) {
      this.orderingError = 'An error occurred. Please try again.';
      this.isOrdering = false;
      this.cdr.detectChanges();
    }
  }

  private displayRoute(
    start: L.LatLng, 
    end: L.LatLng, 
    stopCoords: {lat: number, lng: number}[],
    stopAddresses: any[],
    estimation: RideEstimationResponse
  ): void {
    this.clearRoute();

    // Add Pickup Marker
    this.pickupMarker = L.marker(start, { 
      icon: this.pickupIcon, 
      zIndexOffset: 1000 
    })
      .addTo(this.map)
      .bindPopup('<b>Pickup</b><br>' + this.pickupAddress);

    // Add Stop Markers
    this.stopMarkers = [];
    stopCoords.forEach((coords, index) => {
      const stopMarker = L.marker([coords.lat, coords.lng], {
        icon: this.stopIcon,
        zIndexOffset: 900
      })
        .addTo(this.map)
        .bindPopup(`<b>Stop ${index + 1}</b><br>${stopAddresses[index].address}`);
      
      this.stopMarkers.push(stopMarker);
    });

    // Add Destination Marker
    this.destinationMarker = L.marker(end, { 
      icon: this.destinationIcon, 
      zIndexOffset: 1000 
    })
      .addTo(this.map)
      .bindPopup('<b>Final Destination</b><br>' + this.destinationAddress);

      console.log('RoutePoints:', estimation.routePoints);
      console.log('First point:', estimation.routePoints?.[0]);
      console.log('Location:', estimation.routePoints?.[0]?.location);
    // Draw Route (Yellow + Dashed)
    if (estimation.routePoints && estimation.routePoints.length > 0) {
      const latLngs = estimation.routePoints.map(p => 
        L.latLng(p.location.latitude, p.location.longitude)
      );
      
      this.routeLayer = L.polyline(latLngs, {
        color: '#eab308', // Yellow
        weight: 4,
        opacity: 0.9,
        dashArray: '12, 8', // Dashed
        lineCap: 'round',
        lineJoin: 'round'
      }).addTo(this.map);

      this.map.fitBounds(this.routeLayer.getBounds(), { padding: [50, 50] });
    } else {
      // If no route points from backend, create bounds including all points
      const allPoints = [start, end, ...stopCoords.map(c => L.latLng(c.lat, c.lng))];
      this.map.fitBounds(L.latLngBounds(allPoints), { padding: [50, 50] });
    }
  }

  private clearRoute(): void {
    if (this.pickupMarker) {
      this.map.removeLayer(this.pickupMarker);
      this.pickupMarker = null;
    }
    
    if (this.destinationMarker) {
      this.map.removeLayer(this.destinationMarker);
      this.destinationMarker = null;
    }
    
    // Clear all stop markers
    if (this.stopMarkers && this.stopMarkers.length > 0) {
      this.stopMarkers.forEach(marker => {
        this.map.removeLayer(marker);
      });
      this.stopMarkers = [];
    }
    
    if (this.routeLayer) {
      this.map.removeLayer(this.routeLayer);
      this.routeLayer = null;
    }
  }

  resetOrdering(): void {
    this.pickupAddress = '';
    this.destinationAddress = '';
    this.orderingError = '';
    this.clearRoute();
    const { defaultLat, defaultLng, defaultZoom } = environment.map;
    this.map.setView([defaultLat, defaultLng], defaultZoom);
    this.cdr.detectChanges();
  }

  private async geocodeAddress(address: string): Promise<L.LatLng | null> {
    try {
      const encoded = encodeURIComponent(address);
      const url = `https://nominatim.openstreetmap.org/search?format=json&q=${encoded}&limit=1`;
      const data: any = await this.http.get(url).toPromise();
      
      if (data && data.length > 0) {
        return L.latLng(parseFloat(data[0].lat), parseFloat(data[0].lon));
      }
      return null;
    } catch {
      return null;
    }
  }

  linkPassengers(data: LinkedPassengersData): void {
    // Store emails in the global array for later usage
    this.linkedPassengers = data.emails;
  }

  closePopup(): void {
    this.showPopup = false;
  }
  
}
