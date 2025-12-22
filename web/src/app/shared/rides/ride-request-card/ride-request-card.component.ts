import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ride-request-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ride-request-card.component.html'
})
export class RideRequestCardComponent {
  @Input() type: string = 'ECONOMY';
  @Input() price: number = 0;
  @Input() distance: string = '';
  @Input() time: string = '';
  @Input() pickup: string = '';
  @Input() dropoff: string = '';
}
