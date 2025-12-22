import { Routes } from '@angular/router';
import { DriverOverviewPage } from './pages/driver/driver-overview/driver-overview.page';
import { RideDetails } from './pages/driver/ride-details/ride-details';
import { DashboardPage } from './pages/driver/dashboard/dashboard.page';

export const routes: Routes = [
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
