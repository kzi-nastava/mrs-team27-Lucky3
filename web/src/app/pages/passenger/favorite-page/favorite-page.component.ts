import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { RideService } from '../../../infrastructure/rest/ride.service';
import { LocationDto } from '../../../infrastructure/rest/model/location.model';
import { AuthService } from '../../../infrastructure/auth/auth.service';

//matching DTO from backend
export interface FavoriteRouteResponse {
  id: number;
  routeName: string;
  startLocation: LocationDto;
  endLocation: LocationDto;
  stops: LocationDto[];
  distance: number;       // Double in Java
  estimatedTime: number;  // Double in Java
}

@Component({
  selector: 'app-favorite-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './favorite-page.component.html'
})
export class FavoritePageComponent implements OnInit {
  favoriteRoutes: FavoriteRouteResponse[] = [];
  isLoading: boolean = true;
  errorMessage: string = '';
passengerId: number | null = null;

  constructor(
    private authService: AuthService, // Inject your auth service here
    private router: Router,
    private rideService: RideService,  // Inject your ride service here
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
      this.passengerId = this.authService.getUserId();
      //load favorite routes
      this.loadFavoriteRoutes();
    }

  loadFavoriteRoutes(): void {
    this.isLoading = true;
    this.errorMessage = '';
    
    // Call your ride service to get favorite routes
    this.rideService.getFavoriteRoutes(this.passengerId!).subscribe({
      next: (routes) => {
         this.favoriteRoutes = routes;
         this.isLoading = false;
         this.cdr.detectChanges();
       },
       error: (error) => {
         this.errorMessage = 'Failed to load favorite routes';
         this.isLoading = false;
         console.error('Error loading favorite routes:', error);
       }
    });

  }

  removeFavorite(routeId: number): void {
    if (confirm('Are you sure you want to remove this route from favorites?')) {
       //Calling ride service to remove favorite
       this.rideService.removeFavoriteRoute(this.passengerId!, routeId).subscribe({
         next: () => {
           this.favoriteRoutes = this.favoriteRoutes.filter(route => route.id !== routeId);
         },
         error: (error) => {
           this.errorMessage = 'Failed to remove favorite route';
           console.error('Error removing favorite route:', error);
         }
       });

      // Mock removal
      this.favoriteRoutes = this.favoriteRoutes.filter(route => route.id !== routeId);
    }
  }

  orderRoute(route: FavoriteRouteResponse): void {
    // Navigate to the ride ordering page with route data as state
    this.router.navigate(['/passenger/home'], {
        state: {
        startLocation: route.startLocation,
        endLocation: route.endLocation,
        stops: route.stops || [],
        routeName: route.routeName,
        fromFavorites: true
        }
    });

    //console.log('Ordering route:', route);
    //alert(`Ordering route: ${route.routeName} from ${route.startLocation} to ${route.endLocation}`);
  }
}
