import { Component, EventEmitter, Input, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RideResponse } from '../../../infrastructure/rest/model/ride-response.model';
import { RideOrderData } from '../model/order-ride-data.interface';

type ScheduleType = 'NOW' | 'IN' | 'AT';

@Component({
  selector: 'app-ride-ordering-form',
  imports: [CommonModule, FormsModule],
  templateUrl: './ride-ordering-form.component.html'
})
export class RideOrderingFormComponent {
  @Input() orderingResult: RideResponse | null = null;
  @Input() showOrderingForm: boolean = false;
  @Input() resetOrderingFunction!: () => void;
  @Input() orderingError: string = '';
  @Input() isOrdering: boolean = false;

  @Output() toggleForm = new EventEmitter<void>();
  @Output() orderRideRequest = new EventEmitter<RideOrderData>();

  @Input() prefilledStartLocation: string | null = null;
  @Input() prefilledEndLocation: string | null = null;

  pickupAddress: string = '';
  destinationAddress: string = '';
  intermediateStops: string[] = [];
  selectedVehicleType: string = 'standard';
  petTransport: boolean = false;
  babyTransport: boolean = false;

  // =======================
  // NEW: scheduling fields
  // =======================
  scheduleType: ScheduleType = 'NOW';

  // Presets for "IN"
  readonly schedulePresets: Array<{ label: string; minutes: number }> = [
    { label: '15m', minutes: 15 },
    { label: '30m', minutes: 30 },
    { label: '40m', minutes: 40 },
    { label: '1h', minutes: 60 },
    { label: '2h', minutes: 120 },
    { label: '2h 45m', minutes: 165 },
    { label: '3h', minutes: 180 },
    { label: '4h', minutes: 240 },
    { label: '5h', minutes: 300 },
  ];

  selectedDelayMinutes: number | null = 40; // default when user switches to "IN"
  customScheduleLocal: string = ''; // for <input type="datetime-local">

  minScheduleLocal: string = '';
  maxScheduleLocal: string = '';

  scheduleError: string = '';

  toggleOrderingForm(): void {
    this.toggleForm.emit();
  }

  addStop(): void {
    this.intermediateStops.push('');
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['prefilledStartLocation']) {
      if (this.prefilledStartLocation === null) this.pickupAddress = '';
      else if (this.prefilledStartLocation) this.pickupAddress = this.prefilledStartLocation;
    }

    if (changes['prefilledEndLocation']) {
      if (this.prefilledEndLocation === null) this.destinationAddress = '';
      else if (this.prefilledEndLocation) this.destinationAddress = this.prefilledEndLocation;
    }

    // NEW: when opening the form, refresh min/max bounds
    if (changes['showOrderingForm']?.currentValue === true) {
      this.updateScheduleBounds();
      this.scheduleError = '';
    }
  }

  trackByIndex(index: number, item: any): number {
    return index;
  }

  removeStop(index: number): void {
    this.intermediateStops.splice(index, 1);
  }

  // =======================
  // NEW: scheduling methods
  // =======================
  setScheduleType(type: ScheduleType): void {
    this.scheduleType = type;
    this.scheduleError = '';
    this.updateScheduleBounds();

    if (type === 'IN' && this.selectedDelayMinutes == null) {
      this.selectedDelayMinutes = 40;
    }
  }

  selectDelay(minutes: number): void {
    this.scheduleType = 'IN';
    this.selectedDelayMinutes = minutes;
    this.scheduleError = '';
    this.updateScheduleBounds();
  }

  private updateScheduleBounds(): void {
    const now = new Date();
    const max = new Date(now.getTime() + 5 * 60 * 60 * 1000);

    // "datetime-local" expects: YYYY-MM-DDTHH:mm
    this.minScheduleLocal = this.toLocalInputValue(new Date(now.getTime() + 1 * 60 * 1000)); // +1min
    this.maxScheduleLocal = this.toLocalInputValue(max);

    // If custom value exists but is out of bounds, clear error will handle on resolve
  }

  /**
   * Returns undefined for NOW (meaning: order immediately),
   * otherwise returns a LocalDateTime-like string "YYYY-MM-DDTHH:mm:ss"
   * that your backend can parse as LocalDateTime.
   */
  private resolveScheduledTime(): string | null {
    this.scheduleError = '';

    const now = new Date();
    const max = new Date(now.getTime() + 5 * 60 * 60 * 1000);

    // NOW -> return "now" as LocalDateTime string
    if (this.scheduleType === 'NOW') {
      return this.toLocalDateTimeString(now); // "YYYY-MM-DDTHH:mm:ss"
    }

    let chosen: Date | null = null;

    if (this.scheduleType === 'IN') {
      const m = this.selectedDelayMinutes ?? 0;
      if (m <= 0) {
        this.scheduleError = 'Pick a delay greater than 0 minutes.';
        return null;
      }
      chosen = new Date(now.getTime() + m * 60 * 1000);
    }

    if (!chosen || isNaN(chosen.getTime())) {
      this.scheduleError = 'Invalid time selection.';
      return null;
    }

    // If you're scheduling (IN/AT), it must be strictly in the future:
    if (chosen.getTime() <= now.getTime()) {
      this.scheduleError = 'Time must be in the future.';
      return null;
    }

    if (chosen.getTime() > max.getTime()) {
      this.scheduleError = 'You can schedule only up to 5 hours ahead.';
      return null;
    }

    return this.toLocalDateTimeString(chosen);
  }

  private pad2(n: number): string {
    return String(n).padStart(2, '0');
  }

  private toLocalInputValue(d: Date): string {
    const y = d.getFullYear();
    const mo = this.pad2(d.getMonth() + 1);
    const da = this.pad2(d.getDate());
    const h = this.pad2(d.getHours());
    const mi = this.pad2(d.getMinutes());
    return `${y}-${mo}-${da}T${h}:${mi}`;
  }

  private toLocalDateTimeString(d: Date): string {
    const y = d.getFullYear();
    const mo = this.pad2(d.getMonth() + 1);
    const da = this.pad2(d.getDate());
    const h = this.pad2(d.getHours());
    const mi = this.pad2(d.getMinutes());
    const s = this.pad2(d.getSeconds());
    return `${y}-${mo}-${da}T${h}:${mi}:${s}`;
  }

  orderRide(): void {
    if (!this.pickupAddress.trim() || !this.destinationAddress.trim()) {
      return;
    }

    const scheduledTime = this.resolveScheduledTime();
    if (this.scheduleError) {
      // keep local error (doesn't overwrite parent error unless you want it to)
      return;
    }
    if(!scheduledTime) {
      this.scheduleError = 'Failed to resolve scheduled time.';
      return;
    }

    const rideData: RideOrderData = {
      pickupAddress: this.pickupAddress.trim(),
      destinationAddress: this.destinationAddress.trim(),
      intermediateStops: this.intermediateStops.filter(stop => stop.trim() !== ''),
      vehicleType: this.selectedVehicleType.toUpperCase(),
      petTransport: this.petTransport,
      babyTransport: this.babyTransport,
      scheduledTime: scheduledTime
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

    // NEW reset scheduling
    this.scheduleType = 'NOW';
    this.selectedDelayMinutes = 40;
    this.customScheduleLocal = '';
    this.scheduleError = '';
    this.updateScheduleBounds();
  }

  setOrderingResult(result: RideResponse | null): void {
    this.orderingResult = result;
    this.isOrdering = false;
  }

  setError(error: string): void {
    this.orderingError = error;
    this.isOrdering = false;
  }
}
