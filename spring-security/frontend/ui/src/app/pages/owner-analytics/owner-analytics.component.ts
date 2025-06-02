import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Chart, ChartConfiguration, ChartData, ChartType, registerables } from 'chart.js';
import { AnalyticsService } from '../../services/analytics.service';
import { AnalyticsDashboardData, MarketInsight, PropertyPerformance } from '../../models/analytics.model';
import { Subscription } from 'rxjs';

// Register Chart.js components
Chart.register(...registerables);

@Component({
  selector: 'app-owner-analytics',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './owner-analytics.component.html',
  styleUrls: ['./owner-analytics.component.css']
})
export class OwnerAnalyticsComponent implements OnInit, OnDestroy {
  
  // Data
  dashboardData?: AnalyticsDashboardData;
  isLoading = true;
  error?: string;
  
  // Chart instances
  private inquiryTrendsChart?: Chart;
  private statusBreakdownChart?: Chart;
  private monthlyComparisonChart?: Chart;
  private responseTimeChart?: Chart;
  
  // Chart references
  @ViewChild('inquiryTrendsCanvas', { static: false }) inquiryTrendsCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('statusBreakdownCanvas', { static: false }) statusBreakdownCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('monthlyComparisonCanvas', { static: false }) monthlyComparisonCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('responseTimeCanvas', { static: false }) responseTimeCanvas?: ElementRef<HTMLCanvasElement>;
  
  // Subscriptions
  private dataSubscription?: Subscription;
  
  // Date filter
  selectedPeriod = 'last30days';
  
  constructor(private analyticsService: AnalyticsService) {}
  
  ngOnInit(): void {
    this.loadAnalyticsData();
  }
  
  ngOnDestroy(): void {
    this.destroyCharts();
    if (this.dataSubscription) {
      this.dataSubscription.unsubscribe();
    }
  }
  
  /**
   * Load analytics data from service
   */
  private loadAnalyticsData(): void {
    this.isLoading = true;
    this.error = undefined;
    
    this.dataSubscription = this.analyticsService.getAnalyticsDashboard().subscribe({
      next: (data) => {
        this.dashboardData = data;
        this.isLoading = false;
        
        // Wait for view to render then create charts
        setTimeout(() => {
          this.createAllCharts();
        }, 100);
      },
      error: (error) => {
        console.error('Error loading analytics data:', error);
        this.error = 'Failed to load analytics data';
        this.isLoading = false;
      }
    });
  }
  
  /**
   * Create all charts
   */
  private createAllCharts(): void {
    if (!this.dashboardData) return;
    
    this.createInquiryTrendsChart();
    this.createStatusBreakdownChart();
    this.createMonthlyComparisonChart();
    this.createResponseTimeChart();
  }
  
  /**
   * Create inquiry trends line chart
   */
  private createInquiryTrendsChart(): void {
    if (!this.inquiryTrendsCanvas || !this.dashboardData) return;
    
    const ctx = this.inquiryTrendsCanvas.nativeElement.getContext('2d');
    if (!ctx) return;
    
    this.inquiryTrendsChart = new Chart(ctx, {
      type: 'line',
      data: this.dashboardData.inquiryTrends,
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'top',
          },
          tooltip: {
            mode: 'index',
            intersect: false,
          }
        },
        scales: {
          x: {
            display: true,
            title: {
              display: true,
              text: 'Date'
            }
          },
          y: {
            display: true,
            title: {
              display: true,
              text: 'Count'
            },
            beginAtZero: true
          }
        },
        interaction: {
          mode: 'nearest',
          axis: 'x',
          intersect: false
        }
      }
    });
  }
  
  /**
   * Create inquiry status breakdown pie chart
   */
  private createStatusBreakdownChart(): void {
    if (!this.statusBreakdownCanvas || !this.dashboardData) return;
    
    const ctx = this.statusBreakdownCanvas.nativeElement.getContext('2d');
    if (!ctx) return;
    
    this.statusBreakdownChart = new Chart(ctx, {
      type: 'doughnut',
      data: this.dashboardData.inquiryStatusBreakdown,
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
          },
          tooltip: {
            callbacks: {
              label: (context) => {
                const label = context.label || '';
                const value = context.parsed;
                const total = context.dataset.data.reduce((a: number, b: number) => a + b, 0);
                const percentage = ((value / total) * 100).toFixed(1);
                return `${label}: ${value} (${percentage}%)`;
              }
            }
          }
        }
      }
    });
  }
  
  /**
   * Create monthly comparison bar chart
   */
  private createMonthlyComparisonChart(): void {
    if (!this.monthlyComparisonCanvas || !this.dashboardData) return;
    
    const ctx = this.monthlyComparisonCanvas.nativeElement.getContext('2d');
    if (!ctx) return;
    
    this.monthlyComparisonChart = new Chart(ctx, {
      type: 'bar',
      data: this.dashboardData.monthlyComparison,
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'top',
          }
        },
        scales: {
          x: {
            display: true,
            title: {
              display: true,
              text: 'Month'
            }
          },
          y: {
            display: true,
            title: {
              display: true,
              text: 'Count'
            },
            beginAtZero: true
          }
        }
      }
    });
  }
  
  /**
   * Create response time line chart
   */
  private createResponseTimeChart(): void {
    if (!this.responseTimeCanvas || !this.dashboardData) return;
    
    const ctx = this.responseTimeCanvas.nativeElement.getContext('2d');
    if (!ctx) return;
    
    this.responseTimeChart = new Chart(ctx, {
      type: 'line',
      data: this.dashboardData.responseTimeData,
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'top',
          }
        },
        scales: {
          x: {
            display: true,
            title: {
              display: true,
              text: 'Period'
            }
          },
          y: {
            display: true,
            title: {
              display: true,
              text: 'Hours'
            },
            beginAtZero: true
          }
        }
      }
    });
  }
  
  /**
   * Destroy all chart instances
   */
  private destroyCharts(): void {
    if (this.inquiryTrendsChart) {
      this.inquiryTrendsChart.destroy();
    }
    if (this.statusBreakdownChart) {
      this.statusBreakdownChart.destroy();
    }
    if (this.monthlyComparisonChart) {
      this.monthlyComparisonChart.destroy();
    }
    if (this.responseTimeChart) {
      this.responseTimeChart.destroy();
    }
  }
  
  /**
   * Handle period filter change
   */
  onPeriodChange(event: Event): void {
    const target = event.target as HTMLSelectElement;
    this.selectedPeriod = target.value;
    this.destroyCharts();
    this.loadAnalyticsData();
  }
  
  /**
   * Get trend icon for market insights
   */
  getTrendIcon(trend: string): string {
    switch (trend) {
      case 'up': return '📈';
      case 'down': return '📉';
      case 'stable': return '📊';
      default: return '📊';
    }
  }
  
  /**
   * Get trend class for styling
   */
  getTrendClass(trend: string): string {
    switch (trend) {
      case 'up': return 'trend-up';
      case 'down': return 'trend-down';
      case 'stable': return 'trend-stable';
      default: return 'trend-stable';
    }
  }
  
  /**
   * Refresh analytics data
   */
  refresh(): void {
    this.destroyCharts();
    this.loadAnalyticsData();
  }
  
  /**
   * Export analytics data (placeholder)
   */
  exportData(): void {
    // Placeholder for export functionality
    console.log('Exporting analytics data...');
  }

  /**
   * TrackBy function for property performance table
   */
  trackByPropertyId(index: number, property: PropertyPerformance): number {
    return property.propertyId;
  }
} 