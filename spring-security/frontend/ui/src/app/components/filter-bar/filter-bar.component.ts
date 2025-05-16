import { Component, EventEmitter, Output, OnDestroy, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { Observable, of, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, catchError, takeUntil, finalize } from 'rxjs/operators';

import { Institute} from "../../../../../src/app/models/institute.model";
import { InstituteService } from '../../services/institute.service';
import { PropertySearchCriteria } from '../../../../../src/app/models/property-search-criteria.model';

interface SortOption {
  value: string;
  label: string;
}

@Component({
  selector: 'app-filter-bar',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './filter-bar.component.html',
  styleUrls: ['./filter-bar.component.css']
})
export class FilterBarComponent implements OnDestroy, OnChanges {
  @Input() currentSelectedInstitute: Institute | null = null;
  @Output() instituteSelected = new EventEmitter<Institute | null>();
  @Output() sortChanged = new EventEmitter<string>();
  @Output() filtersChanged = new EventEmitter<PropertySearchCriteria>();

  searchForm: FormGroup;
  filteredInstitutes$: Observable<Institute[]> = of([]);
  isLoadingInstitutes = false;
  showInstitutesDropdown = false;
  showFilterModal = false;

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

  constructor(
    private fb: FormBuilder,
    private instituteService: InstituteService
  ) {
    this.searchForm = this.fb.group({
      instituteQuery: [''],
      sortBy: [this.defaultSortOptions[0].value]
    });

    this.filterForm = this.fb.group({
      propertyType: [''],
      minPrice: [''],
      maxPrice: [''],
      bedrooms: [''],
      radiusKm: [3]
    });

    this.updateSortOptions();

    // Subscribe to instituteQuery changes for autocomplete
    this.searchForm.get('instituteQuery')!.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(query => {
        // Only search if the query is different from the selected institute's name
        // or if no institute is selected or the query is manually changed.
        if (this.currentSelectedInstitute && query === this.currentSelectedInstitute.name) {
          this.showInstitutesDropdown = false; // Keep dropdown hidden if query matches selected name
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

    // Subscribe to sortBy changes
    this.searchForm.get('sortBy')!.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (value) {
        this.sortChanged.emit(value);
      }
    });
  }

  toggleFilterModal(): void {
    this.showFilterModal = !this.showFilterModal;
  }

  applyFilters(): void {
    const filters: PropertySearchCriteria = {};

    // Get values from filter form
    const propertyType = this.filterForm.get('propertyType')?.value;
    const minPrice = this.filterForm.get('minPrice')?.value;
    const maxPrice = this.filterForm.get('maxPrice')?.value;
    const bedrooms = this.filterForm.get('bedrooms')?.value;
    const radiusKm = this.filterForm.get('radiusKm')?.value;

    // Only include non-empty values
    if (propertyType) filters.propertyType = propertyType;
    if (minPrice) filters.minPrice = parseFloat(minPrice);
    if (maxPrice) filters.maxPrice = parseFloat(maxPrice);
    if (bedrooms) filters.bedrooms = parseInt(bedrooms, 10);
    if (radiusKm) filters.radiusKm = parseFloat(radiusKm);

    // If an institute is selected, include its ID
    if (this.currentSelectedInstitute) {
      filters.instituteId = this.currentSelectedInstitute.id;
    }

    // Emit the filters
    this.filtersChanged.emit(filters);
    this.showFilterModal = false;
  }

  clearFilters(): void {
    this.filterForm.reset({
      propertyType: '',
      minPrice: '',
      maxPrice: '',
      bedrooms: '',
      radiusKm: 3
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['currentSelectedInstitute']) {
      const newInstitute = changes['currentSelectedInstitute'].currentValue;
      // Update the search input field if a new institute is selected (e.g., from URL params)
      // but only if the new value is different to prevent feedback loops if user is typing.
      if (newInstitute && this.searchForm.get('instituteQuery')!.value !== newInstitute.name) {
        this.searchForm.get('instituteQuery')!.setValue(newInstitute.name, { emitEvent: false });
      } else if (!newInstitute && this.searchForm.get('instituteQuery')!.value) {
        // If institute is cleared externally, and input has text, clear it (optional)
        // this.searchForm.get('instituteQuery')!.setValue('', { emitEvent: false });
      }

      this.updateSortOptions();
      const sortByControl = this.searchForm.get('sortBy');
      if (!this.currentSelectedInstitute && sortByControl?.value === this.distanceSortOption.value) {
        sortByControl.setValue(this.defaultSortOptions[0].value);
      }
    }
  }

  updateSortOptions(): void {
    if (this.currentSelectedInstitute) {
      // Check if distance sort is already present to avoid duplicates
      if (!this.defaultSortOptions.find(opt => opt.value === this.distanceSortOption.value)) {
        this.sortOptions = [...this.defaultSortOptions, this.distanceSortOption];
      } else {
        this.sortOptions = [...this.defaultSortOptions]; // Should already include it if logic is maintained
      }
    } else {
      this.sortOptions = [...this.defaultSortOptions.filter(opt => opt.value !== this.distanceSortOption.value)];
    }
     // Ensure distance option is correctly added or removed
    const hasDistanceOption = this.sortOptions.some(opt => opt.value === this.distanceSortOption.value);
    if (this.currentSelectedInstitute && !hasDistanceOption) {
      this.sortOptions.push(this.distanceSortOption);
    } else if (!this.currentSelectedInstitute && hasDistanceOption) {
      this.sortOptions = this.sortOptions.filter(opt => opt.value !== this.distanceSortOption.value);
    }
  }

  selectInstitute(institute: Institute): void {
    this.searchForm.get('instituteQuery')!.setValue(institute.name, { emitEvent: false });
    this.instituteSelected.emit(institute);
    this.showInstitutesDropdown = false;
  }

  clearInstituteSearch(): void {
    this.searchForm.get('instituteQuery')!.setValue('', { emitEvent: true }); // emit event to clear results
    this.instituteSelected.emit(null);
    this.showInstitutesDropdown = false;
  }

  onFocusInstitute(): void {
    const currentQuery = this.searchForm.get('instituteQuery')!.value;
    if (currentQuery && currentQuery.trim().length > 1) {
        this.filteredInstitutes$.pipe(takeUntil(this.destroy$)).subscribe(institutes => {
            if(institutes.length > 0){
                this.showInstitutesDropdown = true;
            }
        });
    }
  }

  onBlurInstitute(): void {
    // Delay hiding to allow click on dropdown item
    setTimeout(() => {
        if (!document.activeElement?.closest('.institutes-dropdown')) { // Check if focus is still within dropdown
            this.showInstitutesDropdown = false;
        }
    }, 200);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
