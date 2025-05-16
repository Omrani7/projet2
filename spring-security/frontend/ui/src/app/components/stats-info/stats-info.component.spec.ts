import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StatsInfoComponent } from './stats-info.component';

describe('StatsInfoComponent', () => {
  let component: StatsInfoComponent;
  let fixture: ComponentFixture<StatsInfoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatsInfoComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StatsInfoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
