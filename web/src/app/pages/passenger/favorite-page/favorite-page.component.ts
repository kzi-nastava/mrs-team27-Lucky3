import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { RideService } from '../../../infrastructure/rest/ride.service';
import { RideService } from '../../../infrastructure/rest/ride.service';

export interface FavoriteRoute {
  id: number;
  name: string;
  start: string;
  destination: string;
}

@Component({
  selector: 'app-favorite-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './favorite-page.component.html'
})
export class FavoritePageComponent implements OnInit {
  favoriteRoutes: FavoriteRoute[] = [];
  isLoading: boolean = true;
  errorMessage: string = '';

  constructor(
    private router: Router,
    private rideService: RideService  // Inject your ride service here
  ) {}

  ngOnInit(): void {
    this.loadFavoriteRoutes();
  }

  loadFavoriteRoutes(): void {
    this.isLoading = true;
    this.errorMessage = '';
    
    // Call your ride service to get favorite routes
    this.rideService.getFavoriteRoutes().subscribe({
      next: (routes) => {
         this.favoriteRoutes = routes;
         this.isLoading = false;
       },
       error: (error) => {
         this.errorMessage = 'Failed to load favorite routes';
         this.isLoading = false;
         console.error('Error loading favorite routes:', error);
       }
    });

    // Mock data for testing
    setTimeout(() => {
      this.favoriteRoutes = [
        { id: 1, name: 'Morning Commute', start: 'Home', destination: 'Office' },
        { id: 2, name: 'Weekend Trip', start: 'City Center', destination: 'Airport' },
        { id: 3, name: 'Gym Route', start: 'Apartment', destination: 'Fitness Center' }
      ];
      this.isLoading = false;
    }, 500);
  }

  removeFavorite(routeId: number): void {
    if (confirm('Are you sure you want to remove this route from favorites?')) {
      // Call your ride service to remove favorite
      // this.rideService.removeFavoriteRoute(routeId).subscribe({
      //   next: () => {
      //     this.favoriteRoutes = this.favoriteRoutes.filter(route => route.id !== routeId);
      //   },
      //   error: (error) => {
      //     this.errorMessage = 'Failed to remove favorite route';
      //     console.error('Error removing favorite route:', error);
      //   }
      // });

      // Mock removal
      this.favoriteRoutes = this.favoriteRoutes.filter(route => route.id !== routeId);
    }
  }

  orderRoute(route: FavoriteRoute): void {
    // Call your ride service to order the route
    // this.rideService.orderRoute(route).subscribe({
    //   next: (response) => {
    //     console.log('Route ordered successfully:', response);
    //     this.router.navigate(['/order-confirmation', response.orderId]);
    //   },
    //   error: (error) => {
    //     this.errorMessage = 'Failed to order route';
    //     console.error('Error ordering route:', error);
    //   }
    // });

    console.log('Ordering route:', route);
    alert(`Ordering route: ${route.name} from ${route.start} to ${route.destination}`);
  }
}
