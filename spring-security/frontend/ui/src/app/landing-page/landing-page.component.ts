import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HeaderComponent } from '../components/header/header.component';
import { HeroSectionComponent } from '../components/hero-section/hero-section.component';
import { RecommendedListComponent } from '../components/recommended-list/recommended-list.component';
import { PopularStatesGridComponent } from '../components/popular-states-grid/popular-states-grid.component';
import { StatsInfoComponent } from '../components/stats-info/stats-info.component';
import { StateSpecificListingsComponent } from '../components/state-specific-listings/state-specific-listings.component';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    HeaderComponent,
    HeroSectionComponent,
    RecommendedListComponent,
    PopularStatesGridComponent,
    StatsInfoComponent,
    StateSpecificListingsComponent
  ],
  templateUrl: './landing-page.component.html',
  styleUrls: ['./landing-page.component.css']
})
export class LandingPageComponent {
  searchForm: FormGroup;

  constructor(private fb: FormBuilder, private router: Router) {
    this.searchForm = this.fb.group({
      query: [''] // For a general search query
    });
  }

  onHeroSearchSubmitted(searchQueryValue: string): void {
    const queryParams: any = {};
    if (searchQueryValue && searchQueryValue.trim() !== '') {
      queryParams.genericQuery = searchQueryValue.trim();
    }
    // Add other potential general search fields from landing page here if needed in future

    this.router.navigate(['/discovery'], { queryParams });
  }
}
