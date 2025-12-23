import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';

@Component({
  selector: 'app-reset-password-sent',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './reset-password-sent.component.html',
  styles: []
})
export class ResetPasswordSentComponent implements OnInit {
  email = '';
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
    console.log('Resending email to:', this.email);
    
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
