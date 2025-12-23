import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DriverProfilePage } from './driver-profile.page';

describe('DriverProfilePage', () => {
  let component: DriverProfilePage;
  let fixture: ComponentFixture<DriverProfilePage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DriverProfilePage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DriverProfilePage);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
