import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { RecommendationService, UserWithScore } from '../../services/recommendation.service';

@Component({
  selector: 'app-compatible-students',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './compatible-students.component.html',
  styleUrl: './compatible-students.component.css'
})
export class CompatibleStudentsComponent implements OnInit {
  compatibleStudents: UserWithScore[] = [];
  isLoading = true;
  error: string | null = null;

  constructor(
    private recommendationService: RecommendationService
  ) {}

  ngOnInit() {
    this.loadCompatibleStudents();
  }

  loadCompatibleStudents() {
    this.isLoading = true;
    this.error = null;

    this.recommendationService.getCompatibleStudents(15).subscribe({
      next: (students) => {
        this.compatibleStudents = students;
        this.isLoading = false;
        console.log('Loaded compatible students:', students);
      },
      error: (error) => {
        console.error('Error loading compatible students:', error);
        this.error = 'Failed to load compatible students. Please try again.';
        this.isLoading = false;
      }
    });
  }

  getCompatibilityClass(score: number): string {
    if (score >= 90) return 'compatibility-excellent';
    if (score >= 75) return 'compatibility-very-good';
    if (score >= 60) return 'compatibility-good';
    if (score >= 40) return 'compatibility-fair';
    return 'compatibility-poor';
  }

  getCompatibilityColor(score: number): string {
    if (score >= 90) return '#22c55e'; // green
    if (score >= 75) return '#3b82f6'; // blue
    if (score >= 60) return '#f59e0b'; // yellow
    if (score >= 40) return '#f97316'; // orange
    return '#ef4444'; // red
  }

  sendMessage(student: UserWithScore) {
    // TODO: Implement messaging functionality
    console.log('Send message to:', student.user.username);
    // This could open a message modal or navigate to a conversation
  }

  viewProfile(student: UserWithScore) {
    // TODO: Implement profile viewing
    console.log('View profile of:', student.user.username);
    // This could open a profile modal or navigate to profile page
  }

  refreshRecommendations() {
    this.loadCompatibleStudents();
  }

  trackByStudentId(index: number, student: UserWithScore): number {
    return student.user.id;
  }

  getCompatibilityLevel(score: number): string {
    if (score >= 90) return 'Excellent';
    if (score >= 75) return 'Very Good';
    if (score >= 60) return 'Good';
    if (score >= 40) return 'Fair';
    return 'Poor';
  }
} 