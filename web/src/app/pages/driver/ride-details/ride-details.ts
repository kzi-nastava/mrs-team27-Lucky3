import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Ride } from '../../../shared/data/mock-data';
import { RideSummaryComponent } from '../../../shared/rides/ride-summary/ride-summary.component';
import { RideRouteComponent } from '../../../shared/rides/ride-route/ride-route.component';
import { RidePassengerComponent } from '../../../shared/rides/ride-passenger/ride-passenger.component';
import { RideMapComponent } from '../../../shared/rides/ride-map/ride-map.component';
import { RideVehicleComponent } from '../../../shared/rides/ride-vehicle/ride-vehicle.component';
import { RideTimelineComponent } from '../../../shared/rides/ride-timeline/ride-timeline.component';
import { RideService } from '../../../infrastructure/rest/ride.service';
import { RideResponse } from '../../../infrastructure/rest/model/ride-response.model';

@Component({
  selector: 'app-ride-details',
  standalone: true,
  imports: [
    CommonModule,
    RideSummaryComponent,
    RideRouteComponent,
    RidePassengerComponent,
    RideMapComponent,
    RideVehicleComponent,
    RideTimelineComponent
  ],
  templateUrl: './ride-details.html',
  styleUrl: './ride-details.css'
})
export class RideDetails implements OnInit {
  ride: Ride | undefined;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private rideService: RideService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
        this.rideService.getRide(Number(id)).subscribe({
            next: (r) => {
                this.ride = this.mapToRide(r);
                this.cdr.detectChanges();
            },
            error: (err) => console.error(err)
        });
    }
  }

  private mapToRide(r: RideResponse): Ride {
    // simplified mapping
    return {
      id: String(r.id),
      driverId: String(r.driver?.id),
      startedAt: r.startTime,
      requestedAt: r.startTime ?? '',
      completedAt: r.endTime,
      status: r.status === 'FINISHED' ? 'completed' : (r.status === 'CANCELLED' ? 'cancelled' : 'active'),
      fare: r.totalCost ?? 0,
      distance: r.distanceKm ?? 0,
      pickup: { address: r.departure?.address ?? r.start?.address ?? r.startLocation?.address ?? '—' },
      destination: { address: r.destination?.address ?? r.endLocation?.address ?? '—' },
      hasPanic: r.panicPressed,
      passengerName: r.passengers?.[0]?.name ?? 'Unknown',
      cancelledBy: 'driver',
      cancellationReason: r.rejectionReason
    };
  }

  goBack() {
    this.router.navigate(['/driver/overview']);
  }
}
