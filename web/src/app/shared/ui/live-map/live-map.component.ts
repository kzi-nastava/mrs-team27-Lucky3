import { AfterViewInit, Component, ElementRef, Input, OnChanges, OnDestroy, SimpleChanges, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import * as L from 'leaflet';
import { environment } from '../../../../env/environment';

export type MapPoint = { latitude: number; longitude: number };

@Component({
  selector: 'app-live-map',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './live-map.component.html',
  styleUrl: './live-map.component.css'
})
export class LiveMapComponent implements AfterViewInit, OnChanges, OnDestroy {
  @ViewChild('mapContainer', { static: true }) mapContainer!: ElementRef<HTMLDivElement>;

  @Input() driverLocation: MapPoint | null = null;
  @Input() rideRoute: MapPoint[] | null = null;     // Yellow line (The main ride)
  @Input() approachRoute: MapPoint[] | null = null; // Blue line (Driver -> Pickup)
  @Input() heightClass: string = 'h-[360px]';
  @Input() isOffline: boolean = false;  // When true, shows "Offline" overlay and hides routes

  private map: L.Map | null = null;
  private driverMarker: L.Marker | null = null;
  private ridePolyline: L.Polyline | null = null;
  private approachPolyline: L.Polyline | null = null;
  private pickupMarker: L.Marker | null = null;
  private dropoffMarker: L.Marker | null = null;
  private hasAutoCentered = false;

  private resizeObserver: ResizeObserver | null = null;
  private invalidateQueued = false;
  private onWindowResize = () => this.queueInvalidateSize();

  // Blue circle icon for vehicle location
  private driverIcon = L.divIcon({
    className: '',
    html: `<div style="width:16px;height:16px;border-radius:9999px;background:#3b82f6;box-shadow:0 0 0 4px rgba(59,130,246,0.25);border:2px solid #fff;"></div>`,
    iconSize: [16, 16],
    iconAnchor: [8, 8],
    popupAnchor: [0, -8]
  });

  private pickupIcon = L.divIcon({
    className: '',
    html: `<div style="width:14px;height:14px;border-radius:9999px;background:rgba(34,197,94,0.95);box-shadow:0 0 0 6px rgba(34,197,94,0.18);"></div>`,
    iconSize: [14, 14],
    iconAnchor: [7, 7]
  });

  private dropoffIcon = L.divIcon({
    className: '',
    html: `<div style="width:14px;height:14px;border-radius:9999px;background:rgba(239,68,68,0.95);box-shadow:0 0 0 6px rgba(239,68,68,0.18);"></div>`,
    iconSize: [14, 14],
    iconAnchor: [7, 7]
  });

  ngAfterViewInit(): void {
    this.initMap();
    this.syncDriverMarker();

    // Leaflet sometimes needs a tick after being inserted into a flex/grid container.
    setTimeout(() => this.queueInvalidateSize(), 0);

    if (typeof window !== 'undefined') {
      window.addEventListener('resize', this.onWindowResize, { passive: true });
    }

    if (typeof ResizeObserver !== 'undefined') {
      this.resizeObserver = new ResizeObserver(() => this.queueInvalidateSize());
      this.resizeObserver.observe(this.mapContainer.nativeElement);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['driverLocation']) {
      this.syncDriverMarker();
    }
    if (changes['rideRoute'] || changes['approachRoute'] || changes['isOffline']) {
      this.syncRoutes();
    }
  }

  ngOnDestroy(): void {
    if (typeof window !== 'undefined') {
      window.removeEventListener('resize', this.onWindowResize);
    }

    this.resizeObserver?.disconnect();
    this.resizeObserver = null;

    this.map?.remove();
    this.map = null;
  }

  private queueInvalidateSize(): void {
    if (!this.map || this.invalidateQueued) return;

    this.invalidateQueued = true;
    requestAnimationFrame(() => {
      this.invalidateQueued = false;
      this.map?.invalidateSize();
    });
  }

  private initMap(): void {
    const { defaultLat, defaultLng, defaultZoom } = environment.map;

    this.map = L.map(this.mapContainer.nativeElement, {
      center: [defaultLat, defaultLng],
      zoom: defaultZoom,
      zoomControl: false
    });

    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
      attribution: 'OpenStreetMap contributors',
      subdomains: 'abcd',
      maxZoom: 20
    }).addTo(this.map);

    L.control.zoom({ position: 'bottomleft' }).addTo(this.map);
  }

  private syncDriverMarker(): void {
    if (!this.map || !this.driverLocation) return;

    const latlng: L.LatLngExpression = [this.driverLocation.latitude, this.driverLocation.longitude];

    if (!this.driverMarker) {
      this.driverMarker = L.marker(latlng, { icon: this.driverIcon, zIndexOffset: 1000 })
        .addTo(this.map)
        .bindPopup('Your current location');
    } else {
      this.driverMarker.setLatLng(latlng);
    }

    // Auto-center initially on driver, but if routes exist, we might want to fit bounds instead (handled in syncRoutes)
    if (!this.hasAutoCentered && !this.rideRoute && !this.approachRoute) {
      this.map.setView(latlng, 15);
      this.hasAutoCentered = true;
    }
  }

  private syncRoutes(): void {
    if (!this.map) return;

    // Remove existing
    if (this.ridePolyline) {
      this.ridePolyline.remove();
      this.ridePolyline = null;
    }
    if (this.approachPolyline) {
      this.approachPolyline.remove();
      this.approachPolyline = null;
    }
    if (this.pickupMarker) {
        this.pickupMarker.remove();
        this.pickupMarker = null;
    }
    if (this.dropoffMarker) {
        this.dropoffMarker.remove();
        this.dropoffMarker = null;
    }

    // If offline, don't draw any routes
    if (this.isOffline) {
      return;
    }

    const bounds = L.latLngBounds([]);

    // 1. Ride Polyline (Yellow)
    if (this.rideRoute && this.rideRoute.length > 1) {
      const latlngs = this.rideRoute.map(p => [p.latitude, p.longitude] as L.LatLngExpression);
      this.ridePolyline = L.polyline(latlngs, {
        color: '#eab308', // yellow
        weight: 4,
        opacity: 0.9,
        dashArray: '12, 8',
        lineCap: 'round',
        lineJoin: 'round'
      }).addTo(this.map);
      
      this.pickupMarker = L.marker(latlngs[0], { icon: this.pickupIcon, zIndexOffset: 950 }).addTo(this.map);
      this.dropoffMarker = L.marker(latlngs[latlngs.length - 1], { icon: this.dropoffIcon, zIndexOffset: 950 }).addTo(this.map);

      latlngs.forEach(ll => bounds.extend(ll));
    }

    // 2. Approach Polyline (Light Blue - dotted line)
    if (this.approachRoute && this.approachRoute.length > 1) {
      const latlngs = this.approachRoute.map(p => [p.latitude, p.longitude] as L.LatLngExpression);
      this.approachPolyline = L.polyline(latlngs, {
        color: '#00a2ff', // light blue (sky-300)
        weight: 3,
        opacity: 0.95,
        dashArray: '8, 8',
        lineCap: 'round',
        lineJoin: 'round'
      }).addTo(this.map);
      this.approachPolyline.bringToFront();

      latlngs.forEach(ll => bounds.extend(ll));
      
      // Also add pickup marker here if not already added by rideRoute
      if (!this.pickupMarker) {
          this.pickupMarker = L.marker(latlngs[latlngs.length - 1], { icon: this.pickupIcon, zIndexOffset: 950 }).addTo(this.map);
      }
    }

    // Include driver location in bounds if present
    if (this.driverLocation) {
      bounds.extend([this.driverLocation.latitude, this.driverLocation.longitude]);
    }

    // If we have any route, fit bounds
    if (bounds.isValid()) {
      this.map.fitBounds(bounds, { padding: [40, 40] });
      this.hasAutoCentered = true;
    }
  }
}
