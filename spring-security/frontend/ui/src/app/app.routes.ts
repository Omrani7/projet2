import { Routes } from '@angular/router';
import { LandingPageComponent } from './landing-page/landing-page.component'; // Restore LandingPageComponent
import { DiscoveryComponent } from './pages/discovery/discovery.component'; // Keep DiscoveryComponent for its own route

export const routes: Routes = [
  { path: '', component: LandingPageComponent }, // LandingPageComponent is the root
  { path: 'discovery', component: DiscoveryComponent }, // Route for DiscoveryComponent
  { path: 'auth', loadChildren: () => import('./auth/auth.module').then(m => m.AuthModule) },
  { path: '**', redirectTo: '' }
];
