import { Component, OnInit, OnDestroy, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import * as L from 'leaflet';
import { environment } from '../../../env/environment';

// Vehicle interface for active vehicles on map
interface Vehicle {
  id: string;
  lat: number;
  lng: number;
  isAvailable: boolean;
  driverName: string;
  vehicleType: string;
  licensePlate: string;
}

// Route estimation response
interface RouteEstimation {
  distance: number; // km
  duration: number; // minutes
  price: number;
  route: L.LatLng[];
}

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './home.page.html',
  styleUrl: './home.page.css',
})
export class HomePage implements OnInit, AfterViewInit, OnDestroy {
  private map!: L.Map;
  private vehicleMarkers: L.Marker[] = [];
  private routeLayer: L.Polyline | null = null;
  private pickupMarker: L.Marker | null = null;
  private destinationMarker: L.Marker | null = null;

  // Form state
  showEstimationForm = false;
  pickupAddress = '';
  destinationAddress = '';
  isEstimating = false;
  estimationResult: RouteEstimation | null = null;
  estimationError = '';

  // Mock vehicles data (simulating real-time vehicles)
  vehicles: Vehicle[] = [
    { id: 'v1', lat: 45.2551, lng: 19.8428, isAvailable: true, driverName: 'Marko P.', vehicleType: 'Standard', licensePlate: 'NS-001-AA' },
    { id: 'v2', lat: 45.2501, lng: 19.8478, isAvailable: false, driverName: 'Jovan S.', vehicleType: 'Comfort', licensePlate: 'NS-002-BB' },
    { id: 'v3', lat: 45.2621, lng: 19.8378, isAvailable: true, driverName: 'Ana M.', vehicleType: 'Standard', licensePlate: 'NS-003-CC' },
    { id: 'v4', lat: 45.2471, lng: 19.8528, isAvailable: false, driverName: 'Stefan D.', vehicleType: 'Luxury', licensePlate: 'NS-004-DD' },
    { id: 'v5', lat: 45.2591, lng: 19.8258, isAvailable: true, driverName: 'Milan K.', vehicleType: 'Standard', licensePlate: 'NS-005-EE' },
    { id: 'v6', lat: 45.2441, lng: 19.8608, isAvailable: true, driverName: 'Nikola R.', vehicleType: 'Comfort', licensePlate: 'NS-006-FF' },
    { id: 'v7', lat: 45.2681, lng: 19.8458, isAvailable: false, driverName: 'Petar B.', vehicleType: 'Standard', licensePlate: 'NS-007-GG' },
  ];

  // Custom marker icons
  private availableVehicleIcon = L.divIcon({
    className: '',
    html: `<div style="display:flex;align-items:center;justify-content:center;width:40px;height:40px;border-radius:50%;background:linear-gradient(135deg,rgba(34,197,94,0.2),rgba(34,197,94,0.1));border:2px solid rgba(34,197,94,0.5);color:#22c55e;box-shadow:0 0 20px rgba(34,197,94,0.3);">
      <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
        <path d="M18.92 6.01C18.72 5.42 18.16 5 17.5 5h-11c-.66 0-1.21.42-1.42 1.01L3 12v8c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1h12v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-8l-2.08-5.99zM6.5 16c-.83 0-1.5-.67-1.5-1.5S5.67 13 6.5 13s1.5.67 1.5 1.5S7.33 16 6.5 16zm11 0c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zM5 11l1.5-4.5h11L19 11H5z"/>
      </svg>
    </div>`,
    iconSize: [40, 40],
    iconAnchor: [20, 20],
  });

  private occupiedVehicleIcon = L.divIcon({
    className: '',
    html: `<div style="display:flex;align-items:center;justify-content:center;width:40px;height:40px;border-radius:50%;background:linear-gradient(135deg,rgba(239,68,68,0.2),rgba(239,68,68,0.1));border:2px solid rgba(239,68,68,0.5);color:#ef4444;box-shadow:0 0 20px rgba(239,68,68,0.3);">
      <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
        <path d="M18.92 6.01C18.72 5.42 18.16 5 17.5 5h-11c-.66 0-1.21.42-1.42 1.01L3 12v8c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1h12v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-8l-2.08-5.99zM6.5 16c-.83 0-1.5-.67-1.5-1.5S5.67 13 6.5 13s1.5.67 1.5 1.5S7.33 16 6.5 16zm11 0c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zM5 11l1.5-4.5h11L19 11H5z"/>
      </svg>
    </div>`,
    iconSize: [40, 40],
    iconAnchor: [20, 20],
  });

