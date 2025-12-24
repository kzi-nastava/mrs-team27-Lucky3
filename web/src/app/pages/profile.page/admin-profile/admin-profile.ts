import { Component } from '@angular/core';
import { PersonalInfoComponent } from '../personal-info-component/personal-info';

@Component({
  selector: 'app-admin-profile',
  imports: [PersonalInfoComponent],
  standalone: true,
  templateUrl: './admin-profile.html',
  styleUrl: './admin-profile.css',
})
export class AdminProfile extends PersonalInfoComponent {

}
