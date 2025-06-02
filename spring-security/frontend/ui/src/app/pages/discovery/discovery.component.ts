import { Component, OnInit, OnDestroy, ViewChild, AfterViewInit, ElementRef, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, of, Subscription, BehaviorSubject } from 'rxjs';
import { catchError, finalize, take } from 'rxjs/operators';

import { PropertyListingService } from '../../services/property-listing.service';
import { InstituteService } from '../../services/institute.service';

import { PropertyListingDTO } from '../../models/property-listing.dto';
import { Institute } from '../../models/institute.model';
import { Pageable } from '../../models/pageable.model';
import { PropertySearchCriteria } from '../../models/property-search-criteria.model';
import { Page } from '../../models/page.model';

import { FilterBarComponent } from '../../components/filter-bar/filter-bar.component';
import { PropertyListComponent } from '../../components/property-list/property-list.component';
import { MapDisplayComponent } from '../../components/map-display/map-display.component';
import { HeaderComponent } from '../../components/header/header.component';

@Component({
  selector: 'app-discovery',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    FilterBarComponent,
    PropertyListComponent,
    MapDisplayComponent,
    HeaderComponent
  ],
  templateUrl: './discovery.component.html',
  styleUrl: './discovery.component.css'
})
export class DiscoveryComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild(MapDisplayComponent) mapDisplayComponent?: MapDisplayComponent;
  @ViewChild('propertyListContainer') propertyListContainer?: ElementRef;

  propertyPage$: Observable<Page<PropertyListingDTO> | null> = of(null);
  
  // Track visible property IDs for map syncing
  visiblePropertyIds: number[] = [];
  selectedPropertyId: number | null = null;

  selectedInstitute: Institute | null = null;
  currentSearchCriteria: PropertySearchCriteria = {};
  currentPageable: Pageable = { page: 0, size: 50, sort: 'listingDate,desc' };

  isLoading: boolean = false;
  errorMessage: string | null = null;

  private propertyPage: Page<PropertyListingDTO> | null = null;
  private queryParamsSubscription: Subscription | undefined;
  private scrollCheckInterval: any = null;

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

      const pageableFromQuery: Pageable = { page: 0, size: 50, sort: 'listingDate,desc' };
      if (params['page']) pageableFromQuery.page = +params['page'];
      if (params['sort']) pageableFromQuery.sort = params['sort'];

      this.currentPageable = pageableFromQuery;

      const instituteIdFromQuery = params['instituteId'] ? +params['instituteId'] : null;
      const instituteNameFromQuery = params['instituteName'] || null;

      if (instituteIdFromQuery && instituteNameFromQuery) {
        this.selectedInstitute = {
          id: instituteIdFromQuery,
          name: instituteNameFromQuery,
          latitude: params['instituteLat'] ? +params['instituteLat'] : 0,
          longitude: params['instituteLng'] ? +params['instituteLng'] : 0,
          address: '',
          city: '',
          district: '',
          type: '',
          website: ''
        };
      } else {
        this.selectedInstitute = null;
      }

      this.fetchProperties();
    });
  }

  ngAfterViewInit(): void {
    // Force a timer to update the map size to handle any race conditions
    setTimeout(() => {
      if (this.mapDisplayComponent) {
        this.mapDisplayComponent.updateMapSize();
      }
      // Start tracking visible properties
      this.startMonitoringVisibleProperties();
    }, 500);
  }

  // Start monitoring which properties are visible in the view
  private startMonitoringVisibleProperties(): void {
    // Run a check immediately
    this.checkVisibleProperties();
    
    // Set up a periodic check (every 300ms)
    this.scrollCheckInterval = setInterval(() => {
      this.checkVisibleProperties();
    }, 300);
  }

  // Triggered when property list container is scrolled
  @HostListener('scroll', ['$event.target'])
  onScroll(target: any): void {
    this.checkVisibleProperties();
  }

  // Handle property selection from the map
  onPropertySelected(propertyId: number): void {
    console.log(`Property selected from map: ${propertyId}`);
    
    // Only act if the selection actually changed
    if (this.selectedPropertyId !== propertyId) {
      this.selectedPropertyId = propertyId;
      
      // Scroll the property into view in the list with a smooth animation
      if (this.propertyPage && this.propertyListContainer) {
        const index = this.propertyPage.content.findIndex(p => p.id === propertyId);
        if (index !== -1) {
          // Find the property card element
          const propertyCards = this.propertyListContainer.nativeElement.querySelectorAll('.property-item-card');
          if (propertyCards[index]) {
            // Use smooth scrolling for a nicer transition
            propertyCards[index].scrollIntoView({ 
              behavior: 'smooth', 
              block: 'center',
              inline: 'nearest'
            });
            
            // Add a brief flash animation to make the selection more noticeable
            this.flashSelectedCard(propertyCards[index]);
          }
        }
      }
    }
  }
  
  // Add a brief highlighting animation to make selected card more noticeable
  private flashSelectedCard(cardElement: HTMLElement): void {
    // Only proceed if we have access to the element
    if (!cardElement) return;
    
    // Add a simple flash animation class
    cardElement.classList.add('card-flash-animation');
    
    // Remove the animation class after it completes to allow it to be re-triggered
    setTimeout(() => {
      cardElement.classList.remove('card-flash-animation');
    }, 1000); // Animation duration plus a small buffer
  }
  
  // Check which properties are visible in the viewport - enhanced with throttling
  private checkVisibleProperties(): void {
    if (!this.propertyListContainer || !this.propertyPage) return;
    
    // Throttle frequent scroll events
    if (this.throttleScrollCheck()) return;
    
    // Get the container's bounding rectangle
    const containerRect = this.propertyListContainer.nativeElement.getBoundingClientRect();
    const topEdge = containerRect.top;
    const bottomEdge = containerRect.bottom;
    
    // Get all property cards within the container
    const propertyCards = this.propertyListContainer.nativeElement.querySelectorAll('.property-item-card');
    
    // Store visible property IDs
    const newVisibleIds: number[] = [];
    
    // Check each card's visibility within the viewport
    propertyCards.forEach((card: HTMLElement, index: number) => {
      if (!this.propertyPage) return;
      
      const cardRect = card.getBoundingClientRect();
      
      // Calculate percentage of card that's visible
      const visibleHeight = Math.min(cardRect.bottom, bottomEdge) - Math.max(cardRect.top, topEdge);
      const visibilityPercentage = visibleHeight > 0 ? (visibleHeight / cardRect.height) * 100 : 0;
      
      // If at least 40% of the card is visible in the viewport
      if (visibilityPercentage >= 40) {
        // Add the property ID to the visible list
        const property = this.propertyPage.content[index];
        if (property && property.id) {
          newVisibleIds.push(property.id);
        }
      }
    });
    
    // Check if visible properties have changed
    const hasChanged = 
      newVisibleIds.length !== this.visiblePropertyIds.length || 
      newVisibleIds.some(id => !this.visiblePropertyIds.includes(id));
    
    // Update the visible property IDs if changed
    if (hasChanged) {
      this.visiblePropertyIds = newVisibleIds;
      console.log(`Updated visible properties: ${this.visiblePropertyIds.length} properties in view`);
    }
  }
  
  // Throttle helper to avoid too frequent scroll checks
  private lastScrollCheck = 0;
  private throttleScrollCheck(): boolean {
    const now = Date.now();
    const timeSinceLastCheck = now - this.lastScrollCheck;
    
    // Only check every 150ms for better performance
    if (timeSinceLastCheck < 150) {
      return true; // Skip this check
    }
    
    this.lastScrollCheck = now;
    return false; // Proceed with the check
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
        // Update map size after data load completes
        setTimeout(() => {
          if (this.mapDisplayComponent) {
            this.mapDisplayComponent.updateMapSize();
          }
          // Check visible properties after data load
          this.checkVisibleProperties();
        }, 100);
      })
    );
    
    // Store the property page for access
    this.propertyPage$.subscribe(page => {
      this.propertyPage = page;
      // Reset selected and visible properties when we get new data
      this.selectedPropertyId = null;
      this.visiblePropertyIds = [];
      
      // Check visible properties after a delay to allow rendering
      setTimeout(() => this.checkVisibleProperties(), 100);
    });
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

  ngOnDestroy(): void {
    if (this.queryParamsSubscription) {
      this.queryParamsSubscription.unsubscribe();
    }
    
    // Clear interval for monitoring visible properties
    if (this.scrollCheckInterval) {
      clearInterval(this.scrollCheckInterval);
      this.scrollCheckInterval = null;
    }
  }
}
