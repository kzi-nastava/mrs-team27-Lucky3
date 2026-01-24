import { Component, Input, Output, EventEmitter } from '@angular/core';
import { RideCreated } from '../../../infrastructure/rest/model/order-ride.model';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ride-info-popup',
  templateUrl: './ride-info-popup.component.html',
  imports: [CommonModule]
})
export class RideInfoPopupComponent {
  @Input() rideData!: RideCreated;
  @Output() close = new EventEmitter<void>();

  closePopup(): void {
    this.close.emit();
  }
}
