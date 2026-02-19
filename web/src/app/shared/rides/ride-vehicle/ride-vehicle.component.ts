import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Ride } from '../../data/ride.model';

@Component({
  selector: 'app-ride-vehicle',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ride-vehicle.component.html',
  styleUrl: './ride-vehicle.component.css'
})
export class RideVehicleComponent {
  @Input() ride!: Ride;
}
