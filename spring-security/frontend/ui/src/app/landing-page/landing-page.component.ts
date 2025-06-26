import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HeaderComponent } from '../components/header/header.component';
import { HeroSectionComponent } from '../components/hero-section/hero-section.component';
import { LatestListingsComponent } from '../components/latest-listings/latest-listings.component';
import { PopularStatesGridComponent } from '../components/popular-states-grid/popular-states-grid.component';
import { StatsInfoComponent } from '../components/stats-info/stats-info.component';
import { StateSpecificListingsComponent } from '../components/state-specific-listings/state-specific-listings.component';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [
    CommonModule,
    HeaderComponent,
    HeroSectionComponent,
    LatestListingsComponent,
    PopularStatesGridComponent,
    StatsInfoComponent,
    StateSpecificListingsComponent
  ],
  templateUrl: './landing-page.component.html',
  styleUrls: ['./landing-page.component.css']
})
export class LandingPageComponent implements OnInit {

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Landing page is now accessible to all user types
    // Access restrictions will be applied only to specific roommate functionality
  }
}
