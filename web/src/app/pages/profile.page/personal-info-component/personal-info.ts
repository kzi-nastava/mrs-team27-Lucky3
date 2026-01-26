import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserProfile, UserService } from '../../../infrastructure/rest/user.service';

@Component({
  selector: 'app-personal-info',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './personal-info.html',
  styles: []
})
export class PersonalInfoComponent implements OnInit {
  name: string = '';
  email: string = '';
  password: string = '12345678';
  surname: string = '';
  address: string = '';
  phone: string = '';
  imageUrl: string = '';
  showErrors: boolean = false;
  showSuccess: boolean = false;
  isLoading: boolean = true;
  errorMessage: string = '';

  constructor(
    private userService: UserService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    console.log('Component initialized');
    this.loadUserProfile();
  }

  loadUserProfile(): void {
    console.log('Loading user profile...');
    this.isLoading = true;
    
    this.userService.getCurrentUser().subscribe({
      next: (user: UserProfile) => {
        console.log('User data received:', user);
        
        this.name = user.name;
        this.surname = user.surname;
        this.email = user.email;
        this.phone = user.phoneNumber;
        this.address = user.address;
        this.imageUrl = user.imageUrl || '';
        this.isLoading = false;
        
        console.log('Component properties updated:', {
          name: this.name,
          surname: this.surname,
          email: this.email,
          phone: this.phone,
          address: this.address
        });
        
        // Manually trigger change detection
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Failed to load user profile:', error);
        this.errorMessage = 'Failed to load user data: ' + error.message;
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  onSave(): void {
    this.showErrors = !this.name || !this.surname || !this.email || !this.phone || !this.address;
    if (!this.showErrors) {
      this.showSuccess = true;
      // TODO: Implement update API call here
    }
  }

  closeSuccess(): void {
    this.showSuccess = false;
  }
}

