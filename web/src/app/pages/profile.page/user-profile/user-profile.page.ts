import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PersonalInfoComponent } from '../personal-info-component/personal-info';

@Component({
  selector: 'app-user-profile.page',
  standalone: true,
  imports: [CommonModule, PersonalInfoComponent],
  templateUrl: './user-profile.page.html',
  styleUrls: ['./user-profile.page.css'],
})
export class UserProfilePage extends PersonalInfoComponent {}
