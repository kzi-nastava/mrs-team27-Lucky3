import { Routes } from '@angular/router';
import { DriverOverviewPage } from './pages/driver/driver-overview/driver-overview.page';
import { RideDetails } from './pages/driver/ride-details/ride-details';
import { DriverProfilePage } from './pages/profile.page/driver-profile/driver-profile.page';
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

export const routes: Routes = [
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
    path: 'reset-password-success',
    component: ResetPasswordSuccessComponent
  },
  {
    path: 'register-verification-sent',
    component: RegisterVerificationSentComponent
  },
  {
    path: 'passenger/home',
    component: PassengerHomePage
  },
  {
    path: 'admin/dashboard',
    component: AdminDashboardPage
  },
  {
    path: 'driver/dashboard',
    component: DashboardPage
  },
  {
    path: 'driver/overview',
    component: DriverOverviewPage
  },
  {
    path: 'driver/overview/ride/:id',
    component: RideDetails
  },
  {
    path: 'driver/profile',
    component: DriverProfilePage
  },
  {
    path: '',
    redirectTo: 'driver/dashboard',
    pathMatch: 'full'
  }
];