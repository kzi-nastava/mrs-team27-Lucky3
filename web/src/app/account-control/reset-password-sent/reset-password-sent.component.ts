import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { finalize, take } from 'rxjs';
import { AuthService } from '../../infrastructure/auth/auth.service';

@Component({
  selector: 'app-reset-password-sent',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './reset-password-sent.component.html',
  styles: []
})
export class ResetPasswordSentComponent implements OnInit {
  email = '';
  resendCooldown = 0;
  isLoading = false;
  error = '';
  notice = '';

  private readonly pendingResetEmailKey = 'pendingResetEmail';

  constructor(
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      const emailFromQuery = params['email'] as string | undefined;
      const storedEmail = localStorage.getItem(this.pendingResetEmailKey) || '';

      this.email = (emailFromQuery || storedEmail || '').trim();
      if (emailFromQuery) {
        localStorage.setItem(this.pendingResetEmailKey, this.email);
      }
    });
  }

  resendEmail() {
    if (this.resendCooldown > 0 || this.isLoading) return;

    this.error = '';
    this.notice = '';

    const email = (this.email || '').trim();
    const emailLooksValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
    if (!email || !emailLooksValid) {
      this.error = 'Please provide a valid email address.';
      this.cdr.markForCheck();
      return;
    }

    this.isLoading = true;

    this.authService
      .requestPasswordResetWithStatus(email)
      .pipe(
        take(1),
        finalize(() => {
          this.isLoading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (status) => {
          this.startCooldown();

          if (status === 'alreadySent') {
            this.notice = 'A reset link was already sent recently. Please check your inbox/spam, or wait a bit and try again.';
          } else {
            this.notice = 'We sent a new reset link. Please check your inbox/spam.';
          }
        },
        error: (err) => {
          console.error('Failed to resend email', err);
          this.error = (err instanceof Error && err.message)
            ? err.message
            : 'Failed to resend reset email. Please try again.';
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
