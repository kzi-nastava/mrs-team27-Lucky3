import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { finalize, take } from 'rxjs';
import { AuthService } from '../../infrastructure/auth/auth.service';
import { PassengerRegistrationRequest } from '../../infrastructure/auth/model/registration.model';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './register.component.html',
  styles: []
})
export class RegisterComponent {
  registerForm: FormGroup;
  loading = false;
  error = '';
  selectedFile: File | null = null; // store the uploaded image
  showPassword = false;
  showConfirmPassword = false;

  constructor(
    private router: Router,
    private fb: FormBuilder,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {
    this.registerForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required, Validators.pattern(/^\+?[\d\s-()]{10,}$/)]],
      address: ['', [Validators.required, Validators.minLength(5)]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password')?.value;
    const confirmPassword = control.get('confirmPassword')?.value;

    if (!password || !confirmPassword) return null;
    return password !== confirmPassword ? { passwordMismatch: true } : null;
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
    }
  }

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPasswordVisibility() {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  onSubmit() {
    if (this.loading) return;

    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.error = '';

    const registrationData: PassengerRegistrationRequest = {
      name: this.registerForm.get('firstName')?.value,
      surname: this.registerForm.get('lastName')?.value,
      email: this.registerForm.get('email')?.value,
      phoneNumber: this.registerForm.get('phone')?.value,
      address: this.registerForm.get('address')?.value,
      password: this.registerForm.get('password')?.value,
      confirmPassword: this.registerForm.get('confirmPassword')?.value
    };

    // Call Backend
    this.authService.register(registrationData, this.selectedFile || undefined)
      .pipe(
        take(1),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: () => {
          try {
            localStorage.setItem('pendingActivationEmail', (registrationData.email || '').trim());
          } catch {
            // ignore storage failures
          }
          // Navigate to success page
          this.router.navigate(['/register-verification-sent'], { 
            queryParams: { email: registrationData.email } 
          });
        },
        error: (err) => {
          console.error(err);
          if (err instanceof Error && err.message) {
            this.error = err.message;
            return;
          }

          if (err?.error && typeof err.error === 'string') {
            this.error = err.error;
          } else if (err?.error?.message) {
            this.error = err.error.message;
          } else {
            this.error = 'Registration failed. Please try again.';
          }

          this.cdr.markForCheck();
        }
      });
  }

  get f() { return this.registerForm.controls; }
}
