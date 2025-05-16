import { Component, Output, EventEmitter } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-hero-section',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './hero-section.component.html',
  styleUrls: ['./hero-section.component.css']
})
export class HeroSectionComponent {
  @Output() searchSubmitted = new EventEmitter<string>();
  heroSearchForm: FormGroup;

  constructor(private fb: FormBuilder) {
    this.heroSearchForm = this.fb.group({
      query: ['']
    });
  }

  onHeroSearch(): void {
    if (this.heroSearchForm.valid) {
      const searchQuery = this.heroSearchForm.value.query;
      if (searchQuery && searchQuery.trim() !== '') {
        this.searchSubmitted.emit(searchQuery.trim());
      }
    }
  }
}
