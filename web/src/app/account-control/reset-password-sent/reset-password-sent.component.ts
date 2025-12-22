import { Component, OnInit } from '@angular/core';
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

  constructor(private route: ActivatedRoute) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.email = params['email'] || 'your email';
    });
  }
}
