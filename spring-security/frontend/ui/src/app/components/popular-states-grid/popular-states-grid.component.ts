import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { PopularState } from '../../models/popular-state.model';

@Component({
  selector: 'app-popular-states-grid',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './popular-states-grid.component.html',
  styleUrl: './popular-states-grid.component.css'
})
export class PopularStatesGridComponent implements OnInit {

  popularStates: PopularState[] = [];

  constructor() { }

  ngOnInit(): void {
    this.fetchPopularStates();
  }

  fetchPopularStates(): void {
    // TODO: Replace with actual data/service call if needed
    this.popularStates = [
      {
        id: 'tunis',
        name: 'Tunis',
        imageUrl: 'assets/tunis-state.jpg',
        listingCount: 120 // Placeholder
      },
      {
        id: 'sfax',
        name: 'Sfax',
        imageUrl: 'assets/sfax-state.jpg',
        listingCount: 85 // Placeholder
      },
      {
        id: 'sousse',
        name: 'Sousse',
        imageUrl: 'assets/sousse-state.jpg',
        listingCount: 95 // Placeholder
      },
      {
        id: 'tozeur',
        name: 'Tozeur',
        imageUrl: 'assets/tozeur-state.jpg',
        listingCount: 70 // Placeholder
      },
      {
        id: 'nabeul',
        name: 'Nabeul',
        imageUrl: 'assets/nabeul-state.jpg',
        listingCount: 60 // Placeholder
      },
       {
        id: 'zaghouan',
        name: 'Zaghouan',
        imageUrl: "assets/zaghouan-state.jpg",
        listingCount: 75 // Placeholder
      },
      // Add more states as desired
    ];
  }

}
