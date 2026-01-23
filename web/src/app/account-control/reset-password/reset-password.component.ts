import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { finalize, take } from 'rxjs';
import { AuthService } from '../../infrastructure/auth/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './reset-password.component.html',
  styles: []
})
export class ResetPasswordComponent {
  resetPasswordForm: FormGroup;
  loading = false;
  error = '';
  token = '';

  showNewPassword = false;
  showConfirmPassword = false;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private fb: FormBuilder,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {
    this.resetPasswordForm = this.fb.group({
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordMatchValidator });

    const tokenFromParam = this.route.snapshot.paramMap.get('token');
    const tokenFromQuery = this.route.snapshot.queryParamMap.get('token');
    this.token = (tokenFromParam || tokenFromQuery || '').trim();
  }

  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('newPassword')?.value;
    const confirmPassword = control.get('confirmPassword')?.value;

    if (!password || !confirmPassword) return null;
    return password !== confirmPassword ? { passwordMismatch: true } : null;
  }

  onSubmit() {
    if (this.loading) return;

    if (this.resetPasswordForm.invalid) {
      this.resetPasswordForm.markAllAsTouched();
      return;
    }

    if (!this.token) {
      this.error = 'Reset token is missing. Please open the link from your email again.';
      this.cdr.markForCheck();
      return;
    }

    this.loading = true;
    this.error = '';

    const newPassword = this.resetPasswordForm.get('newPassword')?.value;
    const confirmPassword = this.resetPasswordForm.get('confirmPassword')?.value;

    this.authService
      .resetPassword(this.token, newPassword, confirmPassword)
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
            localStorage.removeItem('pendingResetEmail');
          } catch {
            // ignore storage failures
          }

          this.router.navigate(['/reset-password-success']);
        },
        error: (err) => {
          console.error(err);
          this.error = (err instanceof Error && err.message)
            ? err.message
            : 'Failed to reset password. Please try again.';
          this.cdr.markForCheck();
        }
      });
  }

  toggleNewPasswordVisibility() {
    this.showNewPassword = !this.showNewPassword;
  }

  toggleConfirmPasswordVisibility() {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  get isTokenError(): boolean {
    return /reset link/.test((this.error || '').toLowerCase());
  }

  get f() { return this.resetPasswordForm.controls; }
}