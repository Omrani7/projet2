import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PopularStatesGridComponent } from './popular-states-grid.component';

describe('PopularStatesGridComponent', () => {
  let component: PopularStatesGridComponent;
  let fixture: ComponentFixture<PopularStatesGridComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PopularStatesGridComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PopularStatesGridComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
