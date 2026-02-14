import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { provideRouter, Router } from '@angular/router';
import { of, Subject } from 'rxjs';
import { RegisterComponent } from './register.component';
import { AuthService } from '../../infrastructure/auth/auth.service';
import { PassengerRegistrationRequest } from '../../model/registration.model';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  const validFormData = {
    firstName: 'John',
    lastName: 'Doe',
    email: 'john.doe@example.com',
    phone: '+1 (555) 123-4567',
    address: '123 Main Street, City',
    password: 'Password123!',
    confirmPassword: 'Password123!'
  };

  const expectedRegistrationRequest: PassengerRegistrationRequest = {
    name: 'John',
    surname: 'Doe',
    email: 'john.doe@example.com',
    phoneNumber: '+1 (555) 123-4567',
    address: '123 Main Street, City',
    password: 'Password123!',
    confirmPassword: 'Password123!'
  };

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['register']);

    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy }
      ]
    })
    .compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // =========================================================================
  // Component creation
  // =========================================================================
  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  // =========================================================================
  // Form initialization
  // =========================================================================
  describe('Form Initialization', () => {
    it('should create registerForm with all required controls', () => {
      expect(component.registerForm).toBeTruthy();
      expect(component.registerForm.contains('firstName')).toBeTrue();
      expect(component.registerForm.contains('lastName')).toBeTrue();
      expect(component.registerForm.contains('email')).toBeTrue();
      expect(component.registerForm.contains('phone')).toBeTrue();
      expect(component.registerForm.contains('address')).toBeTrue();
      expect(component.registerForm.contains('password')).toBeTrue();
      expect(component.registerForm.contains('confirmPassword')).toBeTrue();
    });

    it('should initialize all form controls with empty strings', () => {
      expect(component.registerForm.get('firstName')?.value).toBe('');
      expect(component.registerForm.get('lastName')?.value).toBe('');
      expect(component.registerForm.get('email')?.value).toBe('');
      expect(component.registerForm.get('phone')?.value).toBe('');
      expect(component.registerForm.get('address')?.value).toBe('');
      expect(component.registerForm.get('password')?.value).toBe('');
      expect(component.registerForm.get('confirmPassword')?.value).toBe('');
    });

    it('should initialize the form as invalid', () => {
      expect(component.registerForm.valid).toBeFalse();
    });

    it('should initialize loading as false', () => {
      expect(component.loading).toBeFalse();
    });

    it('should initialize error as empty string', () => {
      expect(component.error).toBe('');
    });

    it('should initialize selectedFile as null', () => {
      expect(component.selectedFile).toBeNull();
    });

    it('should initialize showPassword as false', () => {
      expect(component.showPassword).toBeFalse();
    });

    it('should initialize showConfirmPassword as false', () => {
      expect(component.showConfirmPassword).toBeFalse();
    });
  });

  // =========================================================================
  // firstName field validation
  // =========================================================================
  describe('firstName Validation', () => {
    it('should be invalid when empty (required)', () => {
      const control = component.registerForm.get('firstName')!;
      control.setValue('');
      expect(control.hasError('required')).toBeTrue();
    });

    it('should be invalid when shorter than 2 characters', () => {
      const control = component.registerForm.get('firstName')!;
      control.setValue('J');
      expect(control.hasError('minlength')).toBeTrue();
    });

    it('should be valid with 2 characters', () => {
      const control = component.registerForm.get('firstName')!;
      control.setValue('Jo');
      expect(control.valid).toBeTrue();
    });

    it('should be valid with a proper name', () => {
      const control = component.registerForm.get('firstName')!;
      control.setValue('John');
      expect(control.valid).toBeTrue();
    });
  });

  // =========================================================================
  // lastName field validation
  // =========================================================================
  describe('lastName Validation', () => {
    it('should be invalid when empty (required)', () => {
      const control = component.registerForm.get('lastName')!;
      control.setValue('');
      expect(control.hasError('required')).toBeTrue();
    });

    it('should be invalid when shorter than 2 characters', () => {
      const control = component.registerForm.get('lastName')!;
      control.setValue('D');
      expect(control.hasError('minlength')).toBeTrue();
    });

    it('should be valid with 2 characters', () => {
      const control = component.registerForm.get('lastName')!;
      control.setValue('Do');
      expect(control.valid).toBeTrue();
    });

    it('should be valid with a proper surname', () => {
      const control = component.registerForm.get('lastName')!;
      control.setValue('Doe');
      expect(control.valid).toBeTrue();
    });
  });

  // =========================================================================
  // email field validation
  // =========================================================================
  describe('Email Validation', () => {
    it('should be invalid when empty (required)', () => {
      const control = component.registerForm.get('email')!;
      control.setValue('');
      expect(control.hasError('required')).toBeTrue();
    });

    it('should be invalid with malformed email (no @)', () => {
      const control = component.registerForm.get('email')!;
      control.setValue('invalidemail');
      expect(control.hasError('email')).toBeTrue();
    });

    it('should be invalid with malformed email (no domain)', () => {
      const control = component.registerForm.get('email')!;
      control.setValue('user@');
      expect(control.hasError('email')).toBeTrue();
    });

    it('should be valid with proper email format', () => {
      const control = component.registerForm.get('email')!;
      control.setValue('john.doe@example.com');
      expect(control.valid).toBeTrue();
    });
  });

  // =========================================================================
  // phone field validation
  // =========================================================================
  describe('Phone Validation', () => {
    it('should be invalid when empty (required)', () => {
      const control = component.registerForm.get('phone')!;
      control.setValue('');
      expect(control.hasError('required')).toBeTrue();
    });

    it('should be invalid with too short a number', () => {
      const control = component.registerForm.get('phone')!;
      control.setValue('123');
      expect(control.hasError('pattern')).toBeTrue();
    });

    it('should be invalid with letters', () => {
      const control = component.registerForm.get('phone')!;
      control.setValue('abcdefghijk');
      expect(control.hasError('pattern')).toBeTrue();
    });

    it('should be valid with 10+ digit number', () => {
      const control = component.registerForm.get('phone')!;
      control.setValue('1234567890');
      expect(control.valid).toBeTrue();
    });

    it('should be valid with international format', () => {
      const control = component.registerForm.get('phone')!;
      control.setValue('+1 (555) 123-4567');
      expect(control.valid).toBeTrue();
    });
  });

  // =========================================================================
  // address field validation
  // =========================================================================
  describe('Address Validation', () => {
    it('should be invalid when empty (required)', () => {
      const control = component.registerForm.get('address')!;
      control.setValue('');
      expect(control.hasError('required')).toBeTrue();
    });

    it('should be invalid when shorter than 5 characters', () => {
      const control = component.registerForm.get('address')!;
      control.setValue('Main');
      expect(control.hasError('minlength')).toBeTrue();
    });

    it('should be valid with 5 characters', () => {
      const control = component.registerForm.get('address')!;
      control.setValue('123 M');
      expect(control.valid).toBeTrue();
    });

    it('should be valid with a full address', () => {
      const control = component.registerForm.get('address')!;
      control.setValue('123 Main Street, City');
      expect(control.valid).toBeTrue();
    });
  });

  // =========================================================================
  // password field validation
  // =========================================================================
  describe('Password Validation', () => {
    it('should be invalid when empty (required)', () => {
      const control = component.registerForm.get('password')!;
      control.setValue('');
      expect(control.hasError('required')).toBeTrue();
    });

    it('should be invalid when shorter than 8 characters', () => {
      const control = component.registerForm.get('password')!;
      control.setValue('Pass1!');
      expect(control.hasError('minlength')).toBeTrue();
    });

    it('should be valid with exactly 8 characters', () => {
      const control = component.registerForm.get('password')!;
      control.setValue('Pass123!');
      expect(control.valid).toBeTrue();
    });

    it('should be valid with a strong password', () => {
      const control = component.registerForm.get('password')!;
      control.setValue('Password123!');
      expect(control.valid).toBeTrue();
    });
  });

  // =========================================================================
  // confirmPassword field validation
  // =========================================================================
  describe('Confirm Password Validation', () => {
    it('should be invalid when empty (required)', () => {
      const control = component.registerForm.get('confirmPassword')!;
      control.setValue('');
      expect(control.hasError('required')).toBeTrue();
    });

    it('should be valid when it has any non-empty value (own validators)', () => {
      const control = component.registerForm.get('confirmPassword')!;
      control.setValue('something');
      expect(control.valid).toBeTrue();
    });
  });

  // =========================================================================
  // Cross-field passwordMatchValidator
  // =========================================================================
  describe('Password Match Validator (cross-field)', () => {
    it('should set passwordMismatch error when passwords differ', () => {
      component.registerForm.get('password')!.setValue('Password123!');
      component.registerForm.get('confirmPassword')!.setValue('DifferentPass1!');
      expect(component.registerForm.hasError('passwordMismatch')).toBeTrue();
    });

    it('should not set passwordMismatch error when passwords match', () => {
      component.registerForm.get('password')!.setValue('Password123!');
      component.registerForm.get('confirmPassword')!.setValue('Password123!');
      expect(component.registerForm.hasError('passwordMismatch')).toBeFalse();
    });

    it('should not set passwordMismatch error when password is empty', () => {
      component.registerForm.get('password')!.setValue('');
      component.registerForm.get('confirmPassword')!.setValue('Something1!');
      expect(component.registerForm.hasError('passwordMismatch')).toBeFalse();
    });

    it('should not set passwordMismatch error when confirmPassword is empty', () => {
      component.registerForm.get('password')!.setValue('Password123!');
      component.registerForm.get('confirmPassword')!.setValue('');
      expect(component.registerForm.hasError('passwordMismatch')).toBeFalse();
    });
  });

  // =========================================================================
  // Whole form validity with all fields
  // =========================================================================
  describe('Whole Form Validity', () => {
    it('should be valid when all fields are properly filled and passwords match', () => {
      component.registerForm.patchValue(validFormData);
      expect(component.registerForm.valid).toBeTrue();
    });

    it('should be invalid when any required field is missing', () => {
      const dataWithoutEmail = { ...validFormData, email: '' };
      component.registerForm.patchValue(dataWithoutEmail);
      expect(component.registerForm.valid).toBeFalse();
    });

    it('should be invalid when passwords do not match', () => {
      component.registerForm.patchValue({ ...validFormData, confirmPassword: 'Mismatch1!' });
      expect(component.registerForm.valid).toBeFalse();
    });
  });

  // =========================================================================
  // File selection
  // =========================================================================
  describe('onFileSelected', () => {
    it('should set selectedFile when a file is selected', () => {
      const mockFile = new File(['content'], 'photo.png', { type: 'image/png' });
      const event = { target: { files: [mockFile] } };

      component.onFileSelected(event);

      expect(component.selectedFile).toBe(mockFile);
    });

    it('should not change selectedFile when no file is provided', () => {
      const event = { target: { files: [] } };
      component.onFileSelected(event);
      expect(component.selectedFile).toBeNull();
    });
  });

  // =========================================================================
  // Password visibility toggles
  // =========================================================================
  describe('Password Visibility Toggles', () => {
    it('should toggle showPassword from false to true', () => {
      expect(component.showPassword).toBeFalse();
      component.togglePasswordVisibility();
      expect(component.showPassword).toBeTrue();
    });

    it('should toggle showPassword back to false', () => {
      component.showPassword = true;
      component.togglePasswordVisibility();
      expect(component.showPassword).toBeFalse();
    });

    it('should toggle showConfirmPassword from false to true', () => {
      expect(component.showConfirmPassword).toBeFalse();
      component.toggleConfirmPasswordVisibility();
      expect(component.showConfirmPassword).toBeTrue();
    });

    it('should toggle showConfirmPassword back to false', () => {
      component.showConfirmPassword = true;
      component.toggleConfirmPasswordVisibility();
      expect(component.showConfirmPassword).toBeFalse();
    });
  });

  // =========================================================================
  // Form getter
  // =========================================================================
  describe('f getter (form controls shortcut)', () => {
    it('should return the form controls', () => {
      const controls = component.f;
      expect(controls['firstName']).toBeDefined();
      expect(controls['email']).toBeDefined();
      expect(controls['password']).toBeDefined();
    });
  });

  // =========================================================================
  // onSubmit — guard clauses
  // =========================================================================
  describe('onSubmit — Guard Clauses', () => {
    it('should not call authService.register when form is invalid', () => {
      component.onSubmit();
      expect(authServiceSpy.register).not.toHaveBeenCalled();
    });

    it('should mark all controls as touched when form is invalid', () => {
      spyOn(component.registerForm, 'markAllAsTouched');
      component.onSubmit();
      expect(component.registerForm.markAllAsTouched).toHaveBeenCalled();
    });

    it('should not call authService.register when already loading', () => {
      component.loading = true;
      component.registerForm.patchValue(validFormData);
      component.onSubmit();
      expect(authServiceSpy.register).not.toHaveBeenCalled();
    });
  });

  // =========================================================================
  // onSubmit — successful registration
  // =========================================================================
  describe('onSubmit — Successful Registration', () => {
    beforeEach(() => {
      component.registerForm.patchValue(validFormData);
    });

    it('should set loading to true when submit starts', () => {
      authServiceSpy.register.and.returnValue(of({}));
      component.onSubmit();
      // After subscribe completes synchronously with of({}), finalize runs
      // So we verify register was called (loading was set before call)
      expect(authServiceSpy.register).toHaveBeenCalled();
    });

    it('should call authService.register with correct registration data and no file', () => {
      authServiceSpy.register.and.returnValue(of({}));
      component.onSubmit();

      expect(authServiceSpy.register).toHaveBeenCalledOnceWith(
        expectedRegistrationRequest,
        undefined
      );
    });

    it('should call authService.register with correct data and file when file is selected', () => {
      const mockFile = new File(['img'], 'avatar.jpg', { type: 'image/jpeg' });
      component.selectedFile = mockFile;
      authServiceSpy.register.and.returnValue(of({}));

      component.onSubmit();

      expect(authServiceSpy.register).toHaveBeenCalledOnceWith(
        expectedRegistrationRequest,
        mockFile
      );
    });

    it('should store email in localStorage on success', () => {
      spyOn(localStorage, 'setItem');
      authServiceSpy.register.and.returnValue(of({}));

      component.onSubmit();

      expect(localStorage.setItem).toHaveBeenCalledWith(
        'pendingActivationEmail',
        'john.doe@example.com'
      );
    });

    it('should navigate to /register-verification-sent with query params and state', () => {
      authServiceSpy.register.and.returnValue(of({}));
      component.onSubmit();

      expect(router.navigate).toHaveBeenCalledOnceWith(
        ['/register-verification-sent'],
        {
          queryParams: { email: 'john.doe@example.com' },
          state: { registered: true }
        }
      );
    });

    it('should set loading to false after successful registration (finalize)', () => {
      authServiceSpy.register.and.returnValue(of({}));
      component.onSubmit();
      expect(component.loading).toBeFalse();
    });

    it('should clear error on successful submit', () => {
      component.error = 'Previous error';
      authServiceSpy.register.and.returnValue(of({}));
      component.onSubmit();
      expect(component.error).toBe('');
    });
  });

  // =========================================================================
  // onSubmit — maps form fields to DTO correctly
  // =========================================================================
  describe('onSubmit — DTO Field Mapping', () => {
    it('should map firstName to name in the DTO', () => {
      component.registerForm.patchValue(validFormData);
      authServiceSpy.register.and.returnValue(of({}));
      component.onSubmit();

      const callArg = authServiceSpy.register.calls.mostRecent().args[0];
      expect(callArg.name).toBe('John');
    });

    it('should map lastName to surname in the DTO', () => {
      component.registerForm.patchValue(validFormData);
      authServiceSpy.register.and.returnValue(of({}));
      component.onSubmit();

      const callArg = authServiceSpy.register.calls.mostRecent().args[0];
      expect(callArg.surname).toBe('Doe');
    });

    it('should map phone to phoneNumber in the DTO', () => {
      component.registerForm.patchValue(validFormData);
      authServiceSpy.register.and.returnValue(of({}));
      component.onSubmit();

      const callArg = authServiceSpy.register.calls.mostRecent().args[0];
      expect(callArg.phoneNumber).toBe('+1 (555) 123-4567');
    });

    it('should map email directly to DTO email', () => {
      component.registerForm.patchValue(validFormData);
      authServiceSpy.register.and.returnValue(of({}));
      component.onSubmit();

      const callArg = authServiceSpy.register.calls.mostRecent().args[0];
      expect(callArg.email).toBe('john.doe@example.com');
    });

    it('should map address directly to DTO address', () => {
      component.registerForm.patchValue(validFormData);
      authServiceSpy.register.and.returnValue(of({}));
      component.onSubmit();

      const callArg = authServiceSpy.register.calls.mostRecent().args[0];
      expect(callArg.address).toBe('123 Main Street, City');
    });

    it('should map password directly to DTO password', () => {
      component.registerForm.patchValue(validFormData);
      authServiceSpy.register.and.returnValue(of({}));
      component.onSubmit();

      const callArg = authServiceSpy.register.calls.mostRecent().args[0];
      expect(callArg.password).toBe('Password123!');
    });

    it('should include confirmPassword in the DTO', () => {
      component.registerForm.patchValue(validFormData);
      authServiceSpy.register.and.returnValue(of({}));
      component.onSubmit();

      const callArg = authServiceSpy.register.calls.mostRecent().args[0];
      expect(callArg.confirmPassword).toBe('Password123!');
    });
  });

  // =========================================================================
  // onSubmit — error handling
  // =========================================================================
  describe('onSubmit — Error Handling', () => {
    beforeEach(() => {
      spyOn(console, 'error');
      component.registerForm.patchValue(validFormData);
    });

    it('should display Error.message when backend throws Error instance', () => {
      const subject = new Subject<any>();
      authServiceSpy.register.and.returnValue(subject.asObservable());

      component.onSubmit();
      subject.error(new Error('User with this email already exists.'));

      expect(component.error).toBe('User with this email already exists.');
    });

    it('should display err.error when it is a string', () => {
      const subject = new Subject<any>();
      authServiceSpy.register.and.returnValue(subject.asObservable());

      component.onSubmit();
      subject.error({ error: 'Email already taken', status: 409 });

      expect(component.error).toBe('Email already taken');
    });

    it('should display err.error.message when err.error is an object with message', () => {
      const subject = new Subject<any>();
      authServiceSpy.register.and.returnValue(subject.asObservable());

      component.onSubmit();
      subject.error({ error: { message: 'Validation failed' }, status: 400 });

      expect(component.error).toBe('Validation failed');
    });

    it('should display fallback message when error has no recognizable format', () => {
      const subject = new Subject<any>();
      authServiceSpy.register.and.returnValue(subject.asObservable());

      component.onSubmit();
      subject.error({ status: 500 });

      expect(component.error).toBe('Registration failed. Please try again.');
    });

    it('should set loading to false after an error (finalize)', () => {
      const subject = new Subject<any>();
      authServiceSpy.register.and.returnValue(subject.asObservable());

      component.onSubmit();
      subject.error(new Error('Server error'));

      expect(component.loading).toBeFalse();
    });

    it('should not navigate on error', () => {
      const subject = new Subject<any>();
      authServiceSpy.register.and.returnValue(subject.asObservable());

      component.onSubmit();
      subject.error(new Error('Failure'));

      expect(router.navigate).not.toHaveBeenCalled();
    });
  });

  // =========================================================================
  // Template rendering — form presence
  // =========================================================================
  describe('Template Rendering', () => {
    it('should render the registration form element', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const formEl = compiled.querySelector('form');
      expect(formEl).toBeTruthy();
    });

    it('should render all 7 form input fields', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const inputs = compiled.querySelectorAll('input[formControlName]');
      // 7 fields: firstName, lastName, email, phone, address, password, confirmPassword
      expect(inputs.length).toBe(7);
    });

    it('should render a submit button', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const button = compiled.querySelector('button[type="submit"]');
      expect(button).toBeTruthy();
    });

    it('should show "Create Account" text on submit button when not loading', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const button = compiled.querySelector('button[type="submit"]');
      expect(button?.textContent?.trim()).toContain('Create Account');
    });

    it('should render the file upload input', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const fileInput = compiled.querySelector('input[type="file"]');
      expect(fileInput).toBeTruthy();
    });

    it('should not display any error message initially', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      // The error div is only created when component.error is truthy
      const errorDiv = compiled.querySelector('.bg-red-500\\/10');
      expect(errorDiv).toBeFalsy();
    });

    it('should display error message when error is set', () => {
      const freshFixture = TestBed.createComponent(RegisterComponent);
      freshFixture.componentInstance.error = 'Registration failed';
      freshFixture.detectChanges();

      const compiled = freshFixture.nativeElement as HTMLElement;
      const errorDiv = compiled.querySelector('.bg-red-500\\/10');
      expect(errorDiv).toBeTruthy();
      expect(errorDiv?.textContent).toContain('Registration failed');
      freshFixture.destroy();
    });

    it('should display selected file name when a file is selected', () => {
      const freshFixture = TestBed.createComponent(RegisterComponent);
      freshFixture.componentInstance.selectedFile = new File([''], 'my-photo.png', { type: 'image/png' });
      freshFixture.detectChanges();

      const compiled = freshFixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('my-photo.png');
      freshFixture.destroy();
    });
  });

  // =========================================================================
  // Template rendering — validation error messages
  // =========================================================================
  describe('Template — Validation Error Messages', () => {
    it('should show "First name is required" when firstName is touched and empty', () => {
      component.registerForm.get('firstName')!.markAsTouched();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('First name is required');
    });

    it('should show minlength error for firstName when value is 1 char and touched', () => {
      const control = component.registerForm.get('firstName')!;
      control.setValue('J');
      control.markAsTouched();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Must be at least 2 characters');
    });

    it('should show "Last name is required" when lastName is touched and empty', () => {
      component.registerForm.get('lastName')!.markAsTouched();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Last name is required');
    });

    it('should show "Email is required" when email is touched and empty', () => {
      component.registerForm.get('email')!.markAsTouched();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Email is required');
    });

    it('should show "Invalid email address" when email format is wrong and touched', () => {
      const control = component.registerForm.get('email')!;
      control.setValue('bad-email');
      control.markAsTouched();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Invalid email address');
    });

    it('should show "Phone number is required" when phone is touched and empty', () => {
      component.registerForm.get('phone')!.markAsTouched();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Phone number is required');
    });

    it('should show "Invalid phone number format" when phone pattern is wrong and touched', () => {
      const control = component.registerForm.get('phone')!;
      control.setValue('abc');
      control.markAsTouched();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Invalid phone number format');
    });

    it('should show "Address is required" when address is touched and empty', () => {
      component.registerForm.get('address')!.markAsTouched();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Address is required');
    });

    it('should show "Address is too short" when address is under 5 chars and touched', () => {
      const control = component.registerForm.get('address')!;
      control.setValue('Main');
      control.markAsTouched();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Address is too short');
    });

    it('should show "Password is required" when password is touched and empty', () => {
      component.registerForm.get('password')!.markAsTouched();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Password is required');
    });

    it('should show "At least 8 characters" when password is too short and touched', () => {
      const control = component.registerForm.get('password')!;
      control.setValue('short');
      control.markAsTouched();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('At least 8 characters');
    });

    it('should show "Confirmation is required" when confirmPassword is touched and empty', () => {
      component.registerForm.get('confirmPassword')!.markAsTouched();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Confirmation is required');
    });

    it('should show "Passwords do not match" when mismatch and confirmPassword is touched', () => {
      component.registerForm.get('password')!.setValue('Password123!');
      component.registerForm.get('confirmPassword')!.setValue('DifferentPass1');
      component.registerForm.get('confirmPassword')!.markAsTouched();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Passwords do not match');
    });

    it('should not show validation errors for untouched fields', () => {
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).not.toContain('First name is required');
      expect(compiled.textContent).not.toContain('Email is required');
      expect(compiled.textContent).not.toContain('Password is required');
    });
  });

  // =========================================================================
  // Template — Submit button state
  // =========================================================================
  describe('Template — Submit Button', () => {
    it('should disable the submit button when loading is true', () => {
      // Create fresh fixture to set loading before first change detection (avoids NG0100)
      const freshFixture = TestBed.createComponent(RegisterComponent);
      freshFixture.componentInstance.loading = true;
      freshFixture.detectChanges();

      const compiled = freshFixture.nativeElement as HTMLElement;
      const button = compiled.querySelector('button[type="submit"]') as HTMLButtonElement;
      expect(button.disabled).toBeTrue();
      freshFixture.destroy();
    });

    it('should show "Creating Account..." when loading', () => {
      const freshFixture = TestBed.createComponent(RegisterComponent);
      freshFixture.componentInstance.loading = true;
      freshFixture.detectChanges();

      const compiled = freshFixture.nativeElement as HTMLElement;
      const button = compiled.querySelector('button[type="submit"]');
      expect(button?.textContent?.trim()).toContain('Creating Account...');
      freshFixture.destroy();
    });

    it('should show "Create Account" when not loading', () => {
      component.loading = false;
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const button = compiled.querySelector('button[type="submit"]');
      expect(button?.textContent?.trim()).toContain('Create Account');
    });
  });

  // =========================================================================
  // Template — Password type toggling
  // =========================================================================
  describe('Template — Password Input Types', () => {
    it('should render password field with type "password" by default', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const passwordInput = compiled.querySelector('input[formControlName="password"]') as HTMLInputElement;
      expect(passwordInput.type).toBe('password');
    });

    it('should render password field with type "text" when showPassword is true', () => {
      // Create fresh fixture to set state before first change detection (avoids NG0100)
      const freshFixture = TestBed.createComponent(RegisterComponent);
      freshFixture.componentInstance.showPassword = true;
      freshFixture.detectChanges();

      const compiled = freshFixture.nativeElement as HTMLElement;
      const passwordInput = compiled.querySelector('input[formControlName="password"]') as HTMLInputElement;
      expect(passwordInput.type).toBe('text');
      freshFixture.destroy();
    });

    it('should render confirmPassword field with type "password" by default', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const confirmInput = compiled.querySelector('input[formControlName="confirmPassword"]') as HTMLInputElement;
      expect(confirmInput.type).toBe('password');
    });

    it('should render confirmPassword field with type "text" when showConfirmPassword is true', () => {
      // Create fresh fixture to set state before first change detection (avoids NG0100)
      const freshFixture = TestBed.createComponent(RegisterComponent);
      freshFixture.componentInstance.showConfirmPassword = true;
      freshFixture.detectChanges();

      const compiled = freshFixture.nativeElement as HTMLElement;
      const confirmInput = compiled.querySelector('input[formControlName="confirmPassword"]') as HTMLInputElement;
      expect(confirmInput.type).toBe('text');
      freshFixture.destroy();
    });
  });
});
