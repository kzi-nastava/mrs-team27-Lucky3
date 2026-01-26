import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { CommonModule } from '@angular/common';
import * as L from 'leaflet';
import { environment } from '../../../../env/environment';

export type MapPoint = { latitude: number; longitude: number };

export interface ActiveRideMapData {
  start: MapPoint;
  stops: MapPoint[];
  end: MapPoint;
}

@Component({
  selector: 'app-active-ride-map',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './active-ride-map.component.html',
  styleUrl: './active-ride-map.component.css'
})
export class ActiveRideMapComponent implements AfterViewInit, OnChanges, OnDestroy {
  @ViewChild('mapContainer', { static: true }) mapContainer!: ElementRef<HTMLDivElement>;

  @Input() driverLocation: MapPoint | null = null;
  @Input() ride: ActiveRideMapData | null = null;
  @Input() routePolyline: MapPoint[] | null = null;
  @Input() approachRoute: MapPoint[] | null = null; // Blue line (vehicle to next stop)
  @Input() remainingRoute: MapPoint[] | null = null; // Yellow dotted (rest of route after next stop)
  @Input() completedStopIndexes: Set<number> = new Set();
  @Input() isRideInProgress: boolean = false;

  private map: L.Map | null = null;
  private driverMarker: L.Marker | null = null;
  private routeLine: L.Polyline | null = null;
  private approachLine: L.Polyline | null = null;
  private remainingLine: L.Polyline | null = null;
  private startMarker: L.Marker | null = null;
  private stopMarkers: L.Marker[] = [];
  private endMarker: L.Marker | null = null;

  private resizeObserver: ResizeObserver | null = null;
  private invalidateQueued = false;
  private onWindowResize = () => this.queueInvalidateSize();

  // Blue circle icon for vehicle location
  private driverIcon = L.divIcon({
    className: '',
    html: `<div style="width:16px;height:16px;border-radius:9999px;background:#3b82f6;box-shadow:0 0 0 4px rgba(59,130,246,0.25);border:2px solid #fff;"></div>`,
    iconSize: [16, 16],
    iconAnchor: [8, 8]
  });

  private startIcon = L.divIcon({
    className: '',
    html: `<div style="width:14px;height:14px;border-radius:9999px;background:rgba(34,197,94,0.95);box-shadow:0 0 0 6px rgba(34,197,94,0.18);"></div>`,
    iconSize: [14, 14],
    iconAnchor: [7, 7]
  });

  private getStopIcon(isCompleted: boolean) {
    if (isCompleted) {
      return L.divIcon({
        className: '',
        html: `<div style="width:12px;height:12px;border-radius:9999px;background:rgba(34,197,94,0.95);box-shadow:0 0 0 5px rgba(34,197,94,0.18);border:2px solid white;"></div>`,
        iconSize: [12, 12],
        iconAnchor: [6, 6]
      });
    }
    return L.divIcon({
      className: '',
      html: `<div style="width:12px;height:12px;border-radius:9999px;background:rgba(156,163,175,0.85);box-shadow:0 0 0 5px rgba(156,163,175,0.12);border:2px solid rgba(255,255,255,0.9);"></div>`,
      iconSize: [12, 12],
      iconAnchor: [6, 6]
    });
  }

  private endIcon = L.divIcon({
    className: '',
    html: `<div style="width:14px;height:14px;border-radius:9999px;background:rgba(239,68,68,0.95);box-shadow:0 0 0 6px rgba(239,68,68,0.18);"></div>`,
    iconSize: [14, 14],
    iconAnchor: [7, 7]
  });

  ngAfterViewInit(): void {
    this.initMap();
    this.syncRide();
    this.syncDriver();
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
    // Sync approach and remaining routes first (these affect how the main route is rendered)
    if (changes['approachRoute']) {
      this.syncApproach();
    }
    if (changes['remainingRoute']) {
      this.syncRemainingRoute();
    }
    
    // Then sync the main ride display (markers, full route if no approach/remaining)
    if (changes['ride'] || changes['completedStopIndexes'] || changes['routePolyline'] || 
        changes['approachRoute'] || changes['remainingRoute']) {
      this.syncRide();
    }
    
    if (changes['driverLocation']) this.syncDriver();
    
    // Always ensure approach line is on top after any sync
    if (this.approachLine) {
      this.approachLine.bringToFront();
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

  private syncRide(): void {
    if (!this.map) return;

    // Clear previous
    this.routeLine?.remove();
    this.routeLine = null;

    this.startMarker?.remove();
    this.startMarker = null;

    this.stopMarkers.forEach(m => m.remove());
    this.stopMarkers = [];

    this.endMarker?.remove();
    this.endMarker = null;

    if (!this.ride) return;

    const coarsePoints: L.LatLngExpression[] = [
      [this.ride.start.latitude, this.ride.start.longitude],
      ...this.ride.stops.map(s => [s.latitude, s.longitude] as L.LatLngExpression),
      [this.ride.end.latitude, this.ride.end.longitude]
    ];

    const detailed = (this.routePolyline ?? [])
      .filter(p => Number.isFinite(p.latitude) && Number.isFinite(p.longitude))
      .map(p => [p.latitude, p.longitude] as L.LatLngExpression);

    const polyPoints = detailed.length >= 2 ? detailed : coarsePoints;

    // For PENDING rides: Always show the full yellow route (the planned ride path)
    // For IN-PROGRESS rides: Don't show the full route - the approach (blue) + remaining (yellow) routes handle it
    if (!this.isRideInProgress) {
      // Yellow dotted line for the full route (only for pending rides)
      this.routeLine = L.polyline(polyPoints, {
        color: '#eab308',
        weight: 4,
        opacity: 0.9,
        dashArray: '12, 8',
        lineCap: 'round',
        lineJoin: 'round'
      }).addTo(this.map);
    }

    // Only show start marker when ride is NOT in progress (start point already passed)
    if (!this.isRideInProgress) {
      this.startMarker = L.marker(coarsePoints[0], { icon: this.startIcon, zIndexOffset: 900 }).addTo(this.map);
    }

    const stopPoints = coarsePoints.slice(1, -1);
    this.stopMarkers = stopPoints
      .map((p, idx) => {
        // Skip completed stops - don't display them at all
        if (this.completedStopIndexes.has(idx)) return null;
        return L.marker(p, { icon: this.getStopIcon(false), zIndexOffset: 850 }).addTo(this.map!);
      })
      .filter((m): m is L.Marker => m !== null);

    this.endMarker = L.marker(coarsePoints[coarsePoints.length - 1], { icon: this.endIcon, zIndexOffset: 900 }).addTo(this.map);

    const bounds = L.latLngBounds(polyPoints as any);
    this.map.fitBounds(bounds, { padding: [40, 40] });
  }

  private syncApproach(): void {
    if (!this.map) return;
    this.approachLine?.remove();
    this.approachLine = null;

    if (!this.approachRoute || this.approachRoute.length < 2) return;

    const latlngs = this.approachRoute.map(p => [p.latitude, p.longitude] as L.LatLngExpression);
    // Blue solid line from vehicle to next stop
    this.approachLine = L.polyline(latlngs, {
      color: '#3b82f6', // blue-500
      weight: 5,
      opacity: 0.95,
      lineCap: 'round',
      lineJoin: 'round'
    }).addTo(this.map);
    this.approachLine.bringToFront();
  }

  private syncRemainingRoute(): void {
    if (!this.map) return;
    this.remainingLine?.remove();
    this.remainingLine = null;

    if (!this.remainingRoute || this.remainingRoute.length < 2) return;

    const latlngs = this.remainingRoute.map(p => [p.latitude, p.longitude] as L.LatLngExpression);
    // Yellow dotted line for remaining route
    this.remainingLine = L.polyline(latlngs, {
      color: '#eab308', // yellow-500
      weight: 4,
      opacity: 0.9,
      dashArray: '12, 8',
      lineCap: 'round',
      lineJoin: 'round'
    }).addTo(this.map);
  }

  private syncDriver(): void {
    if (!this.map || !this.driverLocation) return;

    const pos: L.LatLngExpression = [this.driverLocation.latitude, this.driverLocation.longitude];

    if (!this.driverMarker) {
      this.driverMarker = L.marker(pos, { icon: this.driverIcon, zIndexOffset: 1000 }).addTo(this.map);
    } else {
      this.driverMarker.setLatLng(pos);
    }
  }
}
