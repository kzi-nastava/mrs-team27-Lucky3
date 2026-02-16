// Add this at the very top of src/test.ts
(window as any).global = window;
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';
import { CreateDriverComponent, VehicleType } from './create-driver.component';
import { DriverService } from '../../infrastructure/rest/driver.service';

describe('CreateDriverComponent - Driver Without Vehicle', () => {
  let component: CreateDriverComponent;
  let fixture: ComponentFixture<CreateDriverComponent>;
  let driverServiceSpy: jasmine.SpyObj<DriverService>;
  let router: Router;

  const validDriverDataWithoutVehicle = {
    firstName: 'John',
    lastName: 'Doe',
    email: 'john.doe@example.com',
    phone: '+381 69 123 4567',
    address: '123 Main Street, Belgrade'
  };

  const validVehicleData = {
    model: '',
    licensePlate: '',
    year: '',
    vehicleType: '',
    numberOfSeats: '',
    babyTransport: false,
    petTransport: false
  };

  const expectedPayloadWithoutVehicle = {
    name: 'John',
    surname: 'Doe',
    email: 'john.doe@example.com',
    address: '123 Main Street, Belgrade',
    phone: '+381 69 123 4567'
  };

  beforeEach(async () => {
    driverServiceSpy = jasmine.createSpyObj('DriverService', ['createDriver']);

    await TestBed.configureTestingModule({
      imports: [CreateDriverComponent],
      providers: [
        provideRouter([]),
        { provide: DriverService, useValue: driverServiceSpy }
      ]
    })
    .compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(CreateDriverComponent);
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
    it('should create driverForm with all required controls', () => {
      expect(component.driverForm).toBeTruthy();
      expect(component.driverForm.contains('firstName')).toBeTrue();
      expect(component.driverForm.contains('lastName')).toBeTrue();
      expect(component.driverForm.contains('email')).toBeTrue();
      expect(component.driverForm.contains('phone')).toBeTrue();
      expect(component.driverForm.contains('address')).toBeTrue();
      expect(component.driverForm.contains('vehicle')).toBeTrue();
    });

    it('should create vehicle form group with all controls', () => {
      const vehicleGroup = component.driverForm.get('vehicle');
      expect(vehicleGroup).toBeTruthy();
      expect(vehicleGroup?.get('model')).toBeTruthy();
      expect(vehicleGroup?.get('licensePlate')).toBeTruthy();
      expect(vehicleGroup?.get('year')).toBeTruthy();
      expect(vehicleGroup?.get('vehicleType')).toBeTruthy();
      expect(vehicleGroup?.get('numberOfSeats')).toBeTruthy();
      expect(vehicleGroup?.get('babyTransport')).toBeTruthy();
      expect(vehicleGroup?.get('petTransport')).toBeTruthy();
    });

    it('should initialize all driver form controls with empty strings', () => {
      expect(component.driverForm.get('firstName')?.value).toBe('');
      expect(component.driverForm.get('lastName')?.value).toBe('');
      expect(component.driverForm.get('email')?.value).toBe('');
      expect(component.driverForm.get('phone')?.value).toBe('');
      expect(component.driverForm.get('address')?.value).toBe('');
    });

    it('should initialize all vehicle form controls with empty strings or false', () => {
      const vehicle = component.driverForm.get('vehicle');
      expect(vehicle?.get('model')?.value).toBe('');
      expect(vehicle?.get('licensePlate')?.value).toBe('');
      expect(vehicle?.get('year')?.value).toBe('');
      expect(vehicle?.get('vehicleType')?.value).toBe('');
      expect(vehicle?.get('numberOfSeats')?.value).toBe('');
      expect(vehicle?.get('babyTransport')?.value).toBe(false);
      expect(vehicle?.get('petTransport')?.value).toBe(false);
    });

    it('should initialize the form as invalid', () => {
      expect(component.driverForm.valid).toBeFalse();
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

    it('should have vehicleTypes array with all enum values', () => {
      expect(component.vehicleTypes).toEqual([VehicleType.LUXURY, VehicleType.VAN, VehicleType.STANDARD]);
    });
  });

  // =========================================================================
  // firstName field validation
  // =========================================================================
  describe('firstName Validation', () => {
    it('should be invalid when empty (required)', () => {
      const control = component.driverForm.get('firstName')!;
      control.setValue('');
      expect(control.hasError('required')).toBeTrue();
    });

    it('should be invalid when shorter than 2 characters', () => {
      const control = component.driverForm.get('firstName')!;
      control.setValue('J');
      expect(control.hasError('minlength')).toBeTrue();
    });

    it('should be valid with 2 characters', () => {
      const control = component.driverForm.get('firstName')!;
      control.setValue('Jo');
      expect(control.valid).toBeTrue();
    });

    it('should be valid with a proper name', () => {
      const control = component.driverForm.get('firstName')!;
      control.setValue('John');
      expect(control.valid).toBeTrue();
    });
  });

  // =========================================================================
  // lastName field validation
  // =========================================================================
  describe('lastName Validation', () => {
    it('should be invalid when empty (required)', () => {
      const control = component.driverForm.get('lastName')!;
      control.setValue('');
      expect(control.hasError('required')).toBeTrue();
    });

    it('should be invalid when shorter than 2 characters', () => {
      const control = component.driverForm.get('lastName')!;
      control.setValue('D');
      expect(control.hasError('minlength')).toBeTrue();
    });

    it('should be valid with 2 characters', () => {
      const control = component.driverForm.get('lastName')!;
      control.setValue('Do');
      expect(control.valid).toBeTrue();
    });

    it('should be valid with a proper surname', () => {
      const control = component.driverForm.get('lastName')!;
      control.setValue('Doe');
      expect(control.valid).toBeTrue();
    });
  });

  // =========================================================================
  // email field validation
  // =========================================================================
  describe('Email Validation', () => {
    it('should be invalid when empty (required)', () => {
      const control = component.driverForm.get('email')!;
      control.setValue('');
      expect(control.hasError('required')).toBeTrue();
    });

    it('should be invalid with malformed email (no @)', () => {
      const control = component.driverForm.get('email')!;
      control.setValue('invalidemail');
      expect(control.hasError('email')).toBeTrue();
    });

    it('should be invalid with malformed email (no domain)', () => {
      const control = component.driverForm.get('email')!;
      control.setValue('user@');
      expect(control.hasError('email')).toBeTrue();
    });

    it('should be valid with proper email format', () => {
      const control = component.driverForm.get('email')!;
      control.setValue('john.doe@example.com');
      expect(control.valid).toBeTrue();
    });
  });

  // =========================================================================
  // phone field validation
  // =========================================================================
  describe('Phone Validation', () => {
    it('should be invalid when empty (required)', () => {
      const control = component.driverForm.get('phone')!;
      control.setValue('');
      expect(control.hasError('required')).toBeTrue();
    });

    it('should be invalid with too short a number', () => {
      const control = component.driverForm.get('phone')!;
      control.setValue('123');
      expect(control.hasError('pattern')).toBeTrue();
    });

    it('should be invalid with letters', () => {
      const control = component.driverForm.get('phone')!;
      control.setValue('abcdefghijk');
      expect(control.hasError('pattern')).toBeTrue();
    });

    it('should be valid with 10+ digit number', () => {
      const control = component.driverForm.get('phone')!;
      control.setValue('0691234567');
      expect(control.valid).toBeTrue();
    });

    it('should be valid with international format', () => {
      const control = component.driverForm.get('phone')!;
      control.setValue('+381 69 123-4567');
      expect(control.valid).toBeTrue();
    });
  });

  // =========================================================================
  // address field validation
  // =========================================================================
  describe('Address Validation', () => {
    it('should be invalid when empty (required)', () => {
      const control = component.driverForm.get('address')!;
      control.setValue('');
      expect(control.hasError('required')).toBeTrue();
    });

    it('should be invalid when shorter than 5 characters', () => {
      const control = component.driverForm.get('address')!;
      control.setValue('Main');
      expect(control.hasError('minlength')).toBeTrue();
    });

    it('should be valid with 5 characters', () => {
      const control = component.driverForm.get('address')!;
      control.setValue('NS 21');
      expect(control.valid).toBeTrue();
    });

    it('should be valid with a full address', () => {
      const control = component.driverForm.get('address')!;
      control.setValue('123 Main Street, Belgrade');
      expect(control.valid).toBeTrue();
    });
  });

  // =========================================================================
  // Vehicle model field validation
  // =========================================================================
  describe('Vehicle Model Validation', () => {
    it('should be invalid when empty (required)', () => {
      const control = component.driverForm.get('vehicle.model')!;
      control.setValue('');
      expect(control.hasError('required')).toBeTrue();
    });

    it('should be valid with a vehicle model', () => {
      const control = component.driverForm.get('vehicle.model')!;
      control.setValue('Toyota Camry');
      expect(control.valid).toBeTrue();
    });
  });

  // =========================================================================
  // Vehicle licensePlate field validation
  // =========================================================================
  describe('Vehicle License Plate Validation', () => {
    it('should be invalid when empty (required)', () => {
      const control = component.driverForm.get('vehicle.licensePlate')!;
      control.setValue('');
      expect(control.hasError('required')).toBeTrue();
    });

    it('should be valid with a license plate', () => {
      const control = component.driverForm.get('vehicle.licensePlate')!;
      control.setValue('ABC-1234');
      expect(control.valid).toBeTrue();
    });
  });

  // =========================================================================
  // Vehicle year field validation
  // =========================================================================
  describe('Vehicle Year Validation', () => {
    it('should be invalid when empty (required)', () => {
      const control = component.driverForm.get('vehicle.year')!;
      control.setValue('');
      expect(control.hasError('required')).toBeTrue();
    });

    it('should be invalid when less than 1900', () => {
      const control = component.driverForm.get('vehicle.year')!;
      control.setValue(1899);
      expect(control.hasError('min')).toBeTrue();
    });

    it('should be invalid when greater than 2026', () => {
      const control = component.driverForm.get('vehicle.year')!;
      control.setValue(2027);
      expect(control.hasError('max')).toBeTrue();
    });

    it('should be valid with year 1900', () => {
      const control = component.driverForm.get('vehicle.year')!;
      control.setValue(1900);
      expect(control.valid).toBeTrue();
    });

    it('should be valid with year 2026', () => {
      const control = component.driverForm.get('vehicle.year')!;
      control.setValue(2026);
      expect(control.valid).toBeTrue();
    });

    it('should be valid with a proper year', () => {
      const control = component.driverForm.get('vehicle.year')!;
      control.setValue(2024);
      expect(control.valid).toBeTrue();
    });
  });

  // =========================================================================
  // Vehicle type field validation
  // =========================================================================
  describe('Vehicle Type Validation', () => {
    it('should be invalid when empty (required)', () => {
      const control = component.driverForm.get('vehicle.vehicleType')!;
      control.setValue('');
      expect(control.hasError('required')).toBeTrue();
    });

    it('should be valid with LUXURY type', () => {
      const control = component.driverForm.get('vehicle.vehicleType')!;
      control.setValue(VehicleType.LUXURY);
      expect(control.valid).toBeTrue();
    });

    it('should be valid with VAN type', () => {
      const control = component.driverForm.get('vehicle.vehicleType')!;
      control.setValue(VehicleType.VAN);
      expect(control.valid).toBeTrue();
    });

    it('should be valid with STANDARD type', () => {
      const control = component.driverForm.get('vehicle.vehicleType')!;
      control.setValue(VehicleType.STANDARD);
      expect(control.valid).toBeTrue();
    });
  });

  // =========================================================================
  // Vehicle numberOfSeats field validation
  // =========================================================================
  describe('Vehicle Number of Seats Validation', () => {
    it('should be invalid when empty (required)', () => {
      const control = component.driverForm.get('vehicle.numberOfSeats')!;
      control.setValue('');
      expect(control.hasError('required')).toBeTrue();
    });

    it('should be invalid when less than 1', () => {
      const control = component.driverForm.get('vehicle.numberOfSeats')!;
      control.setValue(0);
      expect(control.hasError('min')).toBeTrue();
    });

    it('should be invalid when greater than 50', () => {
      const control = component.driverForm.get('vehicle.numberOfSeats')!;
      control.setValue(51);
      expect(control.hasError('max')).toBeTrue();
    });

    it('should be valid with 1 seat', () => {
      const control = component.driverForm.get('vehicle.numberOfSeats')!;
      control.setValue(1);
      expect(control.valid).toBeTrue();
    });

    it('should be valid with 50 seats', () => {
      const control = component.driverForm.get('vehicle.numberOfSeats')!;
      control.setValue(50);
      expect(control.valid).toBeTrue();
    });

    it('should be valid with a proper number of seats', () => {
      const control = component.driverForm.get('vehicle.numberOfSeats')!;
      control.setValue(4);
      expect(control.valid).toBeTrue();
    });
  });

  // =========================================================================
  // Vehicle babyTransport and petTransport fields
  // =========================================================================
  describe('Vehicle Features (babyTransport & petTransport)', () => {
    it('should default babyTransport to false', () => {
      const control = component.driverForm.get('vehicle.babyTransport')!;
      expect(control.value).toBeFalse();
    });

    it('should default petTransport to false', () => {
      const control = component.driverForm.get('vehicle.petTransport')!;
      expect(control.value).toBeFalse();
    });

    it('should accept true for babyTransport', () => {
      const control = component.driverForm.get('vehicle.babyTransport')!;
      control.setValue(true);
      expect(control.value).toBeTrue();
    });

    it('should accept true for petTransport', () => {
      const control = component.driverForm.get('vehicle.petTransport')!;
      control.setValue(true);
      expect(control.value).toBeTrue();
    });
  });

  // =========================================================================
  // Form getters
  // =========================================================================
  xdescribe('Form Getters', () => {
    it('f getter should return driver form controls', () => {
      const controls = component.f;
      expect(controls['firstName']).toBeDefined();
      expect(controls['lastName']).toBeDefined();
      expect(controls['email']).toBeDefined();
      expect(controls['phone']).toBeDefined();
      expect(controls['address']).toBeDefined();
      expect(controls['vehicle']).toBeDefined();
    });

    it('v getter should return vehicle form controls', () => {
      const controls = component.v;
      expect(controls['model']).toBeDefined();
      expect(controls['licensePlate']).toBeDefined();
      expect(controls['year']).toBeDefined();
      expect(controls['vehicleType']).toBeDefined();
      expect(controls['numberOfSeats']).toBeDefined();
      expect(controls['babyTransport']).toBeDefined();
      expect(controls['petTransport']).toBeDefined();
    });
  });

  // =========================================================================
  // File selection
  // =========================================================================
  xdescribe('onFileSelected', () => {
    it('should set selectedFile when a file is selected', () => {
      const mockFile = new File(['content'], 'driver-photo.jpg', { type: 'image/jpeg' });
      const event = { target: { files: [mockFile] } };

      component.onFileSelected(event);

      expect(component.selectedFile).toBe(mockFile);
    });

    it('should not change selectedFile when no file is provided', () => {
      const event = { target: { files: [] } };
      component.onFileSelected(event);
      expect(component.selectedFile).toBeNull();
    });

    it('should preserve the previously selected file if the user cancels selection', () => {
      const initialFile = new File([''], 'initial.jpg');
      component.selectedFile = initialFile;

      const event = { target: { files: [] } };
      component.onFileSelected(event);

      expect(component.selectedFile).toBe(initialFile);
    });
  });
  // =========================================================================
  // onSubmit — guard clauses
  // =========================================================================
  xdescribe('onSubmit — Guard Clauses', () => {
    it('should not call driverService.createDriver when form is invalid', () => {
      component.onSubmit();
      expect(driverServiceSpy.createDriver).not.toHaveBeenCalled();
    });

    it('should mark all driver controls as touched when form is invalid', () => {
      component.onSubmit();
      expect(component.driverForm.get('firstName')?.touched).toBeTrue();
      expect(component.driverForm.get('lastName')?.touched).toBeTrue();
      expect(component.driverForm.get('email')?.touched).toBeTrue();
      expect(component.driverForm.get('phone')?.touched).toBeTrue();
      expect(component.driverForm.get('address')?.touched).toBeTrue();
    });

    it('should mark all vehicle controls as touched when form is invalid', () => {
      component.onSubmit();
      const vehicle = component.driverForm.get('vehicle');
      expect(vehicle?.get('model')?.touched).toBeTrue();
      expect(vehicle?.get('licensePlate')?.touched).toBeTrue();
      expect(vehicle?.get('year')?.touched).toBeTrue();
      expect(vehicle?.get('vehicleType')?.touched).toBeTrue();
      expect(vehicle?.get('numberOfSeats')?.touched).toBeTrue();
    });

    it('should show validation errors in UI when submitting invalid form', () => {
      component.onSubmit();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('First name is required');
    });
  });

  // =========================================================================
  // onSubmit — successful driver creation WITH vehicle
  // =========================================================================
  xdescribe('onSubmit — Successful Driver Creation WITH Vehicle', () => {
    const validCompleteData = {
      firstName: 'John',
      lastName: 'Doe',
      email: 'john.doe@example.com',
      phone: '+381 69 123 4567',
      address: '123 Main Street, Belgrade',
      vehicle: {
        model: 'Toyota Camry',
        licensePlate: 'BG-123-AB',
        year: 2024,
        vehicleType: VehicleType.STANDARD,
        numberOfSeats: 4,
        babyTransport: false,
        petTransport: true
      }
    };

    beforeEach(() => {
      component.driverForm.patchValue(validCompleteData);
    });

    it('should set loading to true during request', fakeAsync(() => {
      driverServiceSpy.createDriver.and.returnValue(of({}).pipe(delay(1000)));

      component.onSubmit();

      expect(component.loading).toBeTrue();

      tick(1000);

      expect(component.loading).toBeFalse();
    }));

    it('should call driverService.createDriver with FormData', () => {
      driverServiceSpy.createDriver.and.returnValue(of({}));
      component.onSubmit();

      expect(driverServiceSpy.createDriver).toHaveBeenCalledWith(jasmine.any(FormData));
    });

    it('should send FormData with correct payload structure', () => {
      driverServiceSpy.createDriver.and.returnValue(of({}));
      component.onSubmit();

      const formData = driverServiceSpy.createDriver.calls.mostRecent().args[0] as FormData;
      const requestBlob = formData.get('request') as Blob;
      
      expect(requestBlob).toBeTruthy();
      expect(requestBlob.type).toBe('application/json');
    });

    it('should map form fields correctly to payload', (done) => {
      driverServiceSpy.createDriver.and.returnValue(of({}));
      component.onSubmit();

      const formData = driverServiceSpy.createDriver.calls.mostRecent().args[0] as FormData;
      const requestBlob = formData.get('request') as Blob;
      
      requestBlob.text().then(text => {
        const payload = JSON.parse(text);
        expect(payload.name).toBe('John');
        expect(payload.surname).toBe('Doe');
        expect(payload.email).toBe('john.doe@example.com');
        expect(payload.phone).toBe('+381 69 123 4567');
        expect(payload.address).toBe('123 Main Street, Belgrade');
        expect(payload.vehicle.model).toBe('Toyota Camry');
        expect(payload.vehicle.licenseNumber).toBe('BG-123-AB');
        expect(payload.vehicle.vehicleType).toBe('STANDARD');
        expect(payload.vehicle.passengerSeats).toBe(4);
        expect(payload.vehicle.babyTransport).toBe(false);
        expect(payload.vehicle.petTransport).toBe(true);
        done();
      });
    });

    it('should include profile image in FormData when file is selected', () => {
      const mockFile = new File(['img'], 'driver.jpg', { type: 'image/jpeg' });
      component.selectedFile = mockFile;
      driverServiceSpy.createDriver.and.returnValue(of({}));

      component.onSubmit();

      const formData = driverServiceSpy.createDriver.calls.mostRecent().args[0] as FormData;
      expect(formData.get('profileImage')).toBe(mockFile);
    });

    it('should not include profile image in FormData when no file is selected', () => {
      component.selectedFile = null;
      driverServiceSpy.createDriver.and.returnValue(of({}));

      component.onSubmit();

      const formData = driverServiceSpy.createDriver.calls.mostRecent().args[0] as FormData;
      expect(formData.get('profileImage')).toBeNull();
    });

    it('should navigate to /admin/drivers on success', () => {
      spyOn(window, 'alert');
      driverServiceSpy.createDriver.and.returnValue(of({ id: 1 }));
      component.onSubmit();

      expect(router.navigate).toHaveBeenCalledOnceWith(['/admin/drivers']);
    });

    it('should show success alert on successful creation', () => {
      spyOn(window, 'alert');
      driverServiceSpy.createDriver.and.returnValue(of({ id: 1 }));
      component.onSubmit();

      expect(window.alert).toHaveBeenCalledWith('Driver created successfully!');
    });

    it('should set loading to false after successful creation', () => {
      driverServiceSpy.createDriver.and.returnValue(of({}));
      component.onSubmit();
      expect(component.loading).toBeFalse();
    });

    it('should clear error on successful submit', () => {
      component.error = 'Previous error';
      driverServiceSpy.createDriver.and.returnValue(of({}));
      component.onSubmit();
      expect(component.error).toBe('');
    });

    it('should log success response to console', () => {
      spyOn(console, 'log');
      const response = { id: 1, name: 'John' };
      driverServiceSpy.createDriver.and.returnValue(of(response));
      component.onSubmit();

      expect(console.log).toHaveBeenCalledWith('Success:', response);
    });
  });

  // =========================================================================
  // onSubmit — DTO field mapping details
  // =========================================================================
  xdescribe('onSubmit — DTO Field Mapping', () => {
    const validData = {
      firstName: 'Jane',
      lastName: 'Smith',
      email: 'jane.smith@test.com',
      phone: '0601234567',
      address: 'Street 456, City',
      vehicle: {
        model: 'Honda Accord',
        licensePlate: 'NS-456-CD',
        year: 2023,
        vehicleType: VehicleType.LUXURY,
        numberOfSeats: 5,
        babyTransport: true,
        petTransport: false
      }
    };

    beforeEach(() => {
      component.driverForm.patchValue(validData);
      driverServiceSpy.createDriver.and.returnValue(of({}));
    });

    it('should map firstName to name in payload', (done) => {
      component.onSubmit();
      const formData = driverServiceSpy.createDriver.calls.mostRecent().args[0] as FormData;
      const requestBlob = formData.get('request') as Blob;
      
      requestBlob.text().then(text => {
        const payload = JSON.parse(text);
        expect(payload.name).toBe('Jane');
        done();
      });
    });

    it('should map lastName to surname in payload', (done) => {
      component.onSubmit();
      const formData = driverServiceSpy.createDriver.calls.mostRecent().args[0] as FormData;
      const requestBlob = formData.get('request') as Blob;
      
      requestBlob.text().then(text => {
        const payload = JSON.parse(text);
        expect(payload.surname).toBe('Smith');
        done();
      });
    });

    it('should map licensePlate to licenseNumber in vehicle payload', (done) => {
      component.onSubmit();
      const formData = driverServiceSpy.createDriver.calls.mostRecent().args[0] as FormData;
      const requestBlob = formData.get('request') as Blob;
      
      requestBlob.text().then(text => {
        const payload = JSON.parse(text);
        expect(payload.vehicle.licenseNumber).toBe('NS-456-CD');
        done();
      });
    });

    it('should map numberOfSeats to passengerSeats in vehicle payload', (done) => {
      component.onSubmit();
      const formData = driverServiceSpy.createDriver.calls.mostRecent().args[0] as FormData;
      const requestBlob = formData.get('request') as Blob;
      
      requestBlob.text().then(text => {
        const payload = JSON.parse(text);
        expect(payload.vehicle.passengerSeats).toBe(5);
        done();
      });
    });
  });

  // =========================================================================
  // onSubmit — error handling
  // =========================================================================
  xdescribe('onSubmit — Error Handling', () => {
    const validData = {
      firstName: 'John',
      lastName: 'Doe',
      email: 'john.doe@example.com',
      phone: '+381 69 123 4567',
      address: '123 Main Street, Belgrade',
      vehicle: {
        model: 'Toyota Camry',
        licensePlate: 'BG-123-AB',
        year: 2024,
        vehicleType: VehicleType.STANDARD,
        numberOfSeats: 4,
        babyTransport: false,
        petTransport: false
      }
    };

    beforeEach(() => {
      spyOn(console, 'error');
      component.driverForm.patchValue(validData);
    });

    it('should display error.error.message when available', () => {
      const errorResponse = { error: { message: 'Driver with this email already exists' } };
      driverServiceSpy.createDriver.and.returnValue(throwError(() => errorResponse));

      component.onSubmit();

      expect(component.error).toBe('Driver with this email already exists');
    });

    it('should display fallback message when error has no message property', () => {
      const errorResponse = { status: 500 };
      driverServiceSpy.createDriver.and.returnValue(throwError(() => errorResponse));

      component.onSubmit();

      expect(component.error).toBe('Failed to create driver');
    });

    it('should log error to console', () => {
      const errorResponse = { error: { message: 'Server error' } };
      driverServiceSpy.createDriver.and.returnValue(throwError(() => errorResponse));

      component.onSubmit();

      expect(console.error).toHaveBeenCalledWith('Error:', errorResponse);
    });

    it('should set loading to false after an error', () => {
      const errorResponse = { error: { message: 'Error occurred' } };
      driverServiceSpy.createDriver.and.returnValue(throwError(() => errorResponse));

      component.onSubmit();

      expect(component.loading).toBeFalse();
    });

    it('should not navigate on error', () => {
      const errorResponse = { error: { message: 'Error occurred' } };
      driverServiceSpy.createDriver.and.returnValue(throwError(() => errorResponse));

      component.onSubmit();

      expect(router.navigate).not.toHaveBeenCalled();
    });

    it('should not show alert on error', () => {
      spyOn(window, 'alert');
      const errorResponse = { error: { message: 'Error occurred' } };
      driverServiceSpy.createDriver.and.returnValue(throwError(() => errorResponse));

      component.onSubmit();

      expect(window.alert).not.toHaveBeenCalled();
    });
  });
  // =========================================================================
  // Template rendering — form presence
  // =========================================================================
  xdescribe('Template Rendering', () => {
    it('should render the driver form element', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const formEl = compiled.querySelector('form');
      expect(formEl).toBeTruthy();
    });

    it('should render all driver information input fields', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('input[formControlName="firstName"]')).toBeTruthy();
      expect(compiled.querySelector('input[formControlName="lastName"]')).toBeTruthy();
      expect(compiled.querySelector('input[formControlName="email"]')).toBeTruthy();
      expect(compiled.querySelector('input[formControlName="phone"]')).toBeTruthy();
      expect(compiled.querySelector('input[formControlName="address"]')).toBeTruthy();
    });

    it('should render all vehicle information input fields', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('input[formControlName="model"]')).toBeTruthy();
      expect(compiled.querySelector('input[formControlName="licensePlate"]')).toBeTruthy();
      expect(compiled.querySelector('input[formControlName="year"]')).toBeTruthy();
      expect(compiled.querySelector('select[formControlName="vehicleType"]')).toBeTruthy();
      expect(compiled.querySelector('input[formControlName="numberOfSeats"]')).toBeTruthy();
      expect(compiled.querySelector('input[formControlName="babyTransport"]')).toBeTruthy();
      expect(compiled.querySelector('input[formControlName="petTransport"]')).toBeTruthy();
    });

    it('should render a submit button', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const button = compiled.querySelector('button[type="submit"]');
      expect(button).toBeTruthy();
    });

    it('should show \"Create Driver Account\" text on submit button when not loading', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const button = compiled.querySelector('button[type="submit"]');
      expect(button?.textContent?.trim()).toContain('Create Driver Account');
    });

    it('should render the file upload input', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const fileInput = compiled.querySelector('input[type="file"]');
      expect(fileInput).toBeTruthy();
    });

    it('should render vehicle type dropdown with all options', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const options = compiled.querySelectorAll('select[formControlName="vehicleType"] option');
      expect(options.length).toBeGreaterThan(3); // Including the disabled "Select" option
    });

    it('should not display any error message initially', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const errorDiv = compiled.querySelector('.bg-red-500\\/10');
      expect(errorDiv).toBeFalsy();
    });

    it('should display error message when error is set', () => {
      const freshFixture = TestBed.createComponent(CreateDriverComponent);
      freshFixture.componentInstance.error = 'Failed to create driver';
      freshFixture.detectChanges();

      const compiled = freshFixture.nativeElement as HTMLElement;
      const errorDiv = compiled.querySelector('.bg-red-500\\/10');
      expect(errorDiv).toBeTruthy();
      expect(errorDiv?.textContent).toContain('Failed to create driver');
      freshFixture.destroy();
    });

    it('should display selected file name when a file is selected', () => {
      const freshFixture = TestBed.createComponent(CreateDriverComponent);
      freshFixture.componentInstance.selectedFile = new File([''], 'driver-profile.jpg', { type: 'image/jpeg' });
      freshFixture.detectChanges();

      const compiled = freshFixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('driver-profile.jpg');
      freshFixture.destroy();
    });
  });

  // =========================================================================
  // Template rendering — validation error messages
  // =========================================================================
  xdescribe('Template — Validation Error Messages', () => {
    it('should show \"First name is required\" when firstName is touched and empty', () => {
      component.driverForm.get('firstName')!.markAsTouched();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('First name is required');
    });

    it('should show minlength error for firstName when value is 1 char and touched', () => {
      const control = component.driverForm.get('firstName')!;
      control.setValue('J');
      control.markAsTouched();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Must be at least 2 characters');
    });

    it('should show \"Last name is required\" when lastName is touched and empty', () => {
      component.driverForm.get('lastName')!.markAsTouched();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Last name is required');
    });

    it('should show \"Email is required\" when email is touched and empty', () => {
      component.driverForm.get('email')!.markAsTouched();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Email is required');
    });

    it('should show \"Invalid email address\" when email format is wrong and touched', () => {
      const control = component.driverForm.get('email')!;
      control.setValue('bad-email');
      control.markAsTouched();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Invalid email address');
    });

    it('should show \"Phone number is required\" when phone is touched and empty', () => {
      component.driverForm.get('phone')!.markAsTouched();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Phone number is required');
    });

    it('should show \"Invalid phone number format\" when phone pattern is wrong and touched', () => {
      const control = component.driverForm.get('phone')!;
      control.setValue('abc');
      control.markAsTouched();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Invalid phone number format');
    });

    it('should show \"Address is required\" when address is touched and empty', () => {
      component.driverForm.get('address')!.markAsTouched();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Address is required');
    });

    it('should show \"Address is too short\" when address is under 5 chars and touched', () => {
      const control = component.driverForm.get('address')!;
      control.setValue('Main');
      control.markAsTouched();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Address is too short');
    });

    it('should show \"Vehicle model is required\" when model is touched and empty', () => {
      component.driverForm.get('vehicle.model')!.markAsTouched();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Vehicle model is required');
    });

    it('should show \"License plate is required\" when licensePlate is touched and empty', () => {
      component.driverForm.get('vehicle.licensePlate')!.markAsTouched();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('License plate is required');
    });

    it('should show \"Year is required\" when year is touched and empty', () => {
      component.driverForm.get('vehicle.year')!.markAsTouched();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Year is required');
    });

    it('should show \"Vehicle type is required\" when vehicleType is touched and empty', () => {
      component.driverForm.get('vehicle.vehicleType')!.markAsTouched();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Vehicle type is required');
    });

    it('should show \"Number of seats is required\" when numberOfSeats is touched and empty', () => {
      component.driverForm.get('vehicle.numberOfSeats')!.markAsTouched();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Number of seats is required');
    });

    it('should not show validation errors for untouched fields', () => {
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).not.toContain('First name is required');
      expect(compiled.textContent).not.toContain('Email is required');
      expect(compiled.textContent).not.toContain('Vehicle model is required');
    });
  });

  // =========================================================================
  // Template — Submit button state
  // =========================================================================
  xdescribe('Template — Submit Button', () => {
    it('should disable the submit button when loading is true', () => {
      const freshFixture = TestBed.createComponent(CreateDriverComponent);
      freshFixture.componentInstance.loading = true;
      freshFixture.detectChanges();

      const compiled = freshFixture.nativeElement as HTMLElement;
      const button = compiled.querySelector('button[type="submit"]') as HTMLButtonElement;
      expect(button.disabled).toBeTrue();
      freshFixture.destroy();
    });

    it('should show \"Creating Driver Account...\" when loading', () => {
      const freshFixture = TestBed.createComponent(CreateDriverComponent);
      freshFixture.componentInstance.loading = true;
      freshFixture.detectChanges();

      const compiled = freshFixture.nativeElement as HTMLElement;
      const button = compiled.querySelector('button[type="submit"]');
      expect(button?.textContent?.trim()).toContain('Creating Driver Account...');
      freshFixture.destroy();
    });

    it('should show \"Create Driver Account\" when not loading', () => {
      component.loading = false;
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const button = compiled.querySelector('button[type="submit"]');
      expect(button?.textContent?.trim()).toContain('Create Driver Account');
    });

    it('should enable the submit button when not loading', () => {
      component.loading = false;
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const button = compiled.querySelector('button[type="submit"]') as HTMLButtonElement;
      expect(button.disabled).toBeFalse();
    });
  });

  // =========================================================================
  // Template — Back navigation link
  // =========================================================================
  xdescribe('Template — Back Navigation', () => {
    it('should render a back link to /admin/drivers', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const backLink = compiled.querySelector('a[routerLink="/admin/drivers"]');
      expect(backLink).toBeTruthy();
      expect(backLink?.textContent?.trim()).toContain('Back to drivers');
    });
  });
});
