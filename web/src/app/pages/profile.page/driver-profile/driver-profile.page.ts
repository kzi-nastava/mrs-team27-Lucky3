import { Component, ViewChild, ChangeDetectorRef } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ChangeInformationRequest, DriverResponse, UserService } from '../../../infrastructure/rest/user.service';

@Component({
  selector: 'app-driver-profile.page',
  imports: [CommonModule, FormsModule],
  templateUrl: './driver-profile.page.html'
})
export class DriverProfilePage {
  
  showPersonalForm = false;
  showVehicleForm = false;

  // Personal form fields (used while editing)
  personalFormFullName = '';
  personalFormEmail = '';
  personalFormPhone = '';
  personalFormAddress = '';
  personalFormReason = '';

  // Vehicle form fields (used while editing)
  vehicleFormModel = '124';
  vehicleFormLicensePlate = '';
  vehicleFormYear: number | null = null;
  vehicleFormColor = '';
  vehicleFormCapacity: number | null = null;
  vehicleFormReason = '';

  // Simple success popup state
  showSuccessPopup = false;
  successMessage = '';

  driver: DriverResponse | null = null;
  request: ChangeInformationRequest | null = null;
  isLoading = true;
  errorMessage = '';

  constructor(private userService: UserService, private cdr: ChangeDetectorRef) {}


  ngOnInit(): void {
    this.loadDriverProfile();
  }

  loadDriverProfile(): void {
    this.userService.getCurrentDriver().subscribe({
      next: (driver: DriverResponse) => {
        this.driver = driver;
        this.isLoading = false;
        this.cdr.detectChanges();
        console.log('Driver loaded:', driver);
        console.log('Vehicle info:', driver.vehicle);
        console.log('Is active:', driver.isActive);
      },
      error: (error) => {
        console.error('Failed to load driver:', error);
        this.errorMessage = 'Failed to load driver data';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }
  // Template form references for resetting validation state
  @ViewChild('personalForm') personalForm?: NgForm;
  @ViewChild('vehicleForm') vehicleForm?: NgForm;

  openPersonalForm() {
    // Reset personal form fields from current profile data and clear reason
    this.personalFormFullName = this.driver ? `${this.driver.name} ${this.driver.surname}` : '';
    this.personalFormEmail = this.driver ? this.driver.email : '';
    this.personalFormPhone = this.driver ? this.driver.phoneNumber : '';
    this.personalFormAddress = this.driver ? this.driver.address : '';
    this.personalFormReason = '';
   
    this.vehicleFormModel = this.driver?.vehicle.model || '';
    this.vehicleFormLicensePlate = this.driver?.vehicle.licenseNumber || '';
    this.vehicleFormYear = 2018; // Placeholder as year is not in VehicleInformation
    this.vehicleFormColor = "black"; // Placeholder as color is not in VehicleInformation
    this.vehicleFormCapacity = this.driver?.vehicle.passengerSeats || null;

    // Reset validation state so previous errors disappear
    if (this.personalForm) {
      this.personalForm.resetForm({
        fullName: this.personalFormFullName,
        email: this.personalFormEmail,
        phone: this.personalFormPhone,
        address: this.personalFormAddress,
        reason: this.personalFormReason,
        
        vehicleModel: this.vehicleFormModel,
        licensePlate: this.vehicleFormLicensePlate,
        vehicleYear: this.vehicleFormYear,
        vehicleColor: this.vehicleFormColor,
        vehicleCapacity: this.vehicleFormCapacity,
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
    /*this.fullName = this.personalFormFullName;
    this.email = this.personalFormEmail;
    this.phone = this.personalFormPhone;
    this.address = this.personalFormAddress;*/

    this.closePersonalForm();
    this.successMessage = 'Your personal information change request has been sent successfully.';
    this.showSuccessPopup = true;
  }

  openVehicleForm() {
    // Reset vehicle form fields from current vehicle data and clear reason
    this.vehicleFormModel = this.driver?.vehicle.model || '';
    this.vehicleFormLicensePlate = this.driver?.vehicle.licenseNumber || '';
    this.vehicleFormYear = 2018; // Placeholder as year is not in VehicleInformation
    this.vehicleFormColor = "black"; // Placeholder as color is not in VehicleInformation
    this.vehicleFormCapacity = this.driver?.vehicle.passengerSeats || null;
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

  closeSuccessPopup() {
    this.showSuccessPopup = false;
  }
}
