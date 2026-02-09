import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-driver-set-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, HttpClientModule],
  templateUrl: './driver-set-password.component.html'
})
export class DriverSetPasswordComponent implements OnInit {
  passwordForm: FormGroup;
  token: string | null = null;
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.passwordForm = this.fb.group({
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  ngOnInit() {
    this.token = this.route.snapshot.queryParams['token'];
    if (!this.token) {
      this.errorMessage = 'No activation token found. Please check your email.';
    }
  }

  passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
    const password = group.get('password')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    return password === confirmPassword ? null : { mismatch: true };
  }

  onSubmit() {
    if (this.passwordForm.invalid || !this.token) return;

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    const request = { token: this.token, password: this.passwordForm.value.password, confirmPassword: this.passwordForm.value.confirmPassword };

    this.http.put('http://localhost:8081/api/auth/driver-activation/password', request)
      .subscribe({
        next: () => {
          this.successMessage = 'Password set successfully! You can now log in.';
          setTimeout(() => this.router.navigate(['/login']), 2000);  // Redirect to login
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Activation failed. Token may be invalid or expired.';
        }
      }).add(() => this.isLoading = false);
  }

  get password() { return this.passwordForm.get('password'); }
  get confirmPassword() { return this.passwordForm.get('confirmPassword'); }
}
