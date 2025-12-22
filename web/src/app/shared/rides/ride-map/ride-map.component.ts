import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Ride } from '../../data/mock-data';

@Component({
  selector: 'app-ride-map',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ride-map.component.html',
  styleUrl: './ride-map.component.css'
})
export class RideMapComponent {
  @Input() ride!: Ride;
}
