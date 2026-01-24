import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { finalize, take } from 'rxjs';
import { AuthService } from '../../infrastructure/auth/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './forgot-password.component.html',
  styles: []
})
export class ForgotPasswordComponent {
  forgotPasswordForm: FormGroup;
  loading = false;
  error = '';
  notice = '';

  private readonly pendingResetEmailKey = 'pendingResetEmail';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {
    this.forgotPasswordForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });

    this.route.queryParams.subscribe((params) => {
      const reason = (params['reason'] as string | undefined) || '';

      if (reason === 'missing-token') {
        this.notice = 'Reset link is missing a token. Please request a new link.';
      } else if (reason === 'invalid-token') {
        this.notice = 'This reset link is invalid. Please request a new link.';
      } else if (reason === 'expired-token') {
        this.notice = 'This reset link has expired. Please request a new link.';
      } else {
        this.notice = '';
      }

      this.cdr.markForCheck();
    });
  }

  onSubmit() {
    if (this.loading) return;

    if (this.forgotPasswordForm.invalid) {
      this.forgotPasswordForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.error = '';

    const email = (this.forgotPasswordForm.get('email')?.value || '').trim();

    this.authService
      .requestPasswordReset(email)
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
            localStorage.setItem(this.pendingResetEmailKey, email);
          } catch {
            // ignore storage failures
          }

          this.router.navigate(['/reset-password-sent'], { 
            state: { resetEmailSent: true } 
          });
        },
        error: (err) => {
          console.error(err);
          this.error = (err instanceof Error && err.message)
            ? err.message
            : 'Failed to send reset link. Please try again.';
          this.cdr.markForCheck();
        }
      });
  }

  get f() { return this.forgotPasswordForm.controls; }
}
