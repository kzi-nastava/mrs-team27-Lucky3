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
  @Input() heightClass: string = 'h-[360px]';

  private map: L.Map | null = null;
  private driverMarker: L.Marker | null = null;
  private hasAutoCentered = false;

  private resizeObserver: ResizeObserver | null = null;
  private invalidateQueued = false;
  private onWindowResize = () => this.queueInvalidateSize();

  private driverIcon = L.divIcon({
    className: '',
    html: `
      <div style="width:18px;height:18px;border-radius:9999px;background:rgba(234,179,8,0.9);box-shadow:0 0 0 6px rgba(234,179,8,0.20), 0 0 18px rgba(234,179,8,0.35);"></div>
    `,
    iconSize: [18, 18],
    iconAnchor: [9, 9]
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

    if (!this.hasAutoCentered) {
      this.map.setView(latlng, 15);
      this.hasAutoCentered = true;
    }
  }
}
