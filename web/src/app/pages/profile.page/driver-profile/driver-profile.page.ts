import { Component, ViewChild, ChangeDetectorRef } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ChangeInformationRequest, DriverResponse, UserService, VehicleInformation } from '../../../infrastructure/rest/user.service';

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
  vehicleFormType : string = 'standard';
  vehicleFormCapacity: number | null = null;
  babyTransport = false;
  petTransport = false;
  vehicleFormReason = '';
  selectedFile: File | null = null;

  // Simple success popup state
  showSuccessPopup = false;
  successMessage = '';

  driver: DriverResponse | null = null;
  request: ChangeInformationRequest | null = null;
  isLoading = true;
  errorMessage = '';

  constructor(private userService: UserService, private cdr: ChangeDetectorRef) {}

  createChangeRequest(): ChangeInformationRequest {
    // Split full name (e.g., "John Doe" -> name: "John", surname: "Doe")
    const nameParts = this.personalFormFullName.trim().split(' ');
    const name = nameParts[0] || '';
    const surname = nameParts.slice(1).join(' ') || '';

    return {
      name,
      surname,
      email: this.personalFormEmail,
      phone: this.personalFormPhone,
      address: this.personalFormAddress,
      vehicle: {
        model: this.vehicleFormModel,
        licenseNumber: this.vehicleFormLicensePlate,
        vehicleType: this.vehicleFormType.toUpperCase(),
        passengerSeats: this.vehicleFormCapacity || 0,
        babyTransport: this.babyTransport || false,
        petTransport: this.petTransport || false,
      }
    };
  }

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
        //console.log('Is active:', driver.isActive);
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
    console.log('Opening personal form');
    // Reset personal form fields from current profile data and clear reason
    this.personalFormFullName = this.driver ? `${this.driver.name} ${this.driver.surname}` : '';
    this.personalFormEmail = this.driver ? this.driver.email : '';
    this.personalFormPhone = this.driver ? this.driver.phoneNumber : '';
    this.personalFormAddress = this.driver ? this.driver.address : '';
    this.personalFormReason = '';
   
    this.vehicleFormModel = this.driver?.vehicle.model || '';
    this.vehicleFormLicensePlate = this.driver?.vehicle.licenseNumber || '';
    this.vehicleFormYear = 2018; // Placeholder as year is not in VehicleInformation
    //this.vehicleFormColor = "black"; // Placeholder as color is not in VehicleInformation
    this.vehicleFormCapacity = this.driver?.vehicle.passengerSeats || null;
    
    this.vehicleFormType = this.driver?.vehicle.vehicleType || 'standard';
    this.petTransport = this.driver?.vehicle.petTransport || false;
    this.babyTransport = this.driver?.vehicle.babyTransport || false;

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
        //vehicleColor: this.vehicleFormColor,
        vehicleCapacity: this.vehicleFormCapacity,
        vehicleReason: this.vehicleFormReason,
        babyTransport: this.babyTransport,
        petTransport: this.petTransport,
        VehicleType: this.vehicleFormType
      });
    }

    this.vehicleFormType = this.driver?.vehicle.vehicleType || 'standard';
    this.petTransport = this.driver?.vehicle.petTransport || false;
    this.babyTransport = this.driver?.vehicle.babyTransport || false;
    
    this.showPersonalForm = true;
  }

  closePersonalForm() {
    //console.log('Closing personal form');
    this.showPersonalForm = false;
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
    }
  }

  onPersonalSubmit(form: any) {
    if (form.invalid) {
      form.control.markAllAsTouched();
      return;
    }

    // Create request from form data (reuse your method)
    const changeRequest = this.createChangeRequest();
    //console.log('Submitting change request:', changeRequest);
    this.userService.updateCurrentDriverProfile(changeRequest, this.selectedFile || undefined)
      .subscribe({
        next: (updatedDriver: DriverResponse) => {
          this.closePersonalForm();
          this.successMessage = 'Profile updated successfully.';
          this.showSuccessPopup = true;
          this.cdr.detectChanges(); // or markForCheck()
        },
        error: (error) => {
          console.error('Update failed:', error);
          this.errorMessage = 'Failed to submit request. Please try again.';
        }
      });
  }

  closeSuccessPopup() {
    this.showSuccessPopup = false;
  }

  getUserImageUrl(driverId : number | undefined): string | null {
    return driverId !== undefined ? "http://localhost:8081/api/users/" + driverId + "/profile-image" : null;
  }
}
