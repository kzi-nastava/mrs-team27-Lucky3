import { Component, EventEmitter, Input, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RideOrderData } from '../model/order-ride-data.interface';
import { RideEstimation } from '../model/order-ride-data.interface';
import { RideResponse } from '../../../infrastructure/rest/model/ride-response.model';

@Component({
  selector: 'app-ride-ordering-form',
  imports: [CommonModule, FormsModule],
  templateUrl: './ride-ordering-form.component.html'
})
export class RideOrderingFormComponent {
  @Input() orderingResult: RideResponse | null = null;
  @Input() showOrderingForm: boolean = false;
  @Input() resetOrderingFunction!: () => void; // ADD THIS
  @Input() orderingError: string = ''; // Make sure this is here
  @Input() isOrdering: boolean = false; // Make sure this is here

  @Output() toggleForm = new EventEmitter<void>();
  @Output() orderRideRequest = new EventEmitter<RideOrderData>();
  //@Output() resetRequest = new EventEmitter<void>();

  // NEW: Inputs for prefilled locations
  @Input() prefilledStartLocation: string | null = null;
  @Input() prefilledEndLocation: string | null = null;

  pickupAddress: string = '';
  destinationAddress: string = '';
  intermediateStops: string[] = [];
  selectedVehicleType: string = 'standard';
  petTransport: boolean = false;
  babyTransport: boolean = false;

  toggleOrderingForm(): void {
    this.toggleForm.emit();
  }

  addStop(): void {
    this.intermediateStops.push('');
  }

  // NEW: Handle changes to input properties when ordering from favourites
  ngOnChanges(changes: SimpleChanges): void {
    // Check if inputs were cleared (set to null)
    if (changes['prefilledStartLocation']) {
      if (this.prefilledStartLocation === null) {
        this.pickupAddress = '';
      } else if (this.prefilledStartLocation) {
        this.pickupAddress = this.prefilledStartLocation;
      }
    }
    
    if (changes['prefilledEndLocation']) {
      if (this.prefilledEndLocation === null) {
        this.destinationAddress = '';
      } else if (this.prefilledEndLocation) {
        this.destinationAddress = this.prefilledEndLocation;
      }
    }
  }

  trackByIndex(index: number, item: any): number {
    return index;
  }

  removeStop(index: number): void {
    this.intermediateStops.splice(index, 1);
  }

  orderRide(): void {
    // Validate inputs
    if (!this.pickupAddress.trim() || !this.destinationAddress.trim()) {
      return; // Parent will handle error
    }

    // Emit data to parent component
    const rideData: RideOrderData = {
      pickupAddress: this.pickupAddress.trim(),
      destinationAddress: this.destinationAddress.trim(),
      intermediateStops: this.intermediateStops.filter(stop => stop.trim() !== ''),
      vehicleType: this.selectedVehicleType.toUpperCase(),
      petTransport: this.petTransport,
      babyTransport: this.babyTransport
    };

    this.orderRideRequest.emit(rideData);
  }

  resetOrdering(): void {
    this.pickupAddress = '';
    this.destinationAddress = '';
    this.intermediateStops = [];
    this.selectedVehicleType = 'standard';
    this.petTransport = false;
    this.babyTransport = false;
    this.orderingResult = null;
    this.orderingError = '';
    this.prefilledEndLocation = null;
    this.prefilledStartLocation = null;
    // this.resetRequest.emit();
  }

  // Method to set estimation result from parent
  setOrderingResult(result: RideResponse | null): void {
    this.orderingResult = result;
    this.isOrdering = false;
  }

  // Method to set error from parent
  setError(error: string): void {
    this.orderingError = error;
    this.isOrdering = false;
  }

  
}
