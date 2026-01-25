import { Component, OnInit, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Router } from '@angular/router';

@Component({
  selector: 'app-favorite-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './favorite-page.component.html'
})
export class FavoritePageComponent  {
  
  constructor(router: Router) {

  }
}
