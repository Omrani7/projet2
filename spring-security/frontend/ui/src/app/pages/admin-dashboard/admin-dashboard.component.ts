import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AdminService, AdminStats, SystemHealth, AdminUser } from '../../services/admin.service';
import { AuthService } from '../../auth/auth.service';
import { Subscription, interval, Observable } from 'rxjs';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.css'
})
export class AdminDashboardComponent implements OnInit, OnDestroy {
  
  // Dashboard data
  stats: AdminStats | null = null;
  systemHealth: SystemHealth | null = null;
  recentUsers: AdminUser[] = [];
  
  // UI state
  isLoading = true;
  currentTime = new Date();
  activeTab = 'overview';
  refreshInterval = 1800; // seconds (30 minutes instead of 30 seconds)
  
  // Chart data
  userGrowthData: any = null;
  activityData: any = null;
  
  // Make Math available in template
  Math = Math;
  
  private subscriptions: Subscription[] = [];
  
  // Export reports functionality
  showExportModal = false;
  isExporting = false;
  selectedExportType = 'users';
  selectedExportFormat = 'csv';
  
  exportTypes = [
    { value: 'users', label: 'Users Report', icon: '👥', description: 'Export all user data and statistics' },
    { value: 'properties', label: 'Properties Report', icon: '🏠', description: 'Export property listings and details' },
    { value: 'inquiries', label: 'Inquiries Report', icon: '💬', description: 'Export inquiry data and responses' },
    { value: 'announcements', label: 'Roommate Announcements', icon: '🤝', description: 'Export roommate announcement data' },
    { value: 'analytics', label: 'System Analytics', icon: '📊', description: 'Export platform analytics and metrics' }
  ];
  
  exportFormats = [
    { value: 'csv', label: 'CSV Format', description: 'Comma-separated values (Excel compatible)' },
    { value: 'excel', label: 'Excel Format', description: 'Microsoft Excel format (coming soon)' }
  ];

  // Toast notification properties
  showScraperStartToast = false;
  showScraperSuccessToast = false;
  showScraperErrorToast = false;
  activeScraperType = '';
  lastCompletedScraperType = '';
  lastScrapedCount = 0;
  scraperErrorMessage = '';
  private scraperPollingSubscription?: Subscription;
  private lastKnownScrapedCount = 0;
  
  constructor(
    private adminService: AdminService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    this.checkAdminAccess();
    this.loadDashboardData();
    this.setupAutoRefresh();
    this.updateCurrentTime();
  }

  ngOnDestroy() {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    if (this.scraperPollingSubscription) {
      this.scraperPollingSubscription.unsubscribe();
    }
  }

  private checkAdminAccess() {
    const userRole = this.authService.getUserRole();
    if (userRole !== 'ADMIN') {
      console.error('Access denied: Admin role required');
      this.router.navigate(['/']);
      return;
    }
  }

  private loadDashboardData() {
    this.isLoading = true;
    
    // Load overview stats
    this.subscriptions.push(
      this.adminService.getOverviewStats().subscribe({
        next: (stats) => {
          this.stats = stats;
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error loading admin stats:', error);
          this.isLoading = false;
        }
      })
    );

    // Load system health
    this.subscriptions.push(
      this.adminService.getSystemHealth().subscribe({
        next: (health) => {
          this.systemHealth = health;
        },
        error: (error) => {
          console.error('Error loading system health:', error);
        }
      })
    );

    // Load growth data
    this.subscriptions.push(
      this.adminService.getUserGrowthStats(30).subscribe({
        next: (data) => {
          this.userGrowthData = data;
        },
        error: (error) => {
          console.error('Error loading user growth data:', error);
        }
      })
    );

    // Load activity data
    this.subscriptions.push(
      this.adminService.getActivityStats(7).subscribe({
        next: (data) => {
          this.activityData = data;
        },
        error: (error) => {
          console.error('Error loading activity data:', error);
        }
      })
    );

    // Load recent users
    this.subscriptions.push(
      this.adminService.getAllUsers(0, 5, 'id', 'desc').subscribe({
        next: (response) => {
          this.recentUsers = response.content;
        },
        error: (error) => {
          console.error('Error loading recent users:', error);
        }
      })
    );
  }

