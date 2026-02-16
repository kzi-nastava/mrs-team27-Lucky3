import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ReviewService, ReviewTokenData, ReviewRequest } from '../../infrastructure/rest/review.service';
import { AuthService } from '../../infrastructure/auth/auth.service';

@Component({
  selector: 'app-review-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './review.page.html',
  styleUrl: './review.page.css'
})
export class ReviewPage implements OnInit {
  token: string | null = null;
  rideId: number | null = null;
  tokenData: ReviewTokenData | null = null;
  isAuthenticatedMode = false;
  
  // Form data
  driverRating = 0;
  vehicleRating = 0;
  comment = '';
  
  // UI state
  isLoading = true;
  isSubmitting = false;
  error: string | null = null;
  success = false;
  tokenExpired = false;
  tokenInvalid = false;
  
  // Hover states for star ratings
  driverHover = 0;
  vehicleHover = 0;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private reviewService: ReviewService,
    private cdr: ChangeDetectorRef,
    private authService: AuthService,
    private location: Location
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token');
    const rideIdParam = this.route.snapshot.queryParamMap.get('rideId');
    
    // Authenticated mode: logged-in passenger with rideId
    if (rideIdParam && this.authService.isLoggedIn()) {
      this.rideId = Number(rideIdParam);
      this.isAuthenticatedMode = true;
      this.isLoading = false;
      this.cdr.detectChanges();
      return;
    }

    if (!this.token) {
      this.tokenInvalid = true;
      this.isLoading = false;
      this.cdr.detectChanges();
      return;
    }

    this.validateToken();
  }

  private validateToken(): void {
    if (!this.token) return;
    
    this.reviewService.validateReviewToken(this.token).subscribe({
      next: (data: ReviewTokenData) => {
        this.tokenData = data;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        this.isLoading = false;
        if (err.status === 410) {
          this.tokenExpired = true;
        } else {
          this.tokenInvalid = true;
        }
        this.cdr.detectChanges();
      }
    });
  }

  setDriverRating(rating: number): void {
    this.driverRating = rating;
  }

  setVehicleRating(rating: number): void {
    this.vehicleRating = rating;
  }

  get canSubmit(): boolean {
    return this.driverRating > 0 && this.vehicleRating > 0 && !this.isSubmitting;
  }

  submitReview(): void {
    if (!this.canSubmit) return;

    this.isSubmitting = true;
    this.error = null;

    if (this.isAuthenticatedMode && this.rideId) {
      const request: ReviewRequest = {
        rideId: this.rideId,
        driverRating: this.driverRating,
        vehicleRating: this.vehicleRating,
        comment: this.comment.trim() || undefined
      };
      this.reviewService.submitReview(request).subscribe({
        next: () => {
          this.isSubmitting = false;
          this.success = true;
          this.cdr.detectChanges();
        },
        error: (err: any) => {
          this.isSubmitting = false;
          if (err.status === 409) {
            this.error = 'You have already submitted a review for this ride.';
          } else {
            this.error = err.error?.message || 'Failed to submit review. Please try again.';
          }
          this.cdr.detectChanges();
        }
      });
    } else if (this.token) {
      const request: ReviewRequest = {
        token: this.token,
        driverRating: this.driverRating,
        vehicleRating: this.vehicleRating,
        comment: this.comment.trim() || undefined
      };
      this.reviewService.submitReviewWithToken(request).subscribe({
        next: () => {
          this.isSubmitting = false;
          this.success = true;
          this.cdr.detectChanges();
        },
        error: (err: any) => {
          this.isSubmitting = false;
          if (err.status === 410) {
            this.tokenExpired = true;
          } else if (err.status === 409) {
            this.error = 'You have already submitted a review for this ride.';
          } else {
            this.error = err.error?.message || 'Failed to submit review. Please try again.';
          }
          this.cdr.detectChanges();
        }
      });
    }
  }

  goHome(): void {
    if (this.authService.isLoggedIn()) {
      const role = this.authService.getRole();
      if (role === 'DRIVER') {
        this.router.navigate(['/driver/dashboard']);
      } else if (role === 'PASSENGER') {
        this.router.navigate(['/passenger/home']);
      } else {
        this.router.navigate(['/']);
      }
    } else {
      this.router.navigate(['/']);
    }
  }

  goBack(): void {
    this.location.back();
  }
}