  private pickupIcon = L.divIcon({
    className: '',
    html: `<div style="display:flex;align-items:center;justify-content:center;color:#22c55e;filter:drop-shadow(0 0 8px rgba(34,197,94,0.5));">
      <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10"/>
        <circle cx="12" cy="12" r="3" fill="currentColor"/>
      </svg>
    </div>`,
    iconSize: [32, 32],
    iconAnchor: [16, 32],
  });

  private destinationIcon = L.divIcon({
    className: '',
    html: `<div style="display:flex;align-items:center;justify-content:center;color:#ef4444;filter:drop-shadow(0 0 8px rgba(239,68,68,0.5));">
      <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
      </svg>
    </div>`,
    iconSize: [32, 32],
    iconAnchor: [16, 32],
  });

  ngOnInit(): void {}

  ngAfterViewInit(): void {
    this.initMap();
    this.addVehicleMarkers();
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
  }

  private initMap(): void {
    const { defaultLat, defaultLng, defaultZoom } = environment.map;

    this.map = L.map('map', {
      center: [defaultLat, defaultLng],
      zoom: defaultZoom,
      zoomControl: false,
    });

    // Dark theme tile layer (CartoDB Dark Matter)
    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>',
      subdomains: 'abcd',
      maxZoom: 20
    }).addTo(this.map);

    // Add zoom control to bottom-left
    L.control.zoom({
      position: 'bottomleft'
    }).addTo(this.map);
  }

  private addVehicleMarkers(): void {
    this.vehicles.forEach(vehicle => {
      const icon = vehicle.isAvailable ? this.availableVehicleIcon : this.occupiedVehicleIcon;
      const statusText = vehicle.isAvailable ? 'Available' : 'Occupied';
      const statusClass = vehicle.isAvailable ? 'text-green-400' : 'text-red-400';
      
      const marker = L.marker([vehicle.lat, vehicle.lng], { icon })
        .addTo(this.map)
        .bindPopup(`
          <div style="min-width:140px;">
            <div style="font-weight:600;color:white;">${vehicle.driverName}</div>
            <div style="color:#9ca3af;font-size:0.875rem;">${vehicle.vehicleType} • ${vehicle.licensePlate}</div>
            <div style="color:${vehicle.isAvailable ? '#4ade80' : '#f87171'};font-size:0.875rem;font-weight:500;">${statusText}</div>
          </div>
        `);
      
      this.vehicleMarkers.push(marker);
    });
  }

  toggleEstimationForm(): void {
    this.showEstimationForm = !this.showEstimationForm;
    if (!this.showEstimationForm) {
      this.resetEstimation();
    }
  }

  async estimateRide(): Promise<void> {
    if (!this.pickupAddress.trim() || !this.destinationAddress.trim()) {
      this.estimationError = 'Please enter both pickup and destination addresses';
      return;
    }

    this.isEstimating = true;
    this.estimationError = '';
    this.estimationResult = null;

    try {
      // Geocode pickup address
      const pickupCoords = await this.geocodeAddress(this.pickupAddress);
      if (!pickupCoords) {
        this.estimationError = 'Could not find pickup location. Please try a different address.';
        this.isEstimating = false;
        return;
      }

      // Geocode destination address
      const destCoords = await this.geocodeAddress(this.destinationAddress);
      if (!destCoords) {
        this.estimationError = 'Could not find destination location. Please try a different address.';
        this.isEstimating = false;
        return;
      }

      // Get route from OSRM
      const route = await this.getRoute(pickupCoords, destCoords);
      
      if (route) {
        this.displayRoute(pickupCoords, destCoords, route);
        this.estimationResult = route;
      } else {
        this.estimationError = 'Could not calculate route. Please try different addresses.';
      }
    } catch (error) {
      this.estimationError = 'An error occurred while estimating. Please try again.';
      console.error('Estimation error:', error);
    }

    this.isEstimating = false;
  }

  private async geocodeAddress(address: string): Promise<L.LatLng | null> {
    try {
      const encodedAddress = encodeURIComponent(address + ', Novi Sad, Serbia');
      const response = await fetch(
        `https://nominatim.openstreetmap.org/search?format=json&q=${encodedAddress}&limit=1`
      );
      const data = await response.json();
      
      if (data && data.length > 0) {
        return L.latLng(parseFloat(data[0].lat), parseFloat(data[0].lon));
      }
      return null;
    } catch (error) {
      console.error('Geocoding error:', error);
      return null;
    }
  }

  private async getRoute(start: L.LatLng, end: L.LatLng): Promise<RouteEstimation | null> {
    try {
      const response = await fetch(
        `https://router.project-osrm.org/route/v1/driving/${start.lng},${start.lat};${end.lng},${end.lat}?overview=full&geometries=geojson`
      );
      const data = await response.json();
      
      if (data.code === 'Ok' && data.routes && data.routes.length > 0) {
        const route = data.routes[0];
        const distance = route.distance / 1000; // Convert to km
        const duration = route.duration / 60; // Convert to minutes
        
        // Calculate price (base fare + per km rate)
        const baseFare = 150; // RSD
        const perKmRate = 65; // RSD per km
        const price = baseFare + (distance * perKmRate);
        
        // Convert GeoJSON coordinates to Leaflet LatLng array
        const routeCoords = route.geometry.coordinates.map((coord: number[]) => 
          L.latLng(coord[1], coord[0])
        );
        
        return {
          distance: Math.round(distance * 10) / 10,
          duration: Math.round(duration),
          price: Math.round(price),
          route: routeCoords
        };
      }
      return null;
    } catch (error) {
      console.error('Routing error:', error);
      return null;
    }
  }

  private displayRoute(pickup: L.LatLng, destination: L.LatLng, estimation: RouteEstimation): void {
    // Clear previous route
    this.clearRoute();
    
    // Add pickup marker
    this.pickupMarker = L.marker(pickup, { icon: this.pickupIcon })
      .addTo(this.map)
      .bindPopup('<div style="color:white;font-weight:500;">Pickup Location</div>');
    
    // Add destination marker
    this.destinationMarker = L.marker(destination, { icon: this.destinationIcon })
      .addTo(this.map)
      .bindPopup('<div style="color:white;font-weight:500;">Destination</div>');
    
    // Draw route
    this.routeLayer = L.polyline(estimation.route, {
      color: '#eab308',
      weight: 4,
      opacity: 0.8,
      dashArray: '10, 10'
    }).addTo(this.map);
    
    // Fit map to show entire route
    const bounds = L.latLngBounds([pickup, destination]);
    this.map.fitBounds(bounds, { padding: [80, 80] });
  }

  private clearRoute(): void {
    if (this.routeLayer) {
      this.map.removeLayer(this.routeLayer);
      this.routeLayer = null;
    }
    if (this.pickupMarker) {
      this.map.removeLayer(this.pickupMarker);
      this.pickupMarker = null;
    }
    if (this.destinationMarker) {
      this.map.removeLayer(this.destinationMarker);
      this.destinationMarker = null;
    }
  }

  resetEstimation(): void {
    this.pickupAddress = '';
    this.destinationAddress = '';
    this.estimationResult = null;
    this.estimationError = '';
    this.clearRoute();
    
    // Reset map view
    const { defaultLat, defaultLng, defaultZoom } = environment.map;
    this.map.setView([defaultLat, defaultLng], defaultZoom);
  }

  // Count available and occupied vehicles
  get availableCount(): number {
    return this.vehicles.filter(v => v.isAvailable).length;
  }

  get occupiedCount(): number {
    return this.vehicles.filter(v => !v.isAvailable).length;
  }
}
