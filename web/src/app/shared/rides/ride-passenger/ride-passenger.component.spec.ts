import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RidePassengerComponent } from './ride-passenger.component';

describe('RidePassengerComponent', () => {
  let component: RidePassengerComponent;
  let fixture: ComponentFixture<RidePassengerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RidePassengerComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RidePassengerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
