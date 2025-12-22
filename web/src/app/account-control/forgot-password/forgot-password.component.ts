import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './forgot-password.component.html',
  styles: []
})
export class ForgotPasswordComponent {
  email = '';
  loading = false;

  constructor(private router: Router) {}

  onSubmit() {
    if (!this.email) return;

    this.loading = true;
    setTimeout(() => {
      this.loading = false;
      this.router.navigate(['/reset-password-sent'], { queryParams: { email: this.email } });
    }, 1500);
  }
}
