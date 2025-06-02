import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OwnerPropertyDetailsComponent } from './owner-property-details.component';

describe('OwnerPropertyDetailsComponent', () => {
  let component: OwnerPropertyDetailsComponent;
  let fixture: ComponentFixture<OwnerPropertyDetailsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OwnerPropertyDetailsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(OwnerPropertyDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
