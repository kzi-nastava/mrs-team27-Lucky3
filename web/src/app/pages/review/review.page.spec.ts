import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ReviewPage } from './review.page';
import { ReviewService, ReviewTokenData, ReviewRequest, ReviewResponse } from '../../infrastructure/rest/review.service';
import { AuthService } from '../../infrastructure/auth/auth.service';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Location } from '@angular/common';
import { By } from '@angular/platform-browser';
import { of, throwError, Subject } from 'rxjs';

// ── CSS selectors for querying DOM elements ──────────────────────
const SEL = {
  DRIVER_STARS: '[data-testid="driver-stars"]',
  VEHICLE_STARS: '[data-testid="vehicle-stars"]',
  SUBMIT_BTN: '[data-testid="submit-button"]',
  COMMENT_INPUT: '[data-testid="comment-input"]',
  REVIEW_FORM: '[data-testid="review-form"]',
  DRIVER_RATING_SECTION: '[data-testid="driver-rating-section"]',
  VEHICLE_RATING_SECTION: '[data-testid="vehicle-rating-section"]',
  VALIDATION_MSG: '[data-testid="validation-message"]',
  ERROR_MSG: '[data-testid="error-message"]',
  LOADING_SPINNER: '.animate-spin',
  SUBMIT_SPINNER: '[data-testid="submit-button"] .animate-spin',
  ERROR_TEXT: '.text-red-400',
  RATING_FEEDBACK: '.text-yellow-500.text-xs',
  CLICK_TO_RATE: '.text-gray-500.text-xs.text-center',
} as const;

// ── CSS classes for classList assertions ──────────────────────────
const CSS = {
  STAR_ACTIVE: 'text-yellow-500',
  STAR_INACTIVE: 'text-gray-600',
  BTN_DISABLED_BG: 'bg-gray-700',
  BTN_DISABLED_CURSOR: 'cursor-not-allowed',
  BTN_ACTIVE_BG: 'bg-yellow-500',
} as const;

