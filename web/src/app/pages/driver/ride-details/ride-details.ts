import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Ride, mockRides } from '../../../shared/data/mock-data';
import { RideSummaryComponent } from '../../../shared/rides/ride-summary/ride-summary.component';
import { RideRouteComponent } from '../../../shared/rides/ride-route/ride-route.component';
import { RidePassengerComponent } from '../../../shared/rides/ride-passenger/ride-passenger.component';
import { RideMapComponent } from '../../../shared/rides/ride-map/ride-map.component';
import { RideVehicleComponent } from '../../../shared/rides/ride-vehicle/ride-vehicle.component';
import { RideTimelineComponent } from '../../../shared/rides/ride-timeline/ride-timeline.component';

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
    private router: Router
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.ride = mockRides.find(r => r.id === id);
    }
  }

  goBack() {
    this.router.navigate(['/driver/overview']);
  }
}
