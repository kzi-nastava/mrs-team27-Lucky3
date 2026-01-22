import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize, take } from 'rxjs';
import { AuthService } from '../../infrastructure/auth/auth.service';

@Component({
  selector: 'app-activation-success',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './activation-success.component.html',
  styles: []
})
export class ActivationSuccessComponent implements OnInit, OnDestroy {
  isLoading = true;
  success = false;
  error = '';
  countdown = 10;

  private countdownInterval: any;
  private readonly pendingEmailKey = 'pendingActivationEmail';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const tokenFromParam = this.route.snapshot.paramMap.get('token');
    const tokenFromQuery = this.route.snapshot.queryParamMap.get('token');
    const token = (tokenFromParam || tokenFromQuery || '').trim();

    if (!token) {
      this.isLoading = false;
      this.success = false;
      this.error = 'Activation token is missing.';
      this.cdr.markForCheck();
      return;
    }

    this.authService
      .activateAccount(token)
      .pipe(
        take(1),
        finalize(() => {
          this.isLoading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: () => {
          this.success = true;
          try {
            localStorage.removeItem(this.pendingEmailKey);
          } catch {
            // ignore storage failures
          }
          this.startCountdown();
        },
        error: (err) => {
          console.error('Activation failed', err);
          this.success = false;
          this.error = 'Activation failed or the link has expired.';
          this.cdr.markForCheck();
        }
      });
  }

  ngOnDestroy(): void {
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
    }
  }

  goToLoginNow(): void {
    this.router.navigate(['/login']);
  }

  private startCountdown(): void {
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
    }

    this.countdown = 10;
    this.countdownInterval = setInterval(() => {
      this.countdown--;
      this.cdr.markForCheck();

      if (this.countdown <= 0) {
        clearInterval(this.countdownInterval);
        this.router.navigate(['/login']);
      }
    }, 1000);
  }
}