describe('ReviewPage', () => {
  let component: ReviewPage;
  let fixture: ComponentFixture<ReviewPage>;
  let reviewServiceSpy: jasmine.SpyObj<ReviewService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let locationSpy: jasmine.SpyObj<Location>;

  // Dummy data for token validation.
  const mockTokenData: ReviewTokenData = {
    valid: true,
    rideId: 1,
    passengerId: 2,
    driverId: 3,
    driverName: 'John Driver',
    pickupAddress: '123 Start St',
    dropoffAddress: '456 End Ave'
  };

  // Dummy data for a successful submission response.
  const mockReviewResponse: ReviewResponse = {
    id: 1,
    rideId: 1,
    passengerId: 2,
    driverRating: 5,
    vehicleRating: 4,
    comment: 'Great ride!',
    createdAt: '2026-02-14T12:00:00'
  };

  /**
   * Shared setup: compiles the component with the given query params.
   * Pass a token for the email-link flow, or a rideId for the authenticated flow.
   */
  async function createComponent(token: string | null = 'valid-test-token', rideId: string | null = null) {
    const params: Record<string, string> = {};
    if (token) params['token'] = token;
    if (rideId) params['rideId'] = rideId;

    await TestBed.configureTestingModule({
      imports: [ReviewPage, FormsModule],
      providers: [
        { provide: ReviewService, useValue: reviewServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: Location, useValue: locationSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap(params)
            }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ReviewPage);
    component = fixture.componentInstance;
  }

  /** Hits the Nth driver star in the DOM (1-based) */
  function clickDriverStar(n: number): void {
    const driverSection = fixture.debugElement.query(By.css(SEL.DRIVER_STARS));
    const starButtons = driverSection.queryAll(By.css('button'));
    starButtons[n - 1].nativeElement.click();
    fixture.detectChanges();
  }

  /** Hits the Nth vehicle star in the DOM (1-based) */
  function clickVehicleStar(n: number): void {
    const vehicleSection = fixture.debugElement.query(By.css(SEL.VEHICLE_STARS));
    const starButtons = vehicleSection.queryAll(By.css('button'));
    starButtons[n - 1].nativeElement.click();
    fixture.detectChanges();
  }

  beforeEach(() => {
    reviewServiceSpy = jasmine.createSpyObj('ReviewService', ['validateReviewToken', 'submitReviewWithToken', 'submitReview']);
    authServiceSpy = jasmine.createSpyObj('AuthService', ['isLoggedIn', 'getRole']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    locationSpy = jasmine.createSpyObj('Location', ['back']);

    reviewServiceSpy.validateReviewToken.and.returnValue(of(mockTokenData));
    reviewServiceSpy.submitReviewWithToken.and.returnValue(of(mockReviewResponse));
    reviewServiceSpy.submitReview.and.returnValue(of(mockReviewResponse));
    authServiceSpy.isLoggedIn.and.returnValue(false);
  });

  // ═══════════════════════════════════════════════════════════════
  //  Initialization Tests
  // ═══════════════════════════════════════════════════════════════

  describe('Initialization', () => {
    it('should create the component', async () => {
      await createComponent();
      fixture.detectChanges();
      expect(component).toBeTruthy();
    });

    it('should call validateReviewToken on ngOnInit when token is present', async () => {
      await createComponent('valid-test-token');
      fixture.detectChanges();
      expect(reviewServiceSpy.validateReviewToken).toHaveBeenCalledWith('valid-test-token');
    });

    it('should set tokenData after successful validation', async () => {
      await createComponent('valid-test-token');
      fixture.detectChanges();
      expect(component.tokenData).toEqual(mockTokenData);
      expect(component.isLoading).toBeFalse();
    });

    it('should set tokenInvalid when no token in query params', async () => {
      await createComponent(null);
      fixture.detectChanges();
      expect(component.tokenInvalid).toBeTrue();
      expect(component.isLoading).toBeFalse();
      expect(reviewServiceSpy.validateReviewToken).not.toHaveBeenCalled();
    });

    it('should set tokenExpired when validation returns 410', async () => {
      reviewServiceSpy.validateReviewToken.and.returnValue(
        throwError(() => ({ status: 410 }))
      );
      await createComponent('expired-token');
      fixture.detectChanges();
      expect(component.tokenExpired).toBeTrue();
      expect(component.isLoading).toBeFalse();
    });

    it('should set tokenInvalid when validation returns non-410 error', async () => {
      reviewServiceSpy.validateReviewToken.and.returnValue(
        throwError(() => ({ status: 400 }))
      );
      await createComponent('bad-token');
      fixture.detectChanges();
      expect(component.tokenInvalid).toBeTrue();
      expect(component.isLoading).toBeFalse();
    });

    it('should display loading spinner while isLoading is true', async () => {
      // Use a Subject so the observable never completes immediately
      const subject = new Subject<ReviewTokenData>();
      reviewServiceSpy.validateReviewToken.and.returnValue(subject.asObservable());
      await createComponent('valid-test-token');
      fixture.detectChanges();
      // Component is still loading because the observable hasn't emitted
      expect(component.isLoading).toBeTrue();
      const loadingEl = fixture.debugElement.query(By.css(SEL.LOADING_SPINNER));
      expect(loadingEl).toBeTruthy();
    });

    it('should display "Invalid Link" when tokenInvalid is true', async () => {
      await createComponent(null);
      fixture.detectChanges();
      const heading = fixture.debugElement.query(By.css('h1'));
      expect(heading.nativeElement.textContent).toContain('Invalid Link');
    });

    it('should display "Link Expired" when tokenExpired is true', async () => {
      reviewServiceSpy.validateReviewToken.and.returnValue(
        throwError(() => ({ status: 410 }))
      );
      await createComponent('expired-token');
      fixture.detectChanges();
      const heading = fixture.debugElement.query(By.css('h1'));
      expect(heading.nativeElement.textContent).toContain('Link Expired');
    });
  });

  // ═══════════════════════════════════════════════════════════════
  //  Rating Interaction Tests
  // ═══════════════════════════════════════════════════════════════

  describe('Rating Interaction', () => {
    beforeEach(async () => {
      await createComponent('valid-test-token');
      fixture.detectChanges();
    });

    it('should have driverRating and vehicleRating initialized to 0', () => {
      expect(component.driverRating).toBe(0);
      expect(component.vehicleRating).toBe(0);
    });

    it('should set driverRating when a driver star button is clicked', () => {
      const driverStarSection = fixture.debugElement.query(By.css(SEL.DRIVER_STARS));
      const starButtons = driverStarSection.queryAll(By.css('button'));
      expect(starButtons.length).toBe(5);

      starButtons[2].nativeElement.click(); // 3rd star
      fixture.detectChanges();
      expect(component.driverRating).toBe(3);
    });

    it('should set vehicleRating when a vehicle star button is clicked', () => {
      const vehicleStarSection = fixture.debugElement.query(By.css(SEL.VEHICLE_STARS));
      const starButtons = vehicleStarSection.queryAll(By.css('button'));
      expect(starButtons.length).toBe(5);

      starButtons[4].nativeElement.click(); // 5th star
      fixture.detectChanges();
      expect(component.vehicleRating).toBe(5);
    });

    it('should change driverRating when clicking a different star', () => {
      clickDriverStar(4);
      expect(component.driverRating).toBe(4);

      clickDriverStar(2);
      expect(component.driverRating).toBe(2);
    });

    it('should apply yellow color class to selected driver stars', () => {
      clickDriverStar(3);
      const driverStarSection = fixture.debugElement.query(By.css(SEL.DRIVER_STARS));
      const stars = driverStarSection.queryAll(By.css('svg'));

      // First 3 stars should be yellow, last 2 should be gray
      expect(stars[0].nativeElement.classList.contains(CSS.STAR_ACTIVE)).toBeTrue();
      expect(stars[1].nativeElement.classList.contains(CSS.STAR_ACTIVE)).toBeTrue();
      expect(stars[2].nativeElement.classList.contains(CSS.STAR_ACTIVE)).toBeTrue();
      expect(stars[3].nativeElement.classList.contains(CSS.STAR_INACTIVE)).toBeTrue();
      expect(stars[4].nativeElement.classList.contains(CSS.STAR_INACTIVE)).toBeTrue();
    });

    it('should display "X of 5 stars" text after rating', () => {
      clickDriverStar(4);
      const ratingText = fixture.debugElement.queryAll(By.css(SEL.RATING_FEEDBACK));
      expect(ratingText.length).toBeGreaterThan(0);
      expect(ratingText[0].nativeElement.textContent).toContain('4 of 5 stars');
    });

    it('should display "Click to rate" when rating is 0', () => {
      fixture.detectChanges();
      const clickToRate = fixture.debugElement.queryAll(By.css(SEL.CLICK_TO_RATE));
      const clickToRateTexts = clickToRate.filter(
        el => el.nativeElement.textContent.includes('Click to rate')
      );
      expect(clickToRateTexts.length).toBe(2); // one for driver, one for vehicle
    });
  });

  // ═══════════════════════════════════════════════════════════════
  //  Validation (Negative) Tests - Submit button disabled
  // ═══════════════════════════════════════════════════════════════

  describe('Validation - Submit Disabled', () => {
    beforeEach(async () => {
      await createComponent('valid-test-token');
      fixture.detectChanges();
    });

    it('should have canSubmit return false when both ratings are 0', () => {
      // Ratings default to 0 after init — no stars clicked
      expect(component.canSubmit).toBeFalse();
    });

    it('should have canSubmit return false when only driverRating is set', () => {
      clickDriverStar(5);
      // vehicleRating still 0
      expect(component.canSubmit).toBeFalse();
    });

    it('should have canSubmit return false when only vehicleRating is set', () => {
      clickVehicleStar(3);
      // driverRating still 0
      expect(component.canSubmit).toBeFalse();
    });

    it('should disable submit button when ratings are 0', () => {
      // Ratings default to 0 — no clicks needed
      fixture.detectChanges();
      const submitBtn = fixture.debugElement.query(By.css(SEL.SUBMIT_BTN));
      expect(submitBtn).toBeTruthy();
      expect(submitBtn.nativeElement.disabled).toBeTrue();
    });

    it('should apply disabled styling (bg-gray-700) when canSubmit is false', () => {
      // Ratings default to 0 — no clicks needed
      fixture.detectChanges();
      const submitBtn = fixture.debugElement.query(By.css(SEL.SUBMIT_BTN));
      expect(submitBtn.nativeElement.classList.contains(CSS.BTN_DISABLED_BG)).toBeTrue();
      expect(submitBtn.nativeElement.classList.contains(CSS.BTN_DISABLED_CURSOR)).toBeTrue();
    });

    it('should show validation message when submit is disabled', () => {
      // Ratings default to 0 — no clicks needed
      component.isSubmitting = false;
      fixture.detectChanges();
      const msg = fixture.debugElement.queryAll(By.css('p')).find(
        p => p.nativeElement.textContent.includes('Please rate both the driver and vehicle')
      );
      expect(msg).toBeTruthy();
    });

    it('should not call submitReviewWithToken when form is invalid', () => {
      // Ratings default to 0 — no clicks needed
      component.submitReview();
      expect(reviewServiceSpy.submitReviewWithToken).not.toHaveBeenCalled();
    });

    it('should have canSubmit return false while isSubmitting is true', () => {
      clickDriverStar(5);
      clickVehicleStar(5);
      component.isSubmitting = true;
      expect(component.canSubmit).toBeFalse();
    });
  });

  // ═══════════════════════════════════════════════════════════════
  //  Happy Path - Successful Submission
  // ═══════════════════════════════════════════════════════════════

  describe('Submission - Happy Path', () => {
    beforeEach(async () => {
      await createComponent('valid-test-token');
      fixture.detectChanges();
    });

    it('should enable submit button when both ratings are set', () => {
      clickDriverStar(4);
      clickVehicleStar(3);
      const submitBtn = fixture.debugElement.query(By.css(SEL.SUBMIT_BTN));
      expect(submitBtn.nativeElement.disabled).toBeFalse();
    });

    it('should apply active styling (bg-yellow-500) when canSubmit is true', () => {
      clickDriverStar(4);
      clickVehicleStar(3);
      const submitBtn = fixture.debugElement.query(By.css(SEL.SUBMIT_BTN));
      expect(submitBtn.nativeElement.classList.contains(CSS.BTN_ACTIVE_BG)).toBeTrue();
    });

    it('should call submitReviewWithToken with correct arguments on submit', () => {
      clickDriverStar(5);
      clickVehicleStar(4);
      component.comment = 'Great ride!';
      component.submitReview();

      expect(reviewServiceSpy.submitReviewWithToken).toHaveBeenCalledOnceWith({
        token: 'valid-test-token',
        driverRating: 5,
        vehicleRating: 4,
        comment: 'Great ride!'
      } as ReviewRequest);
    });

    it('should trim comment whitespace before submitting', () => {
      clickDriverStar(5);
      clickVehicleStar(4);
      component.comment = '  Nice driver  ';
      component.submitReview();

      expect(reviewServiceSpy.submitReviewWithToken).toHaveBeenCalledOnceWith(
        jasmine.objectContaining({ comment: 'Nice driver' })
      );
    });

    it('should send undefined comment when comment is empty', () => {
      clickDriverStar(5);
      clickVehicleStar(4);
      component.comment = '';
      component.submitReview();

      expect(reviewServiceSpy.submitReviewWithToken).toHaveBeenCalledOnceWith(
        jasmine.objectContaining({ comment: undefined })
      );
    });

    it('should set success to true after successful submission', () => {
      clickDriverStar(5);
      clickVehicleStar(4);
      component.submitReview();
      expect(component.success).toBeTrue();
      expect(component.isSubmitting).toBeFalse();
    });

    it('should display "Thank You!" after successful submission', () => {
      clickDriverStar(5);
      clickVehicleStar(4);
      component.submitReview();
      fixture.detectChanges();
      const heading = fixture.debugElement.query(By.css('h1'));
      expect(heading.nativeElement.textContent).toContain('Thank You!');
    });

    it('should hide the review form after successful submission', () => {
      clickDriverStar(5);
      clickVehicleStar(4);
      component.submitReview();
      fixture.detectChanges();
      const textarea = fixture.debugElement.query(By.css('textarea'));
      expect(textarea).toBeNull();
    });

    it('should submit review via button click in the DOM', () => {
      clickDriverStar(5);
      clickVehicleStar(4);
      component.comment = 'Excellent';
      fixture.detectChanges();

      const submitBtn = fixture.debugElement.query(By.css(SEL.SUBMIT_BTN));
      submitBtn.nativeElement.click();
      fixture.detectChanges();

      expect(reviewServiceSpy.submitReviewWithToken).toHaveBeenCalledOnceWith(
        jasmine.objectContaining({
          driverRating: 5,
          vehicleRating: 4,
          comment: 'Excellent'
        })
      );
      expect(component.success).toBeTrue();
    });
  });

  // ═══════════════════════════════════════════════════════════════
  //  Submission Error Handling (Negative)
  // ═══════════════════════════════════════════════════════════════

  describe('Submission - Error Handling', () => {
    beforeEach(async () => {
      await createComponent('valid-test-token');
      fixture.detectChanges();
      clickDriverStar(5);
      clickVehicleStar(4);
    });

    it('should set tokenExpired when submission returns 410', () => {
      reviewServiceSpy.submitReviewWithToken.and.returnValue(
        throwError(() => ({ status: 410 }))
      );
      component.submitReview();
      expect(component.tokenExpired).toBeTrue();
      expect(component.isSubmitting).toBeFalse();
    });

    it('should set duplicate review error when submission returns 409', () => {
      reviewServiceSpy.submitReviewWithToken.and.returnValue(
        throwError(() => ({ status: 409 }))
      );
      component.submitReview();
      expect(component.error).toBe('You have already submitted a review for this ride.');
      expect(component.isSubmitting).toBeFalse();
    });

    it('should display error message from server when submission fails', () => {
      reviewServiceSpy.submitReviewWithToken.and.returnValue(
        throwError(() => ({ status: 500, error: { message: 'Server error occurred' } }))
      );
      component.submitReview();
      expect(component.error).toBe('Server error occurred');
      expect(component.isSubmitting).toBeFalse();
    });

    it('should display fallback error message when server provides no message', () => {
      reviewServiceSpy.submitReviewWithToken.and.returnValue(
        throwError(() => ({ status: 500 }))
      );
      component.submitReview();
      expect(component.error).toBe('Failed to submit review. Please try again.');
    });

    it('should render error message in the DOM', () => {
      reviewServiceSpy.submitReviewWithToken.and.returnValue(
        throwError(() => ({ status: 409 }))
      );
      component.submitReview();
      fixture.detectChanges();
      const errorEl = fixture.debugElement.query(By.css(SEL.ERROR_TEXT));
      expect(errorEl).toBeTruthy();
      expect(errorEl.nativeElement.textContent).toContain('already submitted');
    });

    it('should display "Link Expired" view when submission returns 410', () => {
      reviewServiceSpy.submitReviewWithToken.and.returnValue(
        throwError(() => ({ status: 410 }))
      );
      component.submitReview();
      fixture.detectChanges();
      const heading = fixture.debugElement.query(By.css('h1'));
      expect(heading.nativeElement.textContent).toContain('Link Expired');
    });
  });

  // ═══════════════════════════════════════════════════════════════
  //  Comment Textarea Tests
  // ═══════════════════════════════════════════════════════════════

  describe('Comment Textarea', () => {
    beforeEach(async () => {
      await createComponent('valid-test-token');
      fixture.detectChanges();
    });

    it('should have a textarea with maxlength 500', () => {
      const textarea = fixture.debugElement.query(By.css('textarea'));
      expect(textarea).toBeTruthy();
      expect(textarea.nativeElement.getAttribute('maxlength')).toBe('500');
    });

    it('should display character count', () => {
      component.comment = 'Hello';
      fixture.detectChanges();
      const charCount = fixture.debugElement.queryAll(By.css('p')).find(
        p => p.nativeElement.textContent.includes('/500')
      );
      expect(charCount).toBeTruthy();
      expect(charCount!.nativeElement.textContent).toContain('5/500');
    });

    it('should bind textarea value to component comment via ngModel', fakeAsync(() => {
      const textarea = fixture.debugElement.query(By.css('textarea')).nativeElement;
      textarea.value = 'Test comment';
      textarea.dispatchEvent(new Event('input'));
      tick();
      fixture.detectChanges();
      expect(component.comment).toBe('Test comment');
    }));
  });

  // ═══════════════════════════════════════════════════════════════
  //  Navigation (goHome) Tests
  // ═══════════════════════════════════════════════════════════════

  describe('Navigation - goHome', () => {
    beforeEach(async () => {
      await createComponent('valid-test-token');
      fixture.detectChanges();
    });

    it('should navigate to /driver/dashboard when user is a logged-in DRIVER', () => {
      authServiceSpy.isLoggedIn.and.returnValue(true);
      authServiceSpy.getRole.and.returnValue('DRIVER');
      component.goHome();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/driver/dashboard']);
    });

    it('should navigate to /passenger/home when user is a logged-in PASSENGER', () => {
      authServiceSpy.isLoggedIn.and.returnValue(true);
      authServiceSpy.getRole.and.returnValue('PASSENGER');
      component.goHome();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/passenger/home']);
    });

    it('should navigate to / when user is logged in with unknown role', () => {
      authServiceSpy.isLoggedIn.and.returnValue(true);
      authServiceSpy.getRole.and.returnValue('ADMIN');
      component.goHome();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
    });

    it('should navigate to / when user is not logged in', () => {
      authServiceSpy.isLoggedIn.and.returnValue(false);
      component.goHome();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
    });

    it('should invoke goHome when "Go to Home" button is clicked on success view', () => {
      clickDriverStar(5);
      clickVehicleStar(4);
      component.submitReview();
      fixture.detectChanges();

      authServiceSpy.isLoggedIn.and.returnValue(false);
      const homeBtn = fixture.debugElement.queryAll(By.css('button')).find(
        btn => btn.nativeElement.textContent.includes('Go to Home')
      );
      expect(homeBtn).toBeTruthy();
      homeBtn!.nativeElement.click();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
    });
  });

  // ═══════════════════════════════════════════════════════════════
  //  Full End-to-End Flow via DOM Interaction
  // ═══════════════════════════════════════════════════════════════

  describe('Full Flow - DOM Interaction', () => {
    it('should complete a full review flow: rate driver, rate vehicle, type comment, submit', fakeAsync(async () => {
      await createComponent('valid-test-token');
      fixture.detectChanges();

      // Click 4th driver star
      clickDriverStar(4);
      expect(component.driverRating).toBe(4);

      // Click 5th vehicle star
      clickVehicleStar(5);
      expect(component.vehicleRating).toBe(5);

      // Type comment
      const textarea = fixture.debugElement.query(By.css(SEL.COMMENT_INPUT)).nativeElement;
      textarea.value = 'Wonderful ride, very smooth!';
      textarea.dispatchEvent(new Event('input'));
      tick();
      fixture.detectChanges();

      // Click submit
      const submitBtn = fixture.debugElement.query(By.css(SEL.SUBMIT_BTN));
      expect(submitBtn.nativeElement.disabled).toBeFalse();
      submitBtn.nativeElement.click();
      fixture.detectChanges();

      // Verify service called with correct args
      expect(reviewServiceSpy.submitReviewWithToken).toHaveBeenCalledOnceWith(
        jasmine.objectContaining({
          token: 'valid-test-token',
          driverRating: 4,
          vehicleRating: 5,
          comment: 'Wonderful ride, very smooth!'
        })
      );

      // Verify success state
      expect(component.success).toBeTrue();
      const heading = fixture.debugElement.query(By.css('h1'));
      expect(heading.nativeElement.textContent).toContain('Thank You!');
    }));

    it('should show "Rate Your Ride" heading when form is displayed', async () => {
      await createComponent('valid-test-token');
      fixture.detectChanges();
      const heading = fixture.debugElement.query(By.css('h1'));
      expect(heading.nativeElement.textContent).toContain('Rate Your Ride');
    });
  });

  // ═══════════════════════════════════════════════════════════════
  //  Authenticated Mode (logged-in passenger submits via rideId)
  // ═══════════════════════════════════════════════════════════════

  describe('Authenticated Mode', () => {
    beforeEach(async () => {
      authServiceSpy.isLoggedIn.and.returnValue(true);
      await createComponent(null, '42');
      fixture.detectChanges();
    });

    it('should enter authenticated mode when rideId param is present and user is logged in', () => {
      expect(component.isAuthenticatedMode).toBeTrue();
      expect(component.rideId).toBe(42);
      expect(component.isLoading).toBeFalse();
    });

    it('should NOT call validateReviewToken in authenticated mode', () => {
      expect(reviewServiceSpy.validateReviewToken).not.toHaveBeenCalled();
    });

    it('should display the review form in authenticated mode', () => {
      const form = fixture.debugElement.query(By.css(SEL.REVIEW_FORM));
      expect(form).toBeTruthy();
    });

    it('should display "Rate Your Ride" heading in authenticated mode', () => {
      const heading = fixture.debugElement.query(By.css('h1'));
      expect(heading.nativeElement.textContent).toContain('Rate Your Ride');
    });

    it('should call submitReview (not submitReviewWithToken) on submission', () => {
      clickDriverStar(4);
      clickVehicleStar(3);
      component.comment = 'Great driver!';
      component.submitReview();

      expect(reviewServiceSpy.submitReview).toHaveBeenCalledOnceWith({
        rideId: 42,
        driverRating: 4,
        vehicleRating: 3,
        comment: 'Great driver!'
      } as ReviewRequest);
      expect(reviewServiceSpy.submitReviewWithToken).not.toHaveBeenCalled();
    });

    it('should send undefined comment when comment is empty in authenticated mode', () => {
      clickDriverStar(5);
      clickVehicleStar(4);
      component.comment = '';
      component.submitReview();

      expect(reviewServiceSpy.submitReview).toHaveBeenCalledOnceWith(
        jasmine.objectContaining({ comment: undefined })
      );
    });

    it('should trim whitespace from comment before submitting in authenticated mode', () => {
      clickDriverStar(5);
      clickVehicleStar(4);
      component.comment = '  Nice ride  ';
      component.submitReview();

      expect(reviewServiceSpy.submitReview).toHaveBeenCalledOnceWith(
        jasmine.objectContaining({ comment: 'Nice ride' })
      );
    });

    it('should set success after authenticated submission', () => {
      clickDriverStar(5);
      clickVehicleStar(4);
      component.submitReview();
      expect(component.success).toBeTrue();
      expect(component.isSubmitting).toBeFalse();
    });

    it('should show "Thank You!" after successful authenticated submission', () => {
      clickDriverStar(5);
      clickVehicleStar(4);
      component.submitReview();
      fixture.detectChanges();
      const heading = fixture.debugElement.query(By.css('h1'));
      expect(heading.nativeElement.textContent).toContain('Thank You!');
    });

    it('should handle 409 duplicate review in authenticated mode', () => {
      reviewServiceSpy.submitReview.and.returnValue(
        throwError(() => ({ status: 409 }))
      );
      clickDriverStar(5);
      clickVehicleStar(4);
      component.submitReview();
      expect(component.error).toBe('You have already submitted a review for this ride.');
      expect(component.isSubmitting).toBeFalse();
    });

    it('should handle server error in authenticated mode', () => {
      reviewServiceSpy.submitReview.and.returnValue(
        throwError(() => ({ status: 500, error: { message: 'Internal error' } }))
      );
      clickDriverStar(5);
      clickVehicleStar(4);
      component.submitReview();
      expect(component.error).toBe('Internal error');
    });

    it('should show fallback error when server provides no message in authenticated mode', () => {
      reviewServiceSpy.submitReview.and.returnValue(
        throwError(() => ({ status: 500 }))
      );
      clickDriverStar(5);
      clickVehicleStar(4);
      component.submitReview();
      expect(component.error).toBe('Failed to submit review. Please try again.');
    });

    it('should complete full authenticated flow: rate, comment, submit, success', fakeAsync(() => {
      clickDriverStar(3);
      clickVehicleStar(5);

      const textarea = fixture.debugElement.query(By.css(SEL.COMMENT_INPUT)).nativeElement;
      textarea.value = 'Smooth ride!';
      textarea.dispatchEvent(new Event('input'));
      tick();
      fixture.detectChanges();

      const submitBtn = fixture.debugElement.query(By.css(SEL.SUBMIT_BTN));
      submitBtn.nativeElement.click();
      fixture.detectChanges();

      expect(reviewServiceSpy.submitReview).toHaveBeenCalledOnceWith(
        jasmine.objectContaining({
          rideId: 42,
          driverRating: 3,
          vehicleRating: 5,
          comment: 'Smooth ride!'
        })
      );
      expect(component.success).toBeTrue();
      const heading = fixture.debugElement.query(By.css('h1'));
      expect(heading.nativeElement.textContent).toContain('Thank You!');
    }));
  });

  // ═══════════════════════════════════════════════════════════════
  //  Star Hover Effects
  // ═══════════════════════════════════════════════════════════════

  describe('Star Hover Effects', () => {
    beforeEach(async () => {
      await createComponent('valid-test-token');
      fixture.detectChanges();
    });

    it('should highlight driver stars up to the hovered star', () => {
      const driverSection = fixture.debugElement.query(By.css(SEL.DRIVER_STARS));
      const starButtons = driverSection.queryAll(By.css('button'));

      starButtons[2].nativeElement.dispatchEvent(new Event('mouseenter'));
      fixture.detectChanges();

      const stars = driverSection.queryAll(By.css('svg'));
      expect(stars[0].nativeElement.classList.contains(CSS.STAR_ACTIVE)).toBeTrue();
      expect(stars[1].nativeElement.classList.contains(CSS.STAR_ACTIVE)).toBeTrue();
      expect(stars[2].nativeElement.classList.contains(CSS.STAR_ACTIVE)).toBeTrue();
      expect(stars[3].nativeElement.classList.contains(CSS.STAR_INACTIVE)).toBeTrue();
      expect(stars[4].nativeElement.classList.contains(CSS.STAR_INACTIVE)).toBeTrue();
    });

    it('should remove driver hover highlight on mouseleave', () => {
      const driverSection = fixture.debugElement.query(By.css(SEL.DRIVER_STARS));
      const starButtons = driverSection.queryAll(By.css('button'));

      starButtons[2].nativeElement.dispatchEvent(new Event('mouseenter'));
      fixture.detectChanges();
      starButtons[2].nativeElement.dispatchEvent(new Event('mouseleave'));
      fixture.detectChanges();

      // No rating set, all should be gray
      const stars = driverSection.queryAll(By.css('svg'));
      expect(stars[0].nativeElement.classList.contains(CSS.STAR_INACTIVE)).toBeTrue();
      expect(stars[2].nativeElement.classList.contains(CSS.STAR_INACTIVE)).toBeTrue();
    });

    it('should highlight all 5 vehicle stars when hovering the last one', () => {
      const vehicleSection = fixture.debugElement.query(By.css(SEL.VEHICLE_STARS));
      const starButtons = vehicleSection.queryAll(By.css('button'));

      starButtons[4].nativeElement.dispatchEvent(new Event('mouseenter'));
      fixture.detectChanges();

      const stars = vehicleSection.queryAll(By.css('svg'));
      for (let i = 0; i < 5; i++) {
        expect(stars[i].nativeElement.classList.contains(CSS.STAR_ACTIVE)).toBeTrue();
      }
    });

    it('should override existing rating with hover highlight', () => {
      // Set rating to 2, then hover over star 4
      clickDriverStar(2);
      const driverSection = fixture.debugElement.query(By.css(SEL.DRIVER_STARS));
      const starButtons = driverSection.queryAll(By.css('button'));

      starButtons[3].nativeElement.dispatchEvent(new Event('mouseenter'));
      fixture.detectChanges();

      const stars = driverSection.queryAll(By.css('svg'));
      // Hover (4) overrides rating (2), so first 4 stars should be yellow
      expect(stars[0].nativeElement.classList.contains(CSS.STAR_ACTIVE)).toBeTrue();
      expect(stars[3].nativeElement.classList.contains(CSS.STAR_ACTIVE)).toBeTrue();
      expect(stars[4].nativeElement.classList.contains(CSS.STAR_INACTIVE)).toBeTrue();
    });

    it('should revert to rating highlight after mouseleave', () => {
      clickDriverStar(2);
      const driverSection = fixture.debugElement.query(By.css(SEL.DRIVER_STARS));
      const starButtons = driverSection.queryAll(By.css('button'));

      starButtons[3].nativeElement.dispatchEvent(new Event('mouseenter'));
      fixture.detectChanges();
      starButtons[3].nativeElement.dispatchEvent(new Event('mouseleave'));
      fixture.detectChanges();

      // Should revert to rating of 2
      const stars = driverSection.queryAll(By.css('svg'));
      expect(stars[0].nativeElement.classList.contains(CSS.STAR_ACTIVE)).toBeTrue();
      expect(stars[1].nativeElement.classList.contains(CSS.STAR_ACTIVE)).toBeTrue();
      expect(stars[2].nativeElement.classList.contains(CSS.STAR_INACTIVE)).toBeTrue();
    });
  });

  // ═══════════════════════════════════════════════════════════════
  //  Cancel / Go Back
  // ═══════════════════════════════════════════════════════════════

  describe('Cancel Button', () => {
    beforeEach(async () => {
      await createComponent('valid-test-token');
      fixture.detectChanges();
    });

    it('should call location.back() when Cancel button is clicked', () => {
      const cancelBtn = fixture.debugElement.queryAll(By.css('button')).find(
        btn => btn.nativeElement.textContent.includes('Cancel')
      );
      expect(cancelBtn).toBeTruthy();
      cancelBtn!.nativeElement.click();
      expect(locationSpy.back).toHaveBeenCalled();
    });

    it('should call goBack() which delegates to location.back()', () => {
      component.goBack();
      expect(locationSpy.back).toHaveBeenCalled();
    });
  });

  // ═══════════════════════════════════════════════════════════════
  //  Submitting State UI
  // ═══════════════════════════════════════════════════════════════

  describe('Submitting State', () => {
    beforeEach(async () => {
      await createComponent('valid-test-token');
      fixture.detectChanges();
    });

    it('should show "Submitting..." text while request is in flight', () => {
      clickDriverStar(5);
      clickVehicleStar(4);

      const subject = new Subject<ReviewResponse>();
      reviewServiceSpy.submitReviewWithToken.and.returnValue(subject.asObservable());

      component.submitReview();
      fixture.detectChanges();

      const submitBtn = fixture.debugElement.query(By.css(SEL.SUBMIT_BTN));
      expect(submitBtn.nativeElement.textContent).toContain('Submitting...');
      expect(component.isSubmitting).toBeTrue();
    });

    it('should disable submit button while request is in flight', () => {
      clickDriverStar(5);
      clickVehicleStar(4);

      const subject = new Subject<ReviewResponse>();
      reviewServiceSpy.submitReviewWithToken.and.returnValue(subject.asObservable());

      component.submitReview();
      fixture.detectChanges();

      const submitBtn = fixture.debugElement.query(By.css(SEL.SUBMIT_BTN));
      expect(submitBtn.nativeElement.disabled).toBeTrue();
    });

    it('should show spinner animation while submitting', () => {
      clickDriverStar(5);
      clickVehicleStar(4);

      const subject = new Subject<ReviewResponse>();
      reviewServiceSpy.submitReviewWithToken.and.returnValue(subject.asObservable());

      component.submitReview();
      fixture.detectChanges();

      const spinner = fixture.debugElement.query(By.css(SEL.SUBMIT_SPINNER));
      expect(spinner).toBeTruthy();
    });

    it('should hide validation message while submitting', () => {
      clickDriverStar(5);
      clickVehicleStar(4);

      const subject = new Subject<ReviewResponse>();
      reviewServiceSpy.submitReviewWithToken.and.returnValue(subject.asObservable());

      component.submitReview();
      fixture.detectChanges();

      const validationMsg = fixture.debugElement.query(By.css(SEL.VALIDATION_MSG));
      expect(validationMsg).toBeNull();
    });
  });

  // ═══════════════════════════════════════════════════════════════
  //  Form Structure (data-testid anchors)
  // ═══════════════════════════════════════════════════════════════

  describe('Form Structure', () => {
    beforeEach(async () => {
      await createComponent('valid-test-token');
      fixture.detectChanges();
    });

    it('should render the review-form container', () => {
      expect(fixture.debugElement.query(By.css(SEL.REVIEW_FORM))).toBeTruthy();
    });

    it('should render driver-rating-section with label "Rate the Driver"', () => {
      const section = fixture.debugElement.query(By.css(SEL.DRIVER_RATING_SECTION));
      expect(section).toBeTruthy();
      expect(section.nativeElement.textContent).toContain('Rate the Driver');
    });

    it('should render vehicle-rating-section with label "Rate the Vehicle"', () => {
      const section = fixture.debugElement.query(By.css(SEL.VEHICLE_RATING_SECTION));
      expect(section).toBeTruthy();
      expect(section.nativeElement.textContent).toContain('Rate the Vehicle');
    });

    it('should render exactly 5 driver star buttons', () => {
      const stars = fixture.debugElement.query(By.css(SEL.DRIVER_STARS));
      expect(stars.queryAll(By.css('button')).length).toBe(5);
    });

    it('should render exactly 5 vehicle star buttons', () => {
      const stars = fixture.debugElement.query(By.css(SEL.VEHICLE_STARS));
      expect(stars.queryAll(By.css('button')).length).toBe(5);
    });

    it('should render comment textarea with placeholder "Share your experience..."', () => {
      const textarea = fixture.debugElement.query(By.css(SEL.COMMENT_INPUT));
      expect(textarea).toBeTruthy();
      expect(textarea.nativeElement.getAttribute('placeholder')).toBe('Share your experience...');
    });

    it('should render submit button with text "Submit Review"', () => {
      const btn = fixture.debugElement.query(By.css(SEL.SUBMIT_BTN));
      expect(btn).toBeTruthy();
      expect(btn.nativeElement.textContent).toContain('Submit Review');
    });

    it('should show validation message when form is incomplete', () => {
      const msg = fixture.debugElement.query(By.css(SEL.VALIDATION_MSG));
      expect(msg).toBeTruthy();
      expect(msg.nativeElement.textContent).toContain('Please rate both the driver and vehicle');
    });

    it('should hide validation message once both ratings are set', () => {
      clickDriverStar(3);
      clickVehicleStar(2);
      const msg = fixture.debugElement.query(By.css(SEL.VALIDATION_MSG));
      expect(msg).toBeNull();
    });

    it('should show error message container when error is set', () => {
      component.error = 'Something went wrong';
      fixture.detectChanges();
      const errorEl = fixture.debugElement.query(By.css(SEL.ERROR_MSG));
      expect(errorEl).toBeTruthy();
      expect(errorEl.nativeElement.textContent).toContain('Something went wrong');
    });

    it('should not show error message container when no error', () => {
      const errorEl = fixture.debugElement.query(By.css(SEL.ERROR_MSG));
      expect(errorEl).toBeNull();
    });
  });
});