  private setupAutoRefresh() {
    // Refresh every 30 minutes
    this.subscriptions.push(
      interval(this.refreshInterval * 1000).subscribe(() => {
        this.loadDashboardData();
      })
    );
  }

  private updateCurrentTime() {
    setInterval(() => {
      this.currentTime = new Date();
    }, 1000);
  }

  // Navigation methods
  switchTab(tab: string) {
    switch (tab) {
      case 'users':
        this.router.navigate(['/admin/users']);
        break;
      case 'properties':
        this.router.navigate(['/admin/properties']);
        break;
      case 'roommate-announcements':
        this.router.navigate(['/admin/roommate-announcements']);
        break;
      case 'system':
        // Future system management page
        console.log('System management coming soon');
        break;
      default:
        console.log('Unknown tab:', tab);
    }
  }

  // Action methods
  refreshData() {
    this.loadDashboardData();
  }

  clearCache() {
    this.subscriptions.push(
      this.adminService.clearSystemCache().subscribe({
        next: (response) => {
          console.log('Cache cleared successfully:', response);
          alert('System cache cleared successfully!');
        },
        error: (error) => {
          console.error('Error clearing cache:', error);
          alert('Error clearing cache: ' + error.message);
        }
      })
    );
  }

  openExportModal() {
    this.showExportModal = true;
  }

  closeExportModal() {
    this.showExportModal = false;
    this.selectedExportType = 'users';
    this.selectedExportFormat = 'csv';
  }

  exportReport() {
    if (!this.selectedExportType) return;
    
    this.isExporting = true;
    
    let exportObservable: Observable<string>;
    
    switch (this.selectedExportType) {
      case 'users':
        exportObservable = this.adminService.exportUsersReport(this.selectedExportFormat);
        break;
      case 'properties':
        exportObservable = this.adminService.exportPropertiesReport(this.selectedExportFormat);
        break;
      case 'inquiries':
        exportObservable = this.adminService.exportInquiriesReport(this.selectedExportFormat);
        break;
      case 'announcements':
        exportObservable = this.adminService.exportAnnouncementsReport(this.selectedExportFormat);
        break;
      case 'analytics':
        exportObservable = this.adminService.exportAnalyticsReport(this.selectedExportFormat);
        break;
      default:
        this.isExporting = false;
        alert('Invalid export type selected');
        return;
    }
    
    exportObservable.subscribe({
      next: (response) => {
        console.log('Export successful:', response);
        alert('Report exported successfully! ' + response);
        this.closeExportModal();
        this.isExporting = false;
      },
      error: (error) => {
        console.error('Export failed:', error);
        let errorMessage = 'Failed to export report';
        if (error.error && typeof error.error === 'string') {
          errorMessage = error.error;
        } else if (error.message) {
          errorMessage = error.message;
        }
        alert(errorMessage);
        this.isExporting = false;
      }
    });
  }

  backupDatabase() {
    this.adminService.backupDatabase().subscribe({
      next: (response) => {
        console.log('Backup successful:', response);
        alert('Database backup completed successfully!');
      },
      error: (error) => {
        console.error('Backup failed:', error);
        alert('Database backup failed: ' + error.message);
      }
    });
  }

  triggerScraper(type: 'immobilier' | 'tayara') {
    // Show start toast
    this.activeScraperType = type;
    this.showScraperStartToast = true;
    
    // Get initial count for comparison
    this.adminService.getScraperStatus().subscribe({
      next: (status) => {
        if (status && status.includes('Database contains')) {
          const match = status.match(/Database contains (\d+) property/);
          if (match) {
            this.lastKnownScrapedCount = parseInt(match[1], 10);
          }
        }
      },
      error: () => {
        this.lastKnownScrapedCount = 0;
      }
    });
    
    const scraperCall = type === 'immobilier' 
      ? this.adminService.triggerImmobilierScraper()
      : this.adminService.triggerTayaraScraper();
    
    scraperCall.subscribe({
      next: (response) => {
        console.log(`${type} scraper triggered:`, response);
        
        // Start polling for completion
        this.startScraperPolling();
      },
      error: (error) => {
        console.error(`Error triggering ${type} scraper:`, error);
        
        // Hide start toast and show error toast
        this.showScraperStartToast = false;
        this.showScraperErrorToast = true;
        
        if (error.message && error.message.includes('timeout')) {
          this.scraperErrorMessage = 'Request timed out. Scraping may still be running in background.';
        } else if (error.status === 503) {
          this.scraperErrorMessage = 'Scraper service unavailable. Ensure scraping module is running.';
        } else {
          this.scraperErrorMessage = error.message || 'Unknown error occurred';
        }
        
        // Auto-hide error toast after 10 seconds
        setTimeout(() => {
          this.hideErrorToast();
        }, 10000);
      }
    });
  }

