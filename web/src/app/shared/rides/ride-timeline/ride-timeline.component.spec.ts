import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RideTimelineComponent } from './ride-timeline.component';

describe('RideTimelineComponent', () => {
  let component: RideTimelineComponent;
  let fixture: ComponentFixture<RideTimelineComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RideTimelineComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RideTimelineComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
