import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, of, Subscription } from 'rxjs';
import { catchError, finalize, take } from 'rxjs/operators';

import { PropertyListingService } from '../../services/property-listing.service';
import { InstituteService } from '../../services/institute.service';

import { PropertyListingDTO} from "../../../../../src/app/models/property-listing.dto";
import { Institute} from "../../../../../src/app/models/institute.model";
import { Pageable} from "../../../../../src/app/models/pageable.model";
import { PropertySearchCriteria} from "../../../../../src/app/models/property-search-criteria.model";
import { Page } from "../../../../../src/app/models/page.model";

import { FilterBarComponent } from '../../components/filter-bar/filter-bar.component';
import { PropertyListComponent } from '../../components/property-list/property-list.component';
import { MapDisplayComponent } from '../../components/map-display/map-display.component';

@Component({
  selector: 'app-discovery',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    FilterBarComponent,
    PropertyListComponent,
    MapDisplayComponent
  ],
  templateUrl: './discovery.component.html',
  styleUrl: './discovery.component.css'
})
export class DiscoveryComponent implements OnInit {

  propertyPage$: Observable<Page<PropertyListingDTO> | null> = of(null);

  selectedInstitute: Institute | null = null;

  currentSearchCriteria: PropertySearchCriteria = {};
  currentPageable: Pageable = { page: 0, size: 10, sort: 'listingDate,desc' };

  isLoading: boolean = false;
  errorMessage: string | null = null;

  private queryParamsSubscription: Subscription | undefined;

  constructor(
    private propertyListingService: PropertyListingService,
    private instituteService: InstituteService,
    private activatedRoute: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.queryParamsSubscription = this.activatedRoute.queryParams.pipe(take(1)).subscribe(params => {
      const searchCriteriaFromQuery: PropertySearchCriteria = {};
      if (params['genericQuery']) searchCriteriaFromQuery.genericQuery = params['genericQuery'];
      if (params['propertyType']) searchCriteriaFromQuery.propertyType = params['propertyType'];
      if (params['minPrice']) searchCriteriaFromQuery.minPrice = +params['minPrice'];
      if (params['maxPrice']) searchCriteriaFromQuery.maxPrice = +params['maxPrice'];
      if (params['bedrooms']) searchCriteriaFromQuery.bedrooms = +params['bedrooms'];
      if (params['instituteId']) searchCriteriaFromQuery.instituteId = +params['instituteId'];
      if (params['radiusKm']) searchCriteriaFromQuery.radiusKm = +params['radiusKm'];

      this.currentSearchCriteria = searchCriteriaFromQuery;

      const pageableFromQuery: Pageable = { page: 0, size: 10, sort: 'listingDate,desc' };
      if (params['page']) pageableFromQuery.page = +params['page'];
      if (params['size']) pageableFromQuery.size = +params['size'];
      if (params['sort']) pageableFromQuery.sort = params['sort'];

      this.currentPageable = pageableFromQuery;

      const instituteIdFromQuery = params['instituteId'] ? +params['instituteId'] : null;
      const instituteNameFromQuery = params['instituteName'] || null;

      if (instituteIdFromQuery && instituteNameFromQuery) {
        this.selectedInstitute = {
          id: instituteIdFromQuery,
          name: instituteNameFromQuery,
          latitude: params['instituteLat'] ? +params['instituteLat'] : 0,
          longitude: params['instituteLng'] ? +params['instituteLng'] : 0
        };
      } else {
        this.selectedInstitute = null;
      }

      this.fetchProperties();
    });
  }

  fetchProperties(): void {
    this.isLoading = true;
    this.errorMessage = null;
    console.log('Fetching with criteria:', this.currentSearchCriteria, 'Pageable:', this.currentPageable);
    this.propertyPage$ = this.propertyListingService.searchProperties(
      this.currentSearchCriteria,
      this.currentPageable
    ).pipe(
      catchError(err => {
        console.error("Error fetching properties:", err);
        this.errorMessage = "Failed to load properties. Please try again later.";
        return of(null);
      }),
      finalize(() => {
        this.isLoading = false;
      })
    );
  }

  onFiltersChanged(newCriteria: PropertySearchCriteria): void {
    this.currentSearchCriteria = { ...this.currentSearchCriteria, ...newCriteria };
    this.currentPageable.page = 0;
    this.fetchProperties();
  }

  onSortChanged(sortValue: string): void {
    this.currentPageable.sort = sortValue;
    this.currentPageable.page = 0;
    this.fetchProperties();
  }

  onPageChanged(pageNumber: number): void {
    this.currentPageable.page = pageNumber;
    this.fetchProperties();
  }

  onInstituteSelected(institute: Institute | null): void {
    this.selectedInstitute = institute;
    this.currentSearchCriteria.instituteId = institute ? institute.id : undefined;
    if (institute && !this.currentSearchCriteria.radiusKm) {
      this.currentSearchCriteria.radiusKm = 3.0;
    } else if (!institute) {
      delete this.currentSearchCriteria.radiusKm;
    }
    this.currentPageable.page = 0;
    this.fetchProperties();
  }
}
