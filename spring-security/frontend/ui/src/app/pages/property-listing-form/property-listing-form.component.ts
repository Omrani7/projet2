import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { DomSanitizer } from '@angular/platform-browser';
import { AuthService } from '../../auth/auth.service';
import { PropertyListingService } from '../../services/property-listing.service';
import { InstituteService } from '../../services/institute.service';
import { Institute } from '../../models/institute.model';
import { PropertyTypes } from '../../models/property-types.enum';
import { Observable, of } from 'rxjs';
import { map, startWith } from 'rxjs/operators';

@Component({
  selector: 'app-property-listing-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './property-listing-form.component.html',
  styleUrls: ['./property-listing-form.component.css']
})
export class PropertyListingFormComponent implements OnInit {
  // Form steps
  currentStep = 1;
  totalSteps = 4;
  stepTitles = [
    'Basic Information',
    'Property Details',
    'Pricing & Availability',
    'Media Gallery'
  ];

  // Form group for each step
  basicInfoForm!: FormGroup;
  detailsForm!: FormGroup;
  pricingForm!: FormGroup;
  mediaForm!: FormGroup;

  // Property types
  propertyTypes = Object.values(PropertyTypes);

  // Amenities
  amenities = [
    'Wifi', 'Furniture', 'Air Conditioning', 'Heating', 'Washing Machine', 
    'Dishwasher', 'TV', 'Parking', 'Elevator', 'Security', 'Gym', 
    'Swimming Pool', 'Balcony', 'Garden', 'Pets Allowed'
  ];

  // File handling
  selectedFiles: File[] = [];
  previewUrls: any[] = [];
  existingImageUrls: string[] = [];
  uploadProgress = 0;
  dragAreaClass = 'dragarea';
  isSubmitting = false;
  formSubmitError = '';
  formSubmitSuccess = '';

  // Edit mode
  isEditMode = false;
  propertyIdForEdit: number | null = null;

  // API URL
  private apiUrl = 'http://localhost:8080/api/v1/properties';

