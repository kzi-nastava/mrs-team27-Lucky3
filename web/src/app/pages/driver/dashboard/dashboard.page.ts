import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StatCardComponent } from '../../../shared/ui/stat-card/stat-card.component';
import { ToggleSwitchComponent } from '../../../shared/ui/toggle-switch/toggle-switch.component';
import { RideRequestCardComponent } from '../../../shared/rides/ride-request-card/ride-request-card.component';
import { ActiveRideCardComponent } from '../../../shared/rides/active-ride-card/active-ride-card.component';
import { MapPlaceholderComponent } from '../../../shared/ui/map-placeholder/map-placeholder.component';

@Component({
  selector: 'app-driver-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    StatCardComponent,
    ToggleSwitchComponent,
    RideRequestCardComponent,
    ActiveRideCardComponent,
    MapPlaceholderComponent
  ],
  templateUrl: './dashboard.page.html',
  styleUrls: ['./dashboard.page.css']
})
export class DashboardPage {
  isOnline: boolean = true;
  driverName: string = 'James';
  
  stats = {
    earnings: 245.5,
    ridesCompleted: 12,
    rating: 4.9,
    onlineHours: 6.5
  };

  availableRides = [
    {
      type: 'ECONOMY',
      price: 15.00,
      distance: '4.1km',
      time: '12min',
      pickup: '333 Bush St, San Francisco, CA 941',
      dropoff: '444 Kearny St, San Francisco, CA 941'
    }
  ];

  onStatusChange(status: boolean) {
    this.isOnline = status;
  }
}
