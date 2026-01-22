import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { finalize, take } from 'rxjs';
import { AuthService } from '../../infrastructure/auth/auth.service';

@Component({
  selector: 'app-register-verification-sent',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './register-verification-sent.component.html',
  styles: []
})
export class RegisterVerificationSentComponent implements OnInit {
  email: string = '';
  resendCooldown = 0;
  isLoading = false;
  error = '';

  private readonly pendingEmailKey = 'pendingActivationEmail';

  constructor(
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      const emailFromQuery = params['email'] as string | undefined;
      const storedEmail = localStorage.getItem(this.pendingEmailKey) || '';

      this.email = (emailFromQuery || storedEmail || '').trim();
      if (emailFromQuery) {
        localStorage.setItem(this.pendingEmailKey, this.email);
      }

      if (!this.email) {
        this.error = 'Missing email address. Please go back and register again.';
      }
    });
  }

  resendEmail() {
    if (this.resendCooldown > 0 || this.isLoading) return;

    this.error = '';

    const email = (this.email || '').trim();
    const emailLooksValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
    if (!email || !emailLooksValid) {
      this.error = 'Please provide a valid email address.';
      this.cdr.markForCheck();
      return;
    }

    this.isLoading = true;

    this.authService.resendActivation(email)
      .pipe(
        take(1),
        finalize(() => {
          this.isLoading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: () => {
          this.startCooldown();
        },
        error: (err) => {
          console.error('Failed to resend email', err);
          this.error = 'Failed to resend verification email. Please try again.';
          this.cdr.markForCheck();
        }
      });
  }

  private startCooldown() {
    this.resendCooldown = 60;
    const interval = setInterval(() => {
      this.resendCooldown--;
      this.cdr.markForCheck();

      if (this.resendCooldown <= 0) {
        clearInterval(interval);
      }
    }, 1000);
  }
}
