import { Routes } from '@angular/router';
import { DriverOverviewPage } from './pages/driver/driver-overview/driver-overview.page';
import { RideDetails } from './pages/driver/ride-details/ride-details';
import { DriverProfilePage } from './pages/profile.page/driver-profile/driver-profile.page';
import { UserProfilePage } from './pages/profile.page/user-profile/user-profile.page';
import { AdminProfile } from './pages/profile.page/admin-profile/admin-profile';
import { DashboardPage } from './pages/driver/dashboard/dashboard.page';
import { LoginComponent } from './account-control/login/login.component';
import { RegisterComponent } from './account-control/register/register.component';
import { ForgotPasswordComponent } from './account-control/forgot-password/forgot-password.component';
import { ResetPasswordSentComponent } from './account-control/reset-password-sent/reset-password-sent.component';
import { RegisterVerificationSentComponent } from './account-control/register-verification-sent/register-verification-sent.component';
import { ResetPasswordComponent } from './account-control/reset-password/reset-password.component';
import { ResetPasswordSuccessComponent } from './account-control/reset-password-success/reset-password-success.component';
import { PassengerHomePage } from './pages/passenger/home/passenger-home.page';
import { AdminDashboardPage } from './pages/admin/dashboard/admin-dashboard.page';
import { AdminRequestsPage } from './pages/admin/requests/admin-requests.page';
import { HomePage } from './pages/home.page/home.page';
import { authGuard } from './infrastructure/auth/auth.guard';
import { roleGuard } from './infrastructure/auth/role.guard';
import { noAuthGuard } from './infrastructure/auth/no-auth.guard';
import { ActiveRidePage } from './shared/active-ride/active-ride.page';
import { AdminDriversPage } from './pages/admin/drivers/admin-drivers.page';
import { CreateDriverComponent } from './account-control/create-driver/create-driver.component';
import {DriverSetPasswordComponent} from "./account-control/driver-set-password/driver-set-password.component";
import { ActivationSuccessComponent } from './account-control/activation-success/activation-success.component';
import { resetPasswordTokenGuard } from './infrastructure/auth/reset-password-token.guard';
import { registerVerificationGuard, resetPasswordSentGuard, resetPasswordSuccessGuard } from './infrastructure/auth/flow.guards';
import { activationGuard } from './infrastructure/auth/activation.guard';
import { rideAccessGuard } from './infrastructure/auth/ride-access.guard';
import { passengerRideAccessGuard } from './infrastructure/auth/passenger-ride-access.guard';
import { driverRideHistoryGuard } from './infrastructure/auth/driver-ride-history.guard';
import {RideHistoryComponent} from "./pages/passenger/ride-history/ride-history.component";
import { FavoritePageComponent } from './pages/passenger/favorite-page/favorite-page.component';
import { ReviewPage } from './pages/review/review.page';
import { reviewGuard } from './infrastructure/auth/review.guard';
import { AdminPanicPage } from './pages/admin/panic/admin-panic.page';
import { NotFoundPage } from './pages/not-found/not-found.page';
import { SupportPage } from './shared/support/support.page';
import { AdminSupportPage } from './pages/admin/support/admin-support.page';

export const routes: Routes = [
  // --- GUEST-ONLY ROUTES (redirect to dashboard if logged in) ---
  {
    path: '',
    component: HomePage,
    pathMatch: 'full',
    canActivate: [noAuthGuard]
  },
  {
    path: 'home',
    component: HomePage,
    canActivate: [noAuthGuard]
  },
  {
    path: 'login',
    component: LoginComponent,
    canActivate: [noAuthGuard]
  },
  {
    path: 'register',
    component: RegisterComponent,
    canActivate: [noAuthGuard]
  },
  {
    path: 'forgot-password',
    component: ForgotPasswordComponent,
    canActivate: [noAuthGuard]
  },
  {
    path: 'reset-password-sent',
    component: ResetPasswordSentComponent,
    canActivate: [noAuthGuard, resetPasswordSentGuard]
  },
  {
    path: 'reset-password',
    component: ResetPasswordComponent,
    canActivate: [noAuthGuard, resetPasswordTokenGuard]
  },
  {
    path: 'reset-password/:token',
    component: ResetPasswordComponent,
    canActivate: [noAuthGuard, resetPasswordTokenGuard]
  },
  {
    path: 'reset-password-success',
    component: ResetPasswordSuccessComponent,
    canActivate: [noAuthGuard, resetPasswordSuccessGuard]
  },
  {
    path: 'register-verification-sent',
    component: RegisterVerificationSentComponent,
    canActivate: [noAuthGuard, registerVerificationGuard]
  },
  {
    path: 'driver/set-password',
    component: DriverSetPasswordComponent,
    canActivate: [noAuthGuard]
  },
  {
    path: 'activate/:token',
    component: ActivationSuccessComponent,
    canActivate: [noAuthGuard, activationGuard]
  },
  {
    path: 'activate',
    component: ActivationSuccessComponent,
    canActivate: [noAuthGuard, activationGuard]
  },

  // --- REVIEW PAGE (PUBLIC - TOKEN BASED) ---
  {
    path: 'review',
    component: ReviewPage,
    canActivate: [reviewGuard]
  },

  // --- PASSENGER ROUTES ---
  {
    path: 'passenger/home',
    component: PassengerHomePage,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['PASSENGER'] }
  },
  {
    path: 'passenger/profile',
    component: UserProfilePage,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['PASSENGER'] }
  },
  {
    path: 'passenger/ride-history',
    component: RideHistoryComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['PASSENGER'] }
  },
  {
    path: 'passenger/favorites',
    component: FavoritePageComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['PASSENGER'] }
  },
  {
    path: 'passenger/ride/:id',
    component: ActiveRidePage,
    canActivate: [passengerRideAccessGuard]
  },
  {
    path: 'passenger/support',
    component: SupportPage,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['PASSENGER'] }
  },

  // --- ADMIN ROUTES ---
  {
    path: 'admin/profile',
    component: AdminProfile,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'admin/dashboard',
    component: AdminDashboardPage,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'admin/requests',
    component: AdminRequestsPage,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'admin/drivers',
    component: AdminDriversPage,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'admin/create-driver',
    component: CreateDriverComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'admin/panic',
    component: AdminPanicPage,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'admin/support',
    component: AdminSupportPage,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] }
  },

  // --- DRIVER ROUTES ---
  {
    path: 'driver/dashboard',
    component: DashboardPage,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['DRIVER'] }
  },
  {
    path: 'driver/ride/:id',
    component: ActiveRidePage,
    canActivate: [rideAccessGuard]
  },
  {
    path: 'driver/overview',
    component: DriverOverviewPage,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['DRIVER'] }
  },
  {
    path: 'driver/overview/ride/:id',
    component: RideDetails,
    canActivate: [driverRideHistoryGuard]
  },
  {
    path: 'driver/profile',
    component: DriverProfilePage,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['DRIVER'] }
  },
  {
    path: 'driver/support',
    component: SupportPage,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['DRIVER'] }
  },

  // --- 404 NOT FOUND ROUTE ---
  {
    path: '404',
    component: NotFoundPage
  },
  // --- WILDCARD ROUTE (must be last) ---
  {
    path: '**',
    component: NotFoundPage
  }
];