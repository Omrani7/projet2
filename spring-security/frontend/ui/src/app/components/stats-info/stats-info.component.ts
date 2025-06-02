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
  // Statistics data for the site
  happyStudents: string = '25K+'; // Students who found housing through our platform
  citiesCovered: number = 24; // Tunisia has 24 governorates
  averageResponseTime: string = '2h'; // Average response time
  partnerUniversities: number = 124; // Updated partner institute count

  constructor() { }

}