  institutes: Institute[] = [];
  filteredInstitutes: Observable<Institute[]>;
  paymentFrequencies = ['Monthly', 'Quarterly', 'Yearly'];

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private http: HttpClient,
    private sanitizer: DomSanitizer,
    private authService: AuthService,
    private propertyService: PropertyListingService,
    private instituteService: InstituteService
  ) {
    this.basicInfoForm = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(100)]],
      description: ['', [Validators.required, Validators.minLength(20), Validators.maxLength(2000)]],
      fullAddress: ['', [Validators.required, Validators.minLength(10)]],
      city: ['', Validators.required],
      district: ['', Validators.required],
      nearbyInstitute: ['']
    });

    this.detailsForm = this.fb.group({
      propertyType: ['', Validators.required],
      rooms: [1, [Validators.required, Validators.min(1)]],
      bedrooms: [1, [Validators.required, Validators.min(0)]],
      bathrooms: [1, [Validators.required, Validators.min(0)]],
      area: [null, [Validators.required, Validators.min(1)]],
      selectedAmenities: [[]],
      hasBalcony: [false],
      floor: [0]
    });

    this.pricingForm = this.fb.group({
      price: [null, [Validators.required, Validators.min(1)]],
      securityDeposit: [null],
      availableFrom: [this.formatDate(new Date()), Validators.required],
      availableTo: [''],
      paymentFrequency: ['monthly', Validators.required],
      minimumStayMonths: [1, [Validators.required, Validators.min(1)]],
      contactInfo: ['', [Validators.required, Validators.pattern(/^[0-9+\-\s()]{8,15}$/)]]
    }, { validators: dateRangeValidator() });

    this.mediaForm = this.fb.group({
      // images: [[]] // This was for file input. For edit, might show existing images.
    });

    this.filteredInstitutes = this.basicInfoForm.get('nearbyInstitute')!.valueChanges.pipe(
      startWith(''),
      map(value => this._filterInstitutes(value || ''))
    );
  }

  ngOnInit(): void {
    this.initForms();
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.propertyIdForEdit = +id;
        this.loadPropertyForEdit(this.propertyIdForEdit);
      }
    });
    this.loadInstitutes();
  }

  private initForms(): void {
    // Basic Information form
    this.basicInfoForm = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(100)]],
      description: ['', [Validators.required, Validators.minLength(20), Validators.maxLength(2000)]],
      fullAddress: ['', [Validators.required, Validators.minLength(10)]],
      city: ['', Validators.required],
      district: ['', Validators.required],
      nearbyInstitute: [''],
    });

    // Details form
    this.detailsForm = this.fb.group({
      propertyType: ['', Validators.required],
      rooms: [1, [Validators.required, Validators.min(1)]],
      bedrooms: [1, [Validators.required, Validators.min(0)]],
      bathrooms: [1, [Validators.required, Validators.min(0)]],
      area: [null, [Validators.required, Validators.min(1)]],
      selectedAmenities: [[]],
      hasBalcony: [false],
      floor: [0]
    });

    // Pricing form
    this.pricingForm = this.fb.group({
      price: [null, [Validators.required, Validators.min(1)]],
      securityDeposit: [null],
      availableFrom: [this.formatDate(new Date()), Validators.required],
      availableTo: [''],
      paymentFrequency: ['monthly', Validators.required],
      minimumStayMonths: [1, [Validators.required, Validators.min(1)]],
      contactInfo: ['', [Validators.required, Validators.pattern(/^[0-9+\-\s()]{8,15}$/)]]
    }, { validators: dateRangeValidator() });

    // Media form
    this.mediaForm = this.fb.group({
      // images: [[]] // This was for file input. For edit, might show existing images.
    });
  }

  loadPropertyForEdit(id: number): void {
    this.propertyService.getPropertyById(id).subscribe({
      next: (property) => {
        this.basicInfoForm.patchValue({
          title: property.title,
          description: property.description,
          fullAddress: property.fullAddress,
          city: property.city,
          district: property.district,
          nearbyInstitute: null // We don't have this in the PropertyListingDTO
        });
        this.detailsForm.patchValue({
          propertyType: property.propertyType,
          rooms: property.rooms,
          bedrooms: property.bedrooms,
          bathrooms: property.bathrooms,
          area: property.area,
          selectedAmenities: property.amenities || [],
          hasBalcony: property.hasBalcony,
          floor: property.floor
        });
        this.pricingForm.patchValue({
          price: property.price,
          securityDeposit: property.securityDeposit,
          availableFrom: this.formatDate(new Date()), // Default to today
          availableTo: property.availableTo ? this.formatDate(new Date(property.availableTo)) : '',
          paymentFrequency: property.paymentFrequency,
          minimumStayMonths: property.minimumStayMonths,
          contactInfo: property.contactInfo
        });
        this.existingImageUrls = property.imageUrls || [];
      },
      error: (err) => {
        console.error('Error loading property for edit:', err);
        this.formSubmitError = 'Could not load property details for editing.';
      }
    });
  }

  // Format date to YYYY-MM-DD for date inputs
  private formatDate(date: Date | string): string {
    if (!date) return '';
    const d = new Date(date); // Handles both Date objects and date strings
    const year = d.getFullYear();
    const month = (d.getMonth() + 1).toString().padStart(2, '0');
    const day = d.getDate().toString().padStart(2, '0');
    if (isNaN(year)) return ''; // Handle invalid date parsing
    return `${year}-${month}-${day}`;
  }

  // Navigation between steps
  nextStep(): void {
    if (this.currentStep < this.totalSteps) {
      if (this.validateCurrentStep()) {
        this.currentStep++;
      }
    }
  }

  prevStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  // Validate current step before proceeding
  validateCurrentStep(): boolean {
    switch (this.currentStep) {
      case 1:
        return this.basicInfoForm.valid;
      case 2:
        return this.detailsForm.valid;
      case 3:
        return this.pricingForm.valid;
      case 4:
        return true; // Media is optional
      default:
        return false;
    }
  }

  // File handling methods
  onFileChange(event: any): void {
    const files = event.target.files;
    this.processFiles(files);
  }

  onDragOver(event: any): void {
    event.preventDefault();
    this.dragAreaClass = 'dragarea-hover';
  }

  onDragLeave(event: any): void {
    event.preventDefault();
    this.dragAreaClass = 'dragarea';
  }

  onDrop(event: any): void {
    event.preventDefault();
    this.dragAreaClass = 'dragarea';
    const files = event.dataTransfer.files;
    this.processFiles(files);
  }

  processFiles(files: FileList): void {
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      
      // Check if file is an image
      if (!file.type.match('image.*')) {
        continue;
      }

      // Add to selected files
      this.selectedFiles.push(file);

      // Create preview
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.previewUrls.push(
          this.sanitizer.bypassSecurityTrustUrl(e.target.result)
        );
      };
      reader.readAsDataURL(file);
    }
  }

  removeNewImage(index: number): void {
    this.selectedFiles.splice(index, 1);
    this.previewUrls.splice(index, 1);
  }

  // Submit the form
  submitForm(): void {
    if (!this.validateAllForms()) {
      this.formSubmitError = 'Please correct the errors in the form before submitting.';
      return;
    }

    this.isSubmitting = true;
    this.formSubmitError = '';
    this.formSubmitSuccess = '';

    // Consolidate data from all form groups
    const propertyData: any = {
      title: this.basicInfoForm.get('title')?.value,
      description: this.basicInfoForm.get('description')?.value,
      fullAddress: this.basicInfoForm.get('fullAddress')?.value,
      city: this.basicInfoForm.get('city')?.value,
      district: this.basicInfoForm.get('district')?.value,
      propertyType: this.detailsForm.get('propertyType')?.value,
      rooms: Number(this.detailsForm.get('rooms')?.value) || 0,
      bedrooms: Number(this.detailsForm.get('bedrooms')?.value) || 0,
      bathrooms: Number(this.detailsForm.get('bathrooms')?.value) || 0,
      area: Number(this.detailsForm.get('area')?.value) || 0,
      floor: Number(this.detailsForm.get('floor')?.value) ?? 0,
      hasBalcony: this.detailsForm.get('hasBalcony')?.value || false,
      amenities: this.detailsForm.get('selectedAmenities')?.value || [],
      price: Number(this.pricingForm.get('price')?.value) || 0,
      securityDeposit: Number(this.pricingForm.get('securityDeposit')?.value) || null,
      availableFrom: this.pricingForm.get('availableFrom')?.value,
      availableTo: this.pricingForm.get('availableTo')?.value || null,
      paymentFrequency: this.pricingForm.get('paymentFrequency')?.value,
      minimumStayMonths: Number(this.pricingForm.get('minimumStayMonths')?.value) || 1,
      contactInfo: this.pricingForm.get('contactInfo')?.value,
    };

    // Setup auth headers
    const token = this.authService.getToken();
    let headers = new HttpHeaders();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    if (this.isEditMode && this.propertyIdForEdit) {
      // UPDATE LOGIC - Now also using FormData
      const formData = new FormData();
      formData.append('listingData', JSON.stringify(propertyData));

      // Add any new images
      if (this.selectedFiles.length > 0) {
        this.selectedFiles.forEach(file => {
          formData.append('images', file, file.name);
        });
      }

      console.log('Submitting FormData for UPDATE...');
      // Use multipart endpoint
      this.http.put(`${this.apiUrl}/owner/${this.propertyIdForEdit}`, formData, { headers: headers }).subscribe({
        next: (response) => {
          this.isSubmitting = false;
          this.formSubmitSuccess = 'Property updated successfully!';
          this.selectedFiles = [];
          this.previewUrls = [];
          setTimeout(() => {
            this.router.navigate(['/owner/my-listings']);
          }, 2000);
        },
        error: (err) => this.handleFormError(err, 'update')
      });
    } else {
      // CREATE LOGIC with FormData
      const formData = new FormData();
      
      // Use correct property name that matches the controller
      formData.append('listingData', JSON.stringify(propertyData));

      if (this.selectedFiles.length > 0) {
        this.selectedFiles.forEach(file => {
          formData.append('images', file, file.name);
        });
      }

      console.log('Submitting FormData for CREATE...');
      this.http.post(this.apiUrl, formData, { headers: headers }).subscribe({
        next: (response) => {
          this.isSubmitting = false;
          this.formSubmitSuccess = 'Property listing created successfully!';
          this.selectedFiles = [];
          this.previewUrls = [];
          // Reset forms or navigate
          setTimeout(() => {
            this.router.navigate(['/owner/my-listings']);
          }, 2000);
        },
        error: (err) => this.handleFormError(err, 'create')
      });
    }
  }

  handleFormError(err: any, operation: 'create' | 'update'): void {
    this.isSubmitting = false;
    console.error(`Error ${operation} property listing:`, err);
    if (err.status === 401 || err.status === 403) {
      this.formSubmitError = 'Authentication error. Please log in again.';
      setTimeout(() => {
        this.router.navigate(['/auth/login']);
      }, 2000);
    } else if (err.status === 400) {
      this.formSubmitError = err.error?.message || err.error?.errorDescription || `Invalid property data. Please check your form.`;
    } else {
      this.formSubmitError = `Error ${operation} listing. Please try again.`;
    }
  }

  validateAllForms(): boolean {
    return this.basicInfoForm.valid && this.detailsForm.valid && this.pricingForm.valid;
  }

  // Helper methods for form validation
  hasError(form: FormGroup, controlName: string, errorName: string): boolean {
    const control = form.get(controlName);
    return control ? control.hasError(errorName) && (control.dirty || control.touched) : false;
  }

  /**
   * Check if the form has date range validation error
   */
  hasDateRangeError(form: FormGroup): boolean {
    const availableToControl = form.get('availableTo');
    return form.hasError('dateRangeInvalid') && !!(availableToControl?.dirty || availableToControl?.touched);
  }

  /**
   * Get minimum date for availableTo input (should be after availableFrom)
   */
  get minAvailableToDate(): string {
    const availableFromValue = this.pricingForm.get('availableFrom')?.value;
    if (availableFromValue) {
      // Add one day to availableFrom to make availableTo minimum the next day
      const fromDate = new Date(availableFromValue);
      fromDate.setDate(fromDate.getDate() + 1);
      return this.formatDate(fromDate);
    }
    return '';
  }

  toggleAmenity(amenity: string): void {
    const selectedAmenities = [...this.detailsForm.get('selectedAmenities')?.value || []];
    const index = selectedAmenities.indexOf(amenity);
    
    if (index === -1) {
      selectedAmenities.push(amenity);
    } else {
      selectedAmenities.splice(index, 1);
    }
    
    this.detailsForm.get('selectedAmenities')?.setValue(selectedAmenities);
  }

  isAmenitySelected(amenity: string): boolean {
    const selectedAmenities = this.detailsForm.get('selectedAmenities')?.value || [];
    return selectedAmenities.includes(amenity);
  }

  // Cancel and return to dashboard
  cancelForm(): void {
    this.router.navigate(['/owner/dashboard']);
  }

  private loadInstitutes(): void {
    this.instituteService.getAllInstitutes().subscribe({
      next: (data: Institute[]) => {
        this.institutes = data;
      },
      error: (error: any) => {
        console.error('Error loading institutes:', error);
      }
    });
  }

  private _filterInstitutes(value: string | Institute): Institute[] {
    const filterValue = typeof value === 'string' ? value.toLowerCase() : value.name.toLowerCase();
    return this.institutes.filter(institute => institute.name.toLowerCase().includes(filterValue));
  }

  displayInstituteFn(institute: Institute): string {
    return institute && institute.name ? institute.name : '';
  }

  // Show error message (replaces dialog-based approach)
  showMessage(title: string, message: string): void {
    this.formSubmitError = message;
    console.log(`${title}: ${message}`);
  }

  // Show success message
  showSuccessAndNavigate(message: string): void {
    this.formSubmitSuccess = message;
    setTimeout(() => {
      this.router.navigate(['/owner/my-listings']);
    }, 2000);
  }
}

// Custom validator for date range
function dateRangeValidator(): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const availableFromControl = group.get('availableFrom');
    const availableToControl = group.get('availableTo');
    
    if (!availableFromControl || !availableToControl) {
      return null;
    }
    
    const availableFrom = availableFromControl.value;
    const availableTo = availableToControl.value;
    
    // If availableTo is empty, it's valid (no end date)
    if (!availableTo) {
      return null;
    }
    
    // If both dates are provided, check if availableTo is after availableFrom
    if (availableFrom && availableTo) {
      const fromDate = new Date(availableFrom);
      const toDate = new Date(availableTo);
      
      if (toDate <= fromDate) {
        return { dateRangeInvalid: true };
      }
    }
    
    return null;
  };
}
