import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LatestListingsComponent } from './latest-listings.component';

describe('LatestListingsComponent', () => {
  let component: LatestListingsComponent;
  let fixture: ComponentFixture<LatestListingsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LatestListingsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LatestListingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
