import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
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

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    this.driverForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required, Validators.pattern(/^[+]?[(]?[0-9]{3}[)]?[-\s.]?[0-9]{3}[-\s.]?[0-9]{4,6}$/)]],
      address: ['', [Validators.required, Validators.minLength(5)]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required],
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

    this.loading = true;
    this.error = '';

    const formData = new FormData();
    formData.append('driver', JSON.stringify(this.driverForm.value));
    if (this.selectedFile) {
      formData.append('photo', this.selectedFile);
    }

    // Call your service here
    console.log('Form submitted:', this.driverForm.value);
  }
}
