import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common'; // Needed for *ngFor, *ngIf
import { PropertyCardComponent } from '../property-card/property-card.component'; // Import the card component
import { Property } from '../../models/property.model';
// import { PropertyService } from '../../services/property.service'; // For future use

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
  isLoading: boolean = false; // To show a loading indicator later

  // constructor(private propertyService: PropertyService) { }
  constructor() { }

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
    // Simulate API call
    setTimeout(() => {
      // TODO: Replace with actual this.propertyService.searchProperties({ city: stateName })
      // Sample Data Generation (Replace with real logic)
      const sampleData: Property[] = [
        {
          id: `${stateName.toLowerCase()}-1`,
          imageUrl: `assets/sample-house-1.jpg`, // Use generic or state-specific images
          title: `Nice Apartment in ${stateName}`,
          address: `${stateName} Center`,
          price: Math.floor(Math.random() * 500) + 400, // Random price for demo
          currency: 'TND',
          beds: Math.floor(Math.random() * 3) + 1,
          baths: 1,
          area: Math.floor(Math.random() * 50) + 40
        },
        {
          id: `${stateName.toLowerCase()}-2`,
          imageUrl: `assets/sample-house-2.jpg`,
          title: `Studio near University (${stateName})`,
          address: `University Area, ${stateName}`,
          price: Math.floor(Math.random() * 300) + 300,
          currency: 'TND',
          beds: 1,
          baths: 1,
          area: Math.floor(Math.random() * 20) + 25
        }
        // Add more simulated properties
      ];
      this.propertiesForSelectedState = sampleData;
      this.isLoading = false;
      console.log(`Properties loaded for ${stateName}:`, this.propertiesForSelectedState);
    }, 500); // Simulate network delay
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
