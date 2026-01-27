import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ReviewService, ReviewTokenData, ReviewRequest } from '../../infrastructure/rest/review.service';

@Component({
  selector: 'app-review-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './review.page.html',
  styleUrl: './review.page.css'
})
export class ReviewPage implements OnInit {
  token: string | null = null;
  tokenData: ReviewTokenData | null = null;
  
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
    private reviewService: ReviewService
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token');
    
    if (!this.token) {
      this.tokenInvalid = true;
      this.isLoading = false;
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
      },
      error: (err: any) => {
        this.isLoading = false;
        if (err.status === 410) {
          this.tokenExpired = true;
        } else {
          this.tokenInvalid = true;
        }
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
    if (!this.canSubmit || !this.token) return;

    this.isSubmitting = true;
    this.error = null;

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
      }
    });
  }

  goHome(): void {
    this.router.navigate(['/']);
  }
}
