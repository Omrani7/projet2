import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-access-denied-owner-feature',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './access-denied-owner-feature.component.html',
  styleUrls: ['./access-denied-owner-feature.component.css']
})
export class AccessDeniedOwnerFeatureComponent {
  // For the screenshot, the email is displayed.
  // We can fetch the current user's email if needed, or use a generic message.
  // For now, let's assume we might want to display it.
  // This would require injecting AuthService and getting user details.
  // For simplicity in this step, I'll make the message generic first, 
  // then consider adding email if it's straightforward.
  userEmail: string | null = null; // Placeholder for email

  constructor() {
    // TODO: If email is needed, inject AuthService and get it.
    // Example: this.userEmail = authService.getCurrentUser()?.email;
  }
} 