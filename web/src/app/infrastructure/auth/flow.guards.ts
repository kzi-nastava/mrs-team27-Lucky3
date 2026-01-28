import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';


// Ensures the user has just successfully registered before seeing the 'Check your email' page
export const registerVerificationGuard: CanActivateFn = () => {
  const router = inject(Router);
  const navigation = router.currentNavigation();

  // Check if the previous component passed { registered: true } in the state
  if (navigation?.extras.state?.['registered']) {
    return true;
  }

  // If accessed directly, redirect to 404
  return router.createUrlTree(['/404']);
};

// Ensures the user has just sent a reset link before seeing the confirmation page
export const resetPasswordSentGuard: CanActivateFn = () => {
  const router = inject(Router);
  const navigation = router.currentNavigation();

  if (navigation?.extras.state?.['resetEmailSent']) {
    return true;
  }

  return router.createUrlTree(['/404']);
};

// Ensures the user has just successfully reset their password before seeing the success page
export const resetPasswordSuccessGuard: CanActivateFn = () => {
  const router = inject(Router);
  const navigation = router.currentNavigation();

  if (navigation?.extras.state?.['passwordResetSuccess']) {
    return true;
  }

  return router.createUrlTree(['/404']);
};