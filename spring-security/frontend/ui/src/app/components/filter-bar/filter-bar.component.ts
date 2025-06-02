import { Component, EventEmitter, Output, OnDestroy, Input, OnChanges, SimpleChanges, OnInit, HostListener, ElementRef, ViewContainerRef, Renderer2, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { Observable, of, Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, catchError, takeUntil, finalize, map } from 'rxjs/operators';
import { Router, RouterModule } from '@angular/router';

import { InstituteService } from '../../services/institute.service';
import { Institute } from '../../models/institute.model';
import { AuthService } from '../../auth/auth.service';
import { PropertySearchCriteria } from '../../models/property-search-criteria.model';

// LoginComponent import is removed as the modal won't be triggered from here
// import { LoginComponent } from '../../auth/login/login.component';

interface SortOption {
  value: string;
  label: string;
}

@Component({
  selector: 'app-filter-bar',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule],
  templateUrl: './filter-bar.component.html',
  styleUrls: ['./filter-bar.component.css']
})
export class FilterBarComponent implements OnDestroy, OnChanges, OnInit {
  @Input() currentSelectedInstitute: Institute | null = null;
  @Input() simplified: boolean = false;
  @Output() instituteSelected = new EventEmitter<Institute | null>();
  @Output() sortChanged = new EventEmitter<string>();
  @Output() filtersChanged = new EventEmitter<PropertySearchCriteria>();
  @Output() clearFilters = new EventEmitter<void>();

  searchForm: FormGroup;
  filteredInstitutes$: Observable<Institute[]> = of([]);
  isLoadingInstitutes = false;
  showInstitutesDropdown = false;
  isScrolled = false;
  showPriceFilter = false;
  showFilterModal = false;
  // showLoginModal = false; // Removed

  // isDropdownOpen = false; // Removed (was for its own auth dropdown)
  // private clickListener!: () => void; // Removed

  filterForm: FormGroup;
  propertyTypes = ['Apartment', 'Studio', 'House', 'Room', 'S+1', 'S+2', 'S+3'];

  sortOptions: SortOption[] = [];
  private defaultSortOptions: SortOption[] = [
    { value: 'listingDate,desc', label: 'Newest' },
    { value: 'price,asc', label: 'Price: Low to High' },
    { value: 'price,desc', label: 'Price: High to Low' }
  ];
  private distanceSortOption: SortOption = { value: 'distance,asc', label: 'Distance: Nearest' };

  private destroy$ = new Subject<void>();
  private renderer = inject(Renderer2);
  private el = inject(ElementRef);
  private authService = inject(AuthService);

  private closePriceFilterUnlisten: (() => void) | null = null;

  // Properties for Auth and Dropdown from HeaderComponent
  isAuthenticated$: Observable<boolean>;
  isDropdownOpen = false;
  currentUserRole: string | null = null;
  private clickListener!: () => void;
  private authSubscription!: Subscription;

  // State variables for custom dropdowns
  showSortDropdown = false;
  showDistanceDropdown = false;
  showBedroomsDropdown = false;
  
  // Filter state
  searchCriteria: PropertySearchCriteria = {
    genericQuery: '',
    propertyType: '',
    minPrice: undefined,
    maxPrice: undefined,
    bedrooms: undefined,
    minArea: undefined,
    maxArea: undefined,
    instituteId: undefined,
    radiusKm: undefined
  };

  // Filter options
  bedroomOptions = [
    { value: undefined, label: 'Any' },
    { value: 0, label: 'Studio' },
    { value: 1, label: '1 Bedroom' },
    { value: 2, label: '2 Bedrooms' },
    { value: 3, label: '3 Bedrooms' },
    { value: 4, label: '4+ Bedrooms' }
  ];

  priceRanges = [
    { min: undefined, max: undefined, label: 'Any Price' },
    { min: 0, max: 300, label: 'Under 300 TND' },
    { min: 300, max: 500, label: '300 - 500 TND' },
    { min: 500, max: 800, label: '500 - 800 TND' },
    { min: 800, max: 1200, label: '800 - 1200 TND' },
    { min: 1200, max: undefined, label: 'Above 1200 TND' }
  ];

  areaRanges = [
    { min: undefined, max: undefined, label: 'Any Size' },
    { min: 0, max: 50, label: 'Under 50 m²' },
    { min: 50, max: 80, label: '50 - 80 m²' },
    { min: 80, max: 120, label: '80 - 120 m²' },
    { min: 120, max: 200, label: '120 - 200 m²' },
    { min: 200, max: undefined, label: 'Above 200 m²' }
  ];

  // UI state
  isExpanded = false;
  activeFiltersCount = 0;

  constructor(
    private fb: FormBuilder,
    private instituteService: InstituteService,
    private viewContainerRef: ViewContainerRef,
    private router: Router
  ) {
    this.searchForm = this.fb.group({
      instituteQuery: [''],
      sortBy: ['']
    });

    this.filterForm = this.fb.group({
      minPrice: [''],
      maxPrice: [''],
      bedrooms: [''],
      radiusKm: [''],
      priceRange: [500] 
    });

    this.updateSortOptions();

    // AuthState initialization from HeaderComponent
    this.isAuthenticated$ = this.authService.authState$.pipe(
      map(isAuth => !!isAuth),
      takeUntil(this.destroy$)
    );
    this.authSubscription = this.authService.authState$.subscribe(isAuth => {
      if (isAuth) {
        this.currentUserRole = this.authService.getUserRole();
      } else {
        this.isDropdownOpen = false;
        this.currentUserRole = null;
        this.removeClickListener();
      }
    });

    this.searchForm.get('instituteQuery')!.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(query => {
        if (this.currentSelectedInstitute && query === this.currentSelectedInstitute.name) {
          this.showInstitutesDropdown = false;
          return of([]);
        }
        if (query && query.trim().length > 1) {
          this.isLoadingInstitutes = true;
          this.showInstitutesDropdown = true;
          return this.instituteService.searchInstitutes(query.trim()).pipe(
            catchError(() => { this.isLoadingInstitutes = false; return of([]); }),
            finalize(() => this.isLoadingInstitutes = false)
          );
        } else {
          this.showInstitutesDropdown = false;
          return of([]);
        }
      }),
      takeUntil(this.destroy$)
    ).subscribe(institutes => {
      this.filteredInstitutes$ = of(institutes);
    });

    this.searchForm.get('sortBy')!.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (value) {
        this.sortChanged.emit(value);
      }
    });

    // Removed authState$ subscription that managed filter bar's own dropdown
  }

  ngOnInit(): void {
    this.checkScroll();
    window.addEventListener('scroll', this.checkScroll.bind(this));

    // Set up form value change listeners to apply filters automatically
    this.filterForm.get('bedrooms')!.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      // Auto-apply filter when bedrooms value changes
      this.applyFilters();
    });
    
    this.filterForm.get('radiusKm')!.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      // Auto-apply filter when distance value changes
      this.applyFilters();
    });

    this.updateActiveFiltersCount();
  }

  @HostListener('window:scroll', [])
  private checkScroll(): void {
    const scrollTop = window.pageYOffset || document.documentElement.scrollTop || document.body.scrollTop || 0;
    this.isScrolled = scrollTop > 10;
    const containerEl = this.el.nativeElement.querySelector('.filter-bar-container');
    if (containerEl) {
      if (this.isScrolled) {
        containerEl.classList.add('scrolled');
      } else {
        containerEl.classList.remove('scrolled');
      }
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['currentSelectedInstitute']) {
      const institute = changes['currentSelectedInstitute'].currentValue as Institute | null;
      if (institute) {
        this.searchForm.get('instituteQuery')!.setValue(institute.name, { emitEvent: false });
      } else {
        this.searchForm.get('instituteQuery')!.setValue('', { emitEvent: false });
      }
      this.updateSortOptions();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    window.removeEventListener('scroll', this.checkScroll.bind(this));
    this.removeClickListener();
    if (this.authSubscription) {
      this.authSubscription.unsubscribe();
    }
    
    if (this.closePriceFilterUnlisten) {
      this.closePriceFilterUnlisten();
      this.closePriceFilterUnlisten = null;
    }
    
    document.removeEventListener('click', this.closeSortDropdown);
    document.removeEventListener('click', this.closeDistanceDropdown);
    document.removeEventListener('click', this.closeBedroomsDropdown);
  }

  private updateSortOptions(): void {
    this.sortOptions = [...this.defaultSortOptions];
    if (this.currentSelectedInstitute) {
      this.sortOptions.unshift(this.distanceSortOption);
      if (!this.searchForm.get('sortBy')!.value.includes('distance')) {
        this.searchForm.get('sortBy')!.setValue(this.distanceSortOption.value);
      }
    } else {
      if (this.searchForm.get('sortBy')!.value.includes('distance')) {
        this.searchForm.get('sortBy')!.setValue(this.defaultSortOptions[0].value);
      }
    }
  }

  onFocusInstitute(): void {
    const query = this.searchForm.get('instituteQuery')!.value;
    this.showInstitutesDropdown = query && query.trim().length > 0;
  }

  onBlurInstitute(): void {
    setTimeout(() => {
      this.showInstitutesDropdown = false;
    }, 200);
  }

  onInstituteInputChange(): void {
    const query = this.searchForm.get('instituteQuery')!.value;
    this.showInstitutesDropdown = query && query.trim().length > 0;
    if (query && query.trim().length > 1 && query !== this.currentSelectedInstitute?.name) {
      this.isLoadingInstitutes = true;
      this.instituteService.searchInstitutes(query.trim()).pipe(
        catchError(() => { 
          this.isLoadingInstitutes = false; 
          return of([]); 
        }),
        finalize(() => this.isLoadingInstitutes = false)
      ).subscribe(institutes => {
        this.filteredInstitutes$ = of(institutes);
      });
    }
  }

  selectInstitute(institute: Institute): void {
    this.searchForm.get('instituteQuery')!.setValue(institute.name, { emitEvent: false });
    this.showInstitutesDropdown = false;
    this.instituteSelected.emit(institute);
  }

  clearInstituteSearch(): void {
    this.searchForm.get('instituteQuery')!.setValue('', { emitEvent: true });
    this.showInstitutesDropdown = false;
    this.instituteSelected.emit(null);
  }

  togglePriceFilter(event: MouseEvent): void {
    event.stopPropagation();

    const wasOpen = this.showPriceFilter;
    this.showPriceFilter = !this.showPriceFilter;

    if (this.showPriceFilter) {
      setTimeout(() => {
        if (this.closePriceFilterUnlisten) {
          this.closePriceFilterUnlisten();
        }
        this.closePriceFilterUnlisten = this.renderer.listen(
          'document',
          'click',
          (e: MouseEvent) => {
            const budgetButton = this.el.nativeElement.querySelector('.filter-chip-group button[attr.aria-selected]');
            const priceDropdownElement = this.el.nativeElement.querySelector('.price-dropdown');

            if (budgetButton && priceDropdownElement) {
              if (!budgetButton.contains(e.target) && !priceDropdownElement.contains(e.target)) {
                this.showPriceFilter = false;
                if (this.closePriceFilterUnlisten) {
                  this.closePriceFilterUnlisten();
                  this.closePriceFilterUnlisten = null;
                }
              }
            } else if (!budgetButton && priceDropdownElement && !priceDropdownElement.contains(e.target)) {
                this.showPriceFilter = false;
                if (this.closePriceFilterUnlisten) {
                  this.closePriceFilterUnlisten();
                  this.closePriceFilterUnlisten = null;
                }
            }
          }
        );
      }, 0);
    } else if (wasOpen && !this.showPriceFilter) {
      if (this.closePriceFilterUnlisten) {
        this.closePriceFilterUnlisten();
        this.closePriceFilterUnlisten = null;
      }
    }
  }

  toggleFilterModal(): void { this.showFilterModal = !this.showFilterModal; }
  closeFilterModal(): void { this.showFilterModal = false; }
  selectSort(event: Event): void { 
    const selectElement = event.target as HTMLSelectElement;
    if (selectElement && selectElement.value) {
        this.sortChanged.emit(selectElement.value);
    }
  }
  selectBedrooms(value: number | null): void { this.filterForm.get('bedrooms')!.setValue(value); }
  applyFiltersAndCloseModal(): void { this.applyFilters(); this.closeFilterModal(); }
  applyFilters(): void { 
    const criteria: PropertySearchCriteria = {};
    const formVal = this.filterForm.value;
    if (formVal.minPrice) criteria.minPrice = parseFloat(formVal.minPrice);
    if (formVal.maxPrice) criteria.maxPrice = parseFloat(formVal.maxPrice);
    if (formVal.bedrooms) criteria.bedrooms = parseInt(formVal.bedrooms, 10);
    if (formVal.radiusKm) criteria.radiusKm = parseInt(formVal.radiusKm, 10);
    
    this.filtersChanged.emit(criteria);
  }
  clearAllFilters(): void { 
    this.filterForm.reset({
        minPrice: '',
        maxPrice: '',
        bedrooms: '',
        radiusKm: '',
        priceRange: 500 
    });
    this.searchForm.get('sortBy')!.setValue(this.defaultSortOptions[0].value); 
    this.searchCriteria = {
      genericQuery: '',
      propertyType: '',
      minPrice: undefined,
      maxPrice: undefined,
      bedrooms: undefined,
      minArea: undefined,
      maxArea: undefined,
      instituteId: undefined,
      radiusKm: undefined
    };
    this.updateActiveFiltersCount();
    this.applyFilters();
    this.clearFilters.emit();
  }

  private updateActiveFiltersCount(): void {
    let count = 0;
    
    if (this.searchCriteria.genericQuery?.trim()) count++;
    if (this.searchCriteria.propertyType) count++;
    if (this.searchCriteria.minPrice !== undefined || this.searchCriteria.maxPrice !== undefined) count++;
    if (this.searchCriteria.bedrooms !== undefined) count++;
    if (this.searchCriteria.minArea !== undefined || this.searchCriteria.maxArea !== undefined) count++;
    
    this.activeFiltersCount = count;
  }

  private emitFilters(): void {
    // Clean up undefined values
    const cleanCriteria: PropertySearchCriteria = {};
    
    if (this.searchCriteria.genericQuery?.trim()) {
      cleanCriteria.genericQuery = this.searchCriteria.genericQuery.trim();
    }
    if (this.searchCriteria.propertyType) {
      cleanCriteria.propertyType = this.searchCriteria.propertyType;
    }
    if (this.searchCriteria.minPrice !== undefined && this.searchCriteria.minPrice !== null) {
      cleanCriteria.minPrice = this.searchCriteria.minPrice;
    }
    if (this.searchCriteria.maxPrice !== undefined && this.searchCriteria.maxPrice !== null) {
      cleanCriteria.maxPrice = this.searchCriteria.maxPrice;
    }
    if (this.searchCriteria.bedrooms !== undefined && this.searchCriteria.bedrooms !== null) {
      cleanCriteria.bedrooms = this.searchCriteria.bedrooms;
    }
    if (this.searchCriteria.minArea !== undefined && this.searchCriteria.minArea !== null) {
      cleanCriteria.minArea = this.searchCriteria.minArea;
    }
    if (this.searchCriteria.maxArea !== undefined && this.searchCriteria.maxArea !== null) {
      cleanCriteria.maxArea = this.searchCriteria.maxArea;
    }

    this.filtersChanged.emit(cleanCriteria);
  }
  updatePriceFromSlider(event: Event): void {
    const value = (event.target as HTMLInputElement).valueAsNumber;
    
    // Set the max price based on the slider value
    this.filterForm.get('maxPrice')!.setValue(value);
    
    // Set min price to 0 if not already set
    if (!this.filterForm.get('minPrice')!.value) {
      this.filterForm.get('minPrice')!.setValue(0);
    }
  }
  getFormattedMinPrice(): string { 
    return this.filterForm.get('minPrice')?.value ? 
      `${this.filterForm.get('minPrice')?.value} TND` : '0 TND'; 
  }
  getFormattedMaxPrice(): string { 
    return this.filterForm.get('maxPrice')?.value ? 
      `${this.filterForm.get('maxPrice')?.value} TND` : 'Any'; 
  }
  resetBudget(): void {
    this.filterForm.get('minPrice')!.setValue('');
    this.filterForm.get('maxPrice')!.setValue('');
    this.filterForm.get('priceRange')!.setValue(500); // Reset slider to a default
  }

  // Methods from HeaderComponent for dropdown and auth
  toggleDropdown(): void {
    this.isDropdownOpen = !this.isDropdownOpen;
    if (this.isDropdownOpen) {
      this.addClickListener();
    } else {
      this.removeClickListener();
    }
  }

  private addClickListener(): void {
    this.removeClickListener();
    
    setTimeout(() => {
      this.clickListener = this.renderer.listen('document', 'click', (event) => {
        if (event.defaultPrevented) {
            return;
        }

        // Simpler check: if the click is outside the auth section's relative container, close dropdown.
        // This assumes the auth dropdown button and menu are within a div with class '.auth-section .relative'
        const authSectionRelative = this.el.nativeElement.querySelector('.auth-section .relative');
        if (authSectionRelative && !authSectionRelative.contains(event.target)) {
          if (this.isDropdownOpen) { // Only act if auth dropdown is open
          this.isDropdownOpen = false;
          this.removeClickListener();
          }
        }
      });
    }, 10); // Small delay to prevent immediate closing due to the click that opened it.
  }

  private removeClickListener(): void {
    if (this.clickListener) {
      this.clickListener();
      // this.clickListener = null; // Set to null after removing
    }
  }

  logout(): void {
    this.isDropdownOpen = false;
    this.removeClickListener();
    this.authService.logout().subscribe({
      next: () => console.log('FilterBar: Logout successful'),
      error: (err) => console.error('FilterBar: Logout failed:', err)
    });
  }

  navigateToLogin(): void {
    this.isDropdownOpen = false; // Close dropdown before navigating
    this.removeClickListener();
    const currentUrl = this.router.url;
    localStorage.setItem('original_page', currentUrl);
    localStorage.setItem('user_initiated_login', 'true');
    this.router.navigate(['/auth/login']);
  }

  // Close price filter when another dropdown is clicked
  closePriceFilterIfOpen(): void {
    if (this.showPriceFilter) {
      this.showPriceFilter = false;
      if (this.closePriceFilterUnlisten) {
        this.closePriceFilterUnlisten();
        this.closePriceFilterUnlisten = null;
      }
    }
  }

  // Toggle methods for custom dropdowns
  toggleSortDropdown(): void {
    // Close other dropdowns first
    this.closePriceFilterIfOpen();
    this.showDistanceDropdown = false;
    
    // Toggle sort dropdown
    this.showSortDropdown = !this.showSortDropdown;
    
    if (this.showSortDropdown) {
      // Add a document click listener to close the dropdown when clicking outside
      setTimeout(() => {
        document.addEventListener('click', this.closeSortDropdown);
      });
    }
  }
  
  toggleDistanceDropdown(): void {
    // Close other dropdowns first
    this.closePriceFilterIfOpen();
    this.showSortDropdown = false;
    
    // Toggle distance dropdown
    this.showDistanceDropdown = !this.showDistanceDropdown;
    
    if (this.showDistanceDropdown) {
      // Add a document click listener to close the dropdown when clicking outside
      setTimeout(() => {
        document.addEventListener('click', this.closeDistanceDropdown);
      });
    }
  }
  
  // Close method for sort dropdown
  closeSortDropdown = (event?: MouseEvent): void => {
    if (event) {
      const dropdownElements = this.el.nativeElement.querySelector('.custom-dropdown-wrapper');
      if (dropdownElements && !dropdownElements.contains(event.target)) {
        this.showSortDropdown = false;
        document.removeEventListener('click', this.closeSortDropdown);
      }
    } else {
      this.showSortDropdown = false;
      document.removeEventListener('click', this.closeSortDropdown);
    }
  }
  
  // Close method for distance dropdown
  closeDistanceDropdown = (event?: MouseEvent): void => {
    if (event) {
      const dropdownElements = this.el.nativeElement.querySelector('.custom-dropdown-wrapper:nth-child(4)');
      if (dropdownElements && !dropdownElements.contains(event.target)) {
        this.showDistanceDropdown = false;
        document.removeEventListener('click', this.closeDistanceDropdown);
      }
    } else {
      this.showDistanceDropdown = false;
      document.removeEventListener('click', this.closeDistanceDropdown);
    }
  }
  
  // Method to select a sort option from custom dropdown
  selectCustomSort(value: string): void {
    this.searchForm.get('sortBy')?.setValue(value);
    this.sortChanged.emit(value);
    this.showSortDropdown = false;
    document.removeEventListener('click', this.closeSortDropdown);
  }
  
  // Method to select a distance option from custom dropdown
  selectCustomDistance(value: string): void {
    this.filterForm.get('radiusKm')?.setValue(value);
    this.applyFilters();
    this.showDistanceDropdown = false;
    document.removeEventListener('click', this.closeDistanceDropdown);
  }
  
  // Helper methods to get display labels
  getSortLabel(): string {
    const currentValue = this.searchForm.get('sortBy')?.value;
    if (!currentValue) return 'Sort';
    
    const selectedOption = this.sortOptions.find(option => option.value === currentValue);
    return selectedOption ? selectedOption.label : 'Sort';
  }
  
  getDistanceLabel(): string {
    const currentValue = this.filterForm.get('radiusKm')?.value;
    if (!currentValue) return 'Distance';
    
    return `${currentValue} km`;
  }

  // Toggle method for bedrooms dropdown
  toggleBedroomsDropdown(): void {
    // Close other dropdowns first
    this.closePriceFilterIfOpen();
    this.showSortDropdown = false;
    this.showDistanceDropdown = false;
    
    // Toggle bedrooms dropdown
    this.showBedroomsDropdown = !this.showBedroomsDropdown;
    
    if (this.showBedroomsDropdown) {
      // Add a document click listener to close the dropdown when clicking outside
      setTimeout(() => {
        document.addEventListener('click', this.closeBedroomsDropdown);
      });
    }
  }
  
  // Close method for bedrooms dropdown
  closeBedroomsDropdown = (event?: MouseEvent): void => {
    if (event) {
      const dropdownElements = this.el.nativeElement.querySelector('.custom-dropdown-wrapper:nth-child(5)');
      if (dropdownElements && !dropdownElements.contains(event.target)) {
        this.showBedroomsDropdown = false;
        document.removeEventListener('click', this.closeBedroomsDropdown);
      }
    } else {
      this.showBedroomsDropdown = false;
      document.removeEventListener('click', this.closeBedroomsDropdown);
    }
  }
  
  // Method to select a bedrooms option from custom dropdown
  selectCustomBedrooms(value: string): void {
    this.filterForm.get('bedrooms')?.setValue(value);
    this.applyFilters();
    this.showBedroomsDropdown = false;
    document.removeEventListener('click', this.closeBedroomsDropdown);
  }
  
  // Helper method to get bedrooms display label
  getBedroomsLabel(): string {
    const currentValue = this.filterForm.get('bedrooms')?.value;
    if (!currentValue) return 'Bedrooms';
    
    return currentValue === '4' ? '4+' : `${currentValue}`;
  }

  onSearchChange(): void {
    this.updateActiveFiltersCount();
    this.emitFilters();
  }

  onPropertyTypeChange(): void {
    this.updateActiveFiltersCount();
    this.emitFilters();
  }

  onPriceRangeChange(range: any): void {
    this.searchCriteria.minPrice = range.min;
    this.searchCriteria.maxPrice = range.max;
    this.updateActiveFiltersCount();
    this.emitFilters();
  }

  onAreaRangeChange(range: any): void {
    this.searchCriteria.minArea = range.min;
    this.searchCriteria.maxArea = range.max;
    this.updateActiveFiltersCount();
    this.emitFilters();
  }

  onBedroomsChange(): void {
    this.updateActiveFiltersCount();
    this.emitFilters();
  }

  onCustomPriceChange(): void {
    this.updateActiveFiltersCount();
    this.emitFilters();
  }

  onCustomAreaChange(): void {
    this.updateActiveFiltersCount();
    this.emitFilters();
  }

  toggleExpanded(): void {
    this.isExpanded = !this.isExpanded;
  }


}

