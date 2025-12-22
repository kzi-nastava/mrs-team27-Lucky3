import { Component, OnInit } from '@angular/core';
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

  constructor(private route: ActivatedRoute) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.email = params['email'] || 'your email';
    });
  }
}
