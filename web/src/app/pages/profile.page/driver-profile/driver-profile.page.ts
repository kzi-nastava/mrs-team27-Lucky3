import { Component } from '@angular/core';

@Component({
  selector: 'app-driver-profile.page',
  imports: [],
  templateUrl: './driver-profile.page.html',
  styleUrl: './driver-profile.page.css',
})
export class DriverProfilePage {
  showPersonalForm = false;
  showVehicleForm = false;

  openPersonalForm() {
    this.showPersonalForm = true;
  }

  closePersonalForm() {
    this.showPersonalForm = false;
  }

  openVehicleForm() {
    this.showVehicleForm = true;
  }

  closeVehicleForm() {
    this.showVehicleForm = false;
  }
}
