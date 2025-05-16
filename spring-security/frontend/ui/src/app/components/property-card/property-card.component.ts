import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Property } from '../../models/property.model';

@Component({
  selector: 'app-property-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './property-card.component.html',
  styleUrl: './property-card.component.css'
})
export class PropertyCardComponent {
  @Input() property: Property | undefined;

  constructor() { }
}