  private startScraperPolling() {
    // Poll every 10 seconds to check for completion
    this.scraperPollingSubscription = interval(10000).subscribe(() => {
      this.adminService.getScraperStatus().subscribe({
        next: (status) => {
          if (status && status.includes('Database contains')) {
            const match = status.match(/Database contains (\d+) property/);
            if (match) {
              const currentCount = parseInt(match[1], 10);
              
              // Check if new properties were added
              if (currentCount > this.lastKnownScrapedCount) {
                this.lastScrapedCount = currentCount - this.lastKnownScrapedCount;
                this.lastCompletedScraperType = this.activeScraperType;
                
                // Hide start toast and show success toast
                this.showScraperStartToast = false;
                this.showScraperSuccessToast = true;
                
                // Stop polling
                if (this.scraperPollingSubscription) {
                  this.scraperPollingSubscription.unsubscribe();
                }
                
                // Auto-hide success toast after 15 seconds
                setTimeout(() => {
                  this.hideSuccessToast();
                }, 15000);
              }
            }
          }
        },
        error: (error) => {
          console.error('Error polling scraper status:', error);
        }
      });
    });
    
    // Stop polling after 15 minutes (timeout)
    setTimeout(() => {
      if (this.scraperPollingSubscription) {
        this.scraperPollingSubscription.unsubscribe();
        this.showScraperStartToast = false;
      }
    }, 15 * 60 * 1000);
  }

  hideSuccessToast() {
    this.showScraperSuccessToast = false;
  }

  hideErrorToast() {
    this.showScraperErrorToast = false;
  }

  logout() {
    this.authService.logout().subscribe({
      next: () => {
        this.router.navigate(['/']);
      },
      error: (error) => {
        console.error('Logout error:', error);
        this.router.navigate(['/']);
      }
    });
  }

  // Utility methods
  formatNumber(num: number): string {
    return this.adminService.formatNumber(num);
  }

  formatBytes(bytes: number): string {
    return this.adminService.formatBytes(bytes);
  }

  getStatusColor(status: string): string {
    return this.adminService.getStatusColor(status);
  }

  getGrowthIcon(rate: number): string {
    if (rate > 0) return '📈';
    if (rate < 0) return '📉';
    return '➖';
  }

  getGrowthColor(rate: number): string {
    if (rate > 0) return 'text-green-600';
    if (rate < 0) return 'text-red-600';
    return 'text-gray-600';
  }

  calculateUptime(uptimeHours: string): string {
    const hours = parseFloat(uptimeHours);
    const days = Math.floor(hours / 24);
    const remainingHours = Math.floor(hours % 24);
    
    if (days > 0) {
      return `${days}d ${remainingHours}h`;
    }
    return `${remainingHours}h`;
  }

  getMemoryUsagePercent(): number {
    if (!this.systemHealth?.application) return 0;
    const { heapMemoryUsed, heapMemoryMax } = this.systemHealth.application;
    return Math.round((heapMemoryUsed / heapMemoryMax) * 100);
  }

  getSeverityColor(severity: string): string {
    switch (severity.toUpperCase()) {
      case 'CRITICAL': return 'bg-red-100 text-red-800';
      case 'HIGH': return 'bg-orange-100 text-orange-800';
      case 'MEDIUM': return 'bg-yellow-100 text-yellow-800';
      case 'LOW': return 'bg-blue-100 text-blue-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }
} 