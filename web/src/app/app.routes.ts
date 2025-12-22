import { Routes } from '@angular/router';
import { DriverOverviewPage } from './pages/driver/driver-overview/driver-overview.page';
import { RideDetails } from './pages/driver/ride-details/ride-details';
import { DashboardPage } from './pages/driver/dashboard/dashboard.page';
import { LoginComponent } from './account-control/login/login.component';
import { RegisterComponent } from './account-control/register/register.component';
import { ForgotPasswordComponent } from './account-control/forgot-password/forgot-password.component';
import { ResetPasswordSentComponent } from './account-control/reset-password-sent/reset-password-sent.component';
import { RegisterVerificationSentComponent } from './account-control/register-verification-sent/register-verification-sent.component';

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
    path: 'register-verification-sent',
    component: RegisterVerificationSentComponent
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
    path: '',
    redirectTo: 'driver/dashboard',
    pathMatch: 'full'
  }
];