import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-passenger-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './passenger-home.page.html',
  styles: []
})
export class PassengerHomePage {
  name: string = 'James';
}
