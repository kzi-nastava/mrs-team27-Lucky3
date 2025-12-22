import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-active-ride-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './active-ride-card.component.html'
})
export class ActiveRideCardComponent {
  @Input() hasActiveRide: boolean = false;
}
