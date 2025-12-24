import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RideVehicleComponent } from './ride-vehicle.component';

describe('RideVehicleComponent', () => {
  let component: RideVehicleComponent;
  let fixture: ComponentFixture<RideVehicleComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RideVehicleComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RideVehicleComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
