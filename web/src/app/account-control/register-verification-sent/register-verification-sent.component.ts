import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
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

  constructor(
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.email = params['email'] || 'your email';
    });
  }

  resendEmail() {
    if (this.resendCooldown > 0 || this.isLoading || !this.email) return;

    this.isLoading = true;

    this.authService.resendActivation(this.email).subscribe({
      next: () => {
        console.log('Verification email resent successfully');
        this.isLoading = false;
        this.startCooldown();
      },
      error: (err) => {
        console.error('Failed to resend email', err);
        this.isLoading = false;
      }
    });
  }

  private startCooldown() {
    this.resendCooldown = 60;
    const interval = setInterval(() => {
      this.resendCooldown--;
      this.cdr.detectChanges();

      if (this.resendCooldown <= 0) {
        clearInterval(interval);
      }
    }, 1000);
  }
}
