import { Routes } from '@angular/router';
import { DriverOverviewPage } from './pages/driver/driver-overview/driver-overview.page';
import { RideDetails } from './pages/driver/ride-details/ride-details';
import { DriverProfilePage } from './pages/driver/driver-profile/driver-profile.page';

export const routes: Routes = [
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
    redirectTo: 'driver/overview',
    pathMatch: 'full'
  }
];
