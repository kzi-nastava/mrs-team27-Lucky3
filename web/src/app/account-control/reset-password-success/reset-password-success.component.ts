import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-reset-password-success',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './reset-password-success.component.html',
  styles: []
})
export class ResetPasswordSuccessComponent implements OnInit {
  
  constructor(private router: Router) {}

  ngOnInit() {
    setTimeout(() => {
      this.router.navigate(['/login']);
    }, 3000);
  }
}