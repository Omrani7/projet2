import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-stats-info',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './stats-info.component.html',
  styleUrl: './stats-info.component.css'
})
export class StatsInfoComponent {
  // Sample data - replace with real data source later if needed
  totalBeds: string = '10K+'; // Example value
  citiesCovered: number = 24; // Tunisia has 24 governorates
  studentsHelped: string = '5K+'; // Example value
  partnerUniversities: number = 50; // Example value

  constructor() { }

}
