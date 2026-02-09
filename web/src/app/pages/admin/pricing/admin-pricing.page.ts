import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PricingService, VehiclePriceResponse } from '../../../infrastructure/rest/pricing.service';

interface VehicleTypeDisplay {
  type: string;
  label: string;
  icon: string;
  color: string;
  bgColor: string;
}

@Component({
  selector: 'app-admin-pricing',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-pricing.page.html'
})
export class AdminPricingPage implements OnInit {

  vehicleTypes: VehicleTypeDisplay[] = [
    { type: 'STANDARD', label: 'Standard', icon: '🚗', color: 'text-blue-400', bgColor: 'bg-blue-500/10 border-blue-500/20' },
    { type: 'LUXURY', label: 'Luxury', icon: '✨', color: 'text-yellow-400', bgColor: 'bg-yellow-500/10 border-yellow-500/20' },
    { type: 'VAN', label: 'Van', icon: '🚐', color: 'text-green-400', bgColor: 'bg-green-500/10 border-green-500/20' }
  ];

  prices: Map<string, VehiclePriceResponse> = new Map();
  isLoading = true;
  errorMessage = '';

  // Edit modal state
  showEditModal = false;
  editingType: VehicleTypeDisplay | null = null;
  editBaseFare = 0;
  editPricePerKm = 0;
  isSaving = false;
  saveError = '';
  saveSuccess = false;

  constructor(
    private pricingService: PricingService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadPrices();
  }

  loadPrices(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.pricingService.getAllPrices().subscribe({
      next: (data) => {
        this.prices.clear();
        for (const p of data) {
          this.prices.set(p.vehicleType, p);
        }
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.errorMessage = `Failed to load pricing data (${err.status})`;
        this.isLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  getPrice(type: string): VehiclePriceResponse | undefined {
    return this.prices.get(type);
  }

  getBaseFare(type: string): number {
    return this.prices.get(type)?.baseFare ?? 0;
  }

  getPricePerKm(type: string): number {
    return this.prices.get(type)?.pricePerKm ?? 0;
  }

  getEstimatedFare(type: string): number {
    const price = this.prices.get(type);
    if (!price) return 0;
    // Example: 5km ride
    return price.baseFare + price.pricePerKm * 5;
  }

  openEditModal(vt: VehicleTypeDisplay): void {
    this.editingType = vt;
    const price = this.prices.get(vt.type);
    this.editBaseFare = price?.baseFare ?? 0;
    this.editPricePerKm = price?.pricePerKm ?? 0;
    this.saveError = '';
    this.saveSuccess = false;
    this.showEditModal = true;
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.editingType = null;
  }

  savePrice(): void {
    if (!this.editingType) return;

    this.isSaving = true;
    this.saveError = '';

    this.pricingService.updatePrice(
      this.editingType.type,
      this.editBaseFare,
      this.editPricePerKm
    ).subscribe({
      next: (updated) => {
        this.prices.set(updated.vehicleType, updated);
        this.isSaving = false;
        this.saveSuccess = true;
        this.cdr.markForCheck();

        // Auto-close after short delay
        setTimeout(() => {
          this.closeEditModal();
          this.saveSuccess = false;
          this.cdr.markForCheck();
        }, 1000);
      },
      error: (err) => {
        this.saveError = err.error?.message || `Failed to update price (${err.status})`;
        this.isSaving = false;
        this.cdr.markForCheck();
      }
    });
  }
}
