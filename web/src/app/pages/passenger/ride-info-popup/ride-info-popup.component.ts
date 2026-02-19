import { Component, Input, Output, EventEmitter } from '@angular/core';
import { RideResponse } from '../../../infrastructure/rest/model/ride-response.model';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ride-info-popup',
  templateUrl: './ride-info-popup.component.html',
  imports: [CommonModule]
})
export class RideInfoPopupComponent {
  @Input() rideData!: RideResponse;
  @Output() close = new EventEmitter<void>();

  closePopup(): void {
    this.close.emit();
  }
}
