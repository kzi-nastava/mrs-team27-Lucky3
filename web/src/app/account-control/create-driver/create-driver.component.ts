import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';  // Add this
import { Router } from '@angular/router';             // Add this
import { RegularExpressionLiteralExpr } from '@angular/compiler';
import { DriverService } from '../../infrastructure/rest/driver.service';

export enum VehicleType {
  LUXURY = 'LUXURY',
  VAN = 'VAN',
  STANDARD = 'STANDARD'
}

@Component({
  selector: 'app-create-driver',
  standalone: true,  // Add this
  imports: [CommonModule, ReactiveFormsModule, RouterModule],  // Add this
  templateUrl: './create-driver.component.html'
})
export class CreateDriverComponent implements OnInit {
  driverForm!: FormGroup;
  loading = false;
  error = '';
  selectedFile: File | null = null;
  vehicleTypes = Object.values(VehicleType);

  constructor(
    private fb: FormBuilder,
    private driverService: DriverService,
    private router: Router     // Add if missing
  ) {}


  ngOnInit(): void {
    this.driverForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required, Validators.pattern(/^[+]?[(]?[0-9]{3}[)]?[-\s.]?[0-9]{3}[-\s.]?[0-9]{4,6}$/)]],
      address: ['', [Validators.required, Validators.minLength(5)]],

      vehicle: this.fb.group({
        model: ['', Validators.required],
        licensePlate: ['', Validators.required],
        year: ['', [Validators.required, Validators.min(1900), Validators.max(2026)]],
        vehicleType: ['', Validators.required],
        numberOfSeats: ['', [Validators.required, Validators.min(1), Validators.max(50)]],
        babyTransport: [false],
        petTransport: [false]
      })
    });
  }

  get f() {
    return this.driverForm.controls;
  }

  get v() {
    return (this.driverForm.get('vehicle') as FormGroup).controls;
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
    }
  }

  onSubmit(): void {
    if (this.driverForm.invalid) {
      Object.keys(this.driverForm.controls).forEach(key => {
        this.driverForm.get(key)?.markAsTouched();
      });
      const vehicleGroup = this.driverForm.get('vehicle') as FormGroup;
      Object.keys(vehicleGroup.controls).forEach(key => {
        vehicleGroup.get(key)?.markAsTouched();
      });
      return;
    }
    else{
        console.log('onSubmit called!');
    }

    this.loading = true;
    this.error = '';

    const payload = {
        name: this.driverForm.value.firstName,
        surname: this.driverForm.value.lastName,
        email: this.driverForm.value.email,
        address: this.driverForm.value.address,
        phone: this.driverForm.value.phone,
        vehicle: {
        model: this.driverForm.value.vehicle.model,
        vehicleType: this.driverForm.value.vehicle.vehicleType,
        licenseNumber: this.driverForm.value.vehicle.licensePlate,
        passengerSeats: this.driverForm.value.vehicle.numberOfSeats,
        babyTransport: this.driverForm.value.vehicle.babyTransport,
        petTransport: this.driverForm.value.vehicle.petTransport
        }
    };

    const formData = new FormData();
    formData.append('request', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
    if (this.selectedFile) {
        formData.append('profileImage', this.selectedFile);
    }

    this.driverService.createDriver(formData).subscribe({
        next: (response) => {
            console.log('Success:', response);
            this.loading = false;
            alert('Driver created successfully!');
            this.router.navigate(['/admin/drivers']);
        },
        error: (error) => {
            console.error('Error:', error);
            this.error = error.error?.message || 'Failed to create driver';
            this.loading = false;
        }
    });
  }
}
