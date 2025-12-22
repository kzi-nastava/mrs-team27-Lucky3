import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RideDetails } from './ride-details';

describe('RideDetails', () => {
  let component: RideDetails;
  let fixture: ComponentFixture<RideDetails>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RideDetails]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RideDetails);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
