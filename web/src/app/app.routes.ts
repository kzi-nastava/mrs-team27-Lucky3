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
import { HomePage } from './pages/home.page/home.page';
import { authGuard } from './infrastructure/auth/auth.guard';
import { roleGuard } from './infrastructure/auth/role.guard';
import { ActiveRidePage } from './pages/driver/active-ride/active-ride.page';
import { ActivationSuccessComponent } from './account-control/activation-success/activation-success.component';

export const routes: Routes = [
  // --- PUBLIC ROUTES (No Guards) ---
  {
    path: '',
    component: HomePage,
    pathMatch: 'full'
  },
  {
    path: 'home',
    component: HomePage
  },
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: 'register',
    component: RegisterComponent
  },
  {
    path: 'forgot-password',
    component: ForgotPasswordComponent
  },
  {
    path: 'reset-password-sent',
    component: ResetPasswordSentComponent
  },
  {
    path: 'reset-password',
    component: ResetPasswordComponent
  },
  {
    path: 'reset-password/:token',
    component: ResetPasswordComponent
  },
  {
    path: 'reset-password-success',
    component: ResetPasswordSuccessComponent
  },
  {
    path: 'register-verification-sent',
    component: RegisterVerificationSentComponent
  },
  {
    path: 'activate/:token',
    component: ActivationSuccessComponent
  },
  {
    path: 'activate',
    component: ActivationSuccessComponent
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

  // --- DRIVER ROUTES ---
  {
    path: 'driver/dashboard',
    component: DashboardPage,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['DRIVER'] }
  },
  {
    path: 'driver/ride/:id/active',
    component: ActiveRidePage,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['DRIVER'] }
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
    canActivate: [authGuard, roleGuard],
    data: { roles: ['DRIVER'] }
  },
  {
    path: 'driver/profile',
    component: DriverProfilePage,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['DRIVER'] }
  },

  // --- DEFAULT ROUTE ---
  {
    path: '**',
    redirectTo: '',
    pathMatch: 'full'
  }
];