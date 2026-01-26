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
  selectedFile: File | null = null;
  imagePreview: string | null = null;
  
  showErrors: boolean = false;
  showSuccess: boolean = false;
  isLoading: boolean = true;
  isSaving: boolean = false;
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

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      
      // Create image preview
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.imagePreview = e.target.result;
      };
      reader.readAsDataURL(file);
    }
  }

  onSave(): void {
    this.showErrors = !this.name || !this.surname || !this.email || !this.phone || !this.address;
    
    if (!this.showErrors) {
      this.isSaving = true;
      
      const userProfile: UserProfile = {
        name: this.name,
        surname: this.surname,
        email: this.email,
        phoneNumber: this.phone,
        address: this.address
      };
      
      this.userService.updateCurrentUserProfile(userProfile, this.selectedFile || undefined).subscribe({
        next: (response) => {
          console.log('Profile updated successfully:', response);
          this.showSuccess = true;
          this.isSaving = false;
          this.selectedFile = null;
          this.imagePreview = null;
          
          // Reload user data to get updated imageUrl
          this.loadUserProfile();
        },
        error: (error) => {
          console.error('Failed to update profile:', error);
          this.errorMessage = 'Failed to update profile: ' + (error.error?.message || error.message);
          this.isSaving = false;
        }
      });
    }
  }

  closeSuccess(): void {
    this.showSuccess = false;
  }
}

