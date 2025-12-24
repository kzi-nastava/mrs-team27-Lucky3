import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './forgot-password.component.html',
  styles: []
})
export class ForgotPasswordComponent {
  forgotPasswordForm: FormGroup;
  loading = false;

  constructor(
    private router: Router,
    private fb: FormBuilder
  ) {
    this.forgotPasswordForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  onSubmit() {
    if (this.forgotPasswordForm.invalid) {
      this.forgotPasswordForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    setTimeout(() => {
      this.loading = false;
      this.router.navigate(['/reset-password-sent'], { queryParams: { email: this.forgotPasswordForm.get('email')?.value } });
    }, 1500);
  }

  get f() { return this.forgotPasswordForm.controls; }
}
