import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Property } from '../../models/property.model';
import { PropertyCardComponent } from '../property-card/property-card.component';
// import { PropertyService } from '../../services/property.service'; // We'll use this later

@Component({
  selector: 'app-recommended-list',
  standalone: true,
  imports: [CommonModule, PropertyCardComponent],
  templateUrl: './recommended-list.component.html',
  styleUrl: './recommended-list.component.css'
})
export class RecommendedListComponent implements OnInit {

  recommendedProperties: Property[] = [];

  // constructor(private propertyService: PropertyService) { } // Inject later
  constructor() { }

  ngOnInit(): void {
    this.fetchRecommendedProperties();
  }

  fetchRecommendedProperties(): void {
    // TODO: Replace with actual service call
    // For now, use sample data
    this.recommendedProperties = [
      {
        id: 'rec1',
        imageUrl: 'assets/sample-house-1.jpg', // Make sure you have sample images in assets
        title: 'Cozy Studio near ENSI',
        address: 'Manouba, Tunis',
        price: 550,
        currency: 'TND',
        beds: 1,
        baths: 1,
        area: 35,
        propertyType: 'Studio'
      },
      {
        id: 'rec2',
        imageUrl: 'assets/sample-house-2.jpg',
        title: 'Shared Apt - Central Location',
        address: 'Lafayette, Tunis',
        price: 400,
        currency: 'TND',
        beds: 3,
        baths: 1,
        area: 80,
        propertyType: 'Apartment'
      },
      {
        id: 'rec3',
        imageUrl: 'assets/sample-house-3.jpg',
        title: 'Modern S+1 - Great View',
        address: 'Les Berges du Lac 2, Tunis',
        price: 900,
        currency: 'TND',
        beds: 1,
        baths: 1,
        area: 60,
        propertyType: 'Apartment'
      },
      // Add more sample properties if needed
    ];
  }
}
