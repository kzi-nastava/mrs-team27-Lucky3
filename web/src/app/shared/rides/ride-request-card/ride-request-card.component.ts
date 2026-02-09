import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-ride-request-card',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './ride-request-card.component.html'
})
export class RideRequestCardComponent {
  @Input() link: any[] | string | null = null;

  @Input() type: string = 'ECONOMY';
  @Input() price: number = 0;
  @Input() distance: string = '';
  @Input() time: string = '';
  @Input() pickup: string = '';
  @Input() dropoff: string = '';

  @Input() showAction: boolean = true;
  @Input() actionLabel: string = 'Accept Ride';
  @Input() actionVariant: 'primary' | 'danger' = 'primary';

  @Output() action = new EventEmitter<void>();

  onActionClick(): void {
    this.action.emit();
  }
}
