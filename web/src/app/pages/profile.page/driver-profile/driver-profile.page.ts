import { Component, ViewChild } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-driver-profile.page',
  imports: [CommonModule, FormsModule],
  templateUrl: './driver-profile.page.html',
  styleUrl: './driver-profile.page.css',
})
export class DriverProfilePage {
  
  showPersonalForm = false;
  showVehicleForm = false;

  // Personal information form model fields (used by ngModel)
  fullName = 'Marko Jovanović';
  email = 'marko.jovanovic@example.com';
  phone = '+381641234567';
  address = 'Bulevar kralja Aleksandra 73, Beograd';
  reason = '';

  // Personal form fields (used while editing)
  personalFormFullName = '';
  personalFormEmail = '';
  personalFormPhone = '';
  personalFormAddress = '';
  personalFormReason = '';

  // Vehicle information form model fields (used by ngModel)
  vehicleModel = 'Škoda Octavia';
  licensePlate = 'BG 123-AB';
  vehicleYear: number | null = 2019;
  vehicleColor = 'Siva';
  vehicleCapacity: number | null = 4;
  vehicleReason = '';

  // Vehicle form fields (used while editing)
  vehicleFormModel = '';
  vehicleFormLicensePlate = '';
  vehicleFormYear: number | null = null;
  vehicleFormColor = '';
  vehicleFormCapacity: number | null = null;
  vehicleFormReason = '';

  // Simple success popup state
  showSuccessPopup = false;
  successMessage = '';

  // Template form references for resetting validation state
  @ViewChild('personalForm') personalForm?: NgForm;
  @ViewChild('vehicleForm') vehicleForm?: NgForm;

  openPersonalForm() {
    // Reset personal form fields from current profile data and clear reason
    this.personalFormFullName = this.fullName;
    this.personalFormEmail = this.email;
    this.personalFormPhone = this.phone;
    this.personalFormAddress = this.address;
    this.personalFormReason = '';

    // Reset validation state so previous errors disappear
    if (this.personalForm) {
      this.personalForm.resetForm({
        fullName: this.personalFormFullName,
        email: this.personalFormEmail,
        phone: this.personalFormPhone,
        address: this.personalFormAddress,
        reason: this.personalFormReason,
      });
    }

    this.showPersonalForm = true;
  }

  closePersonalForm() {
    this.showPersonalForm = false;
  }

  onPersonalSubmit(form: any) {
    if (form.invalid) {
      return;
    }
    // Persist form changes into the displayed profile data
    this.fullName = this.personalFormFullName;
    this.email = this.personalFormEmail;
    this.phone = this.personalFormPhone;
    this.address = this.personalFormAddress;

    this.closePersonalForm();
    this.successMessage = 'Your personal information change request has been sent successfully.';
    this.showSuccessPopup = true;
  }

  openVehicleForm() {
    // Reset vehicle form fields from current vehicle data and clear reason
    this.vehicleFormModel = this.vehicleModel;
    this.vehicleFormLicensePlate = this.licensePlate;
    this.vehicleFormYear = this.vehicleYear;
    this.vehicleFormColor = this.vehicleColor;
    this.vehicleFormCapacity = this.vehicleCapacity;
    this.vehicleFormReason = '';

    // Reset validation state so previous errors disappear
    if (this.vehicleForm) {
      this.vehicleForm.resetForm({
        vehicleModel: this.vehicleFormModel,
        licensePlate: this.vehicleFormLicensePlate,
        vehicleYear: this.vehicleFormYear,
        vehicleColor: this.vehicleFormColor,
        vehicleCapacity: this.vehicleFormCapacity,
        vehicleReason: this.vehicleFormReason,
      });
    }

    this.showVehicleForm = true;
  }

  closeVehicleForm() {
    this.showVehicleForm = false;
  }

  onVehicleSubmit(form: any) {
    if (form.invalid) {
      return;
    }
    // Persist form changes into the displayed vehicle data
    this.vehicleModel = this.vehicleFormModel;
    this.licensePlate = this.vehicleFormLicensePlate;
    this.vehicleYear = this.vehicleFormYear;
    this.vehicleColor = this.vehicleFormColor;
    this.vehicleCapacity = this.vehicleFormCapacity;

    this.closeVehicleForm();
    this.successMessage = 'Your vehicle information change request has been sent successfully.';
    this.showSuccessPopup = true;
  }

  closeSuccessPopup() {
    this.showSuccessPopup = false;
  }
}
