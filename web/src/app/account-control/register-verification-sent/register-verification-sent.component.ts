import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-register-verification-sent',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './register-verification-sent.component.html'
})
export class RegisterVerificationSentComponent implements OnInit {
  email: string = '';
  resendCooldown = 0;

  constructor(
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.email = params['email'] || 'your email';
    });
  }

  resendEmail() {
    if (this.resendCooldown > 0) return;
    
    // Simulate API call
    console.log('Resending verification email to:', this.email);
    
    this.resendCooldown = 60;
    const interval = setInterval(() => {
      this.resendCooldown--;
      this.cdr.detectChanges();
      
      if (this.resendCooldown <= 0) {
        clearInterval(interval);
      }
    }, 1000);
  }
}
