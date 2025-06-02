import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OwnerProfileSetupComponent } from './owner-profile-setup.component';

describe('OwnerProfileSetupComponent', () => {
  let component: OwnerProfileSetupComponent;
  let fixture: ComponentFixture<OwnerProfileSetupComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OwnerProfileSetupComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(OwnerProfileSetupComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
