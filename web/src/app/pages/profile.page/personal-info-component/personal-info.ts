import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-personal-info',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './personal-info.html',
  styles: []
})
export class PersonalInfoComponent {
    name = 'Nikola';
    email = 'nikola@example.com';
    password = '12345678';
    surname = 'Perisic';
    address = 'Strazilovska 4';
    phone = '06534542221';
    showErrors = false;
    showSuccess = false;

    onSave() {
      this.showErrors = !this.name || !this.surname || !this.email || !this.phone || !this.address;
      if (!this.showErrors) {
        this.showSuccess = true;
      }
    }

    closeSuccess() {
      this.showSuccess = false;
    }
}
