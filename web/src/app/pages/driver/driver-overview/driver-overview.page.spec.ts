import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DriverOverviewPage } from './driver-overview.page';

describe('DriverOverviewPage', () => {
  let component: DriverOverviewPage;
  let fixture: ComponentFixture<DriverOverviewPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DriverOverviewPage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DriverOverviewPage);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
