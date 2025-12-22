import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RideRouteComponent } from './ride-route.component';

describe('RideRouteComponent', () => {
  let component: RideRouteComponent;
  let fixture: ComponentFixture<RideRouteComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RideRouteComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RideRouteComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
