import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common'; // Needed for *ngFor, *ngIf
import { PropertyCardComponent } from '../property-card/property-card.component'; // Import the card component
import { Property } from '../../models/property.model';
import { PropertyServiceService } from '../../services/property.service.service'; // Correct import for PropertyService

@Component({
  selector: 'app-state-specific-listings',
  standalone: true,
  imports: [
    CommonModule, // Add CommonModule
    PropertyCardComponent // Add PropertyCardComponent
  ],
  templateUrl: './state-specific-listings.component.html',
  styleUrls: ['./state-specific-listings.component.css'] // Corrected from styleUrl
})
export class StateSpecificListingsComponent implements OnInit {

  @ViewChild('cityFiltersContainer') cityFiltersContainer!: ElementRef<HTMLDivElement>;

  tunisianStates: string[] = [
    'Ariana', 'Béja', 'Ben Arous', 'Bizerte', 'Gabès', 'Gafsa', 'Jendouba', 'Kairouan',
    'Kasserine', 'Kébili', 'Kef', 'Mahdia', 'Manouba', 'Médenine', 'Monastir', 'Nabeul',
    'Sfax', 'Sidi Bouzid', 'Siliana', 'Sousse', 'Tataouine', 'Tozeur', 'Tunis', 'Zaghouan'
  ];
  selectedState: string | null = 'Monastir';
  propertiesForSelectedState: Property[] = [];
  isLoading: boolean = false; // To show a loading indicator

  constructor(private propertyService: PropertyServiceService) { }

  ngOnInit(): void {
    if (this.selectedState) {
      this.fetchPropertiesForState(this.selectedState);
    }
  }

  selectState(stateName: string): void {
    console.log(`State selected: ${stateName}`);
    if (this.selectedState === stateName) {
      return; // Avoid reloading if the same state is clicked
    }
    this.selectedState = stateName;
    this.propertiesForSelectedState = []; // Clear previous results
    this.fetchPropertiesForState(stateName);
  }

  fetchPropertiesForState(stateName: string): void {
    this.isLoading = true;
    console.log(`Fetching properties for: ${stateName}`);
    
    // Call the real service to get properties by city
    this.propertyService.getPropertiesByCity(stateName).subscribe({
      next: (properties: Property[]) => {
        this.propertiesForSelectedState = properties;
        this.isLoading = false;
        console.log(`Properties loaded for ${stateName}:`, this.propertiesForSelectedState);
      },
      error: (error: any) => {
        console.error(`Error fetching properties for ${stateName}:`, error);
        this.isLoading = false;
        this.propertiesForSelectedState = []; // Clear on error
      }
    });
  }

  scrollCities(direction: number): void {
    const container = this.cityFiltersContainer.nativeElement;
    const scrollAmount = container.clientWidth * 0.8;
    container.scrollBy({
      left: scrollAmount * direction,
      behavior: 'smooth'
    });
  }
}
