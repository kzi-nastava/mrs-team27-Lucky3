import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Ride, RidePassenger } from '../../data/ride.model';

@Component({
  selector: 'app-ride-passenger',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ride-passenger.component.html',
  styleUrl: './ride-passenger.component.css'
})
export class RidePassengerComponent {
  @Input() ride!: Ride;

  get passengers(): RidePassenger[] {
    return this.ride?.passengers ?? [];
  }
}
