export interface AnalyticsOverview {
  totalInquiries: number;
  conversionRate: number;
  totalRevenue: number;
  averageResponseTime: number; // in hours
  activeProperties: number;
  totalViews: number;
}

export interface RevenueData {
  month: string;
  revenue: number;
  inquiries: number;
  deals: number;
}

export interface InquiryData {
  date: string;
  count: number;
  status: 'PENDING_REPLY' | 'REPLIED' | 'CLOSED' | 'PROPERTY_NO_LONGER_AVAILABLE';
}

export interface PropertyPerformance {
  propertyId: number;
  title: string;
  inquiries: number;
  conversions: number;
  revenue: number;
  views: number;
  conversionRate: number;
}

export interface InquiryStatusBreakdown {
  status: string;
  count: number;
  percentage: number;
  color: string;
}

export interface MarketInsight {
  category: string;
  label: string;
  value: number;
  trend: 'up' | 'down' | 'stable';
  percentage: number;
}

export interface TimeSeriesData {
  labels: string[];
  datasets: {
    label: string;
    data: number[];
    borderColor: string;
    backgroundColor: string;
    tension?: number;
  }[];
}

export interface BarChartData {
  labels: string[];
  datasets: {
    label: string;
    data: number[];
    backgroundColor: string[];
    borderColor?: string[];
    borderWidth?: number;
  }[];
}

export interface PieChartData {
  labels: string[];
  datasets: {
    data: number[];
    backgroundColor: string[];
    borderColor?: string[];
    borderWidth?: number;
  }[];
}

export interface AnalyticsDashboardData {
  overview: AnalyticsOverview;
  revenueData: RevenueData[];
  inquiryTrends: TimeSeriesData;
  propertyPerformance: PropertyPerformance[];
  inquiryStatusBreakdown: PieChartData;
  marketInsights: MarketInsight[];
  monthlyComparison: BarChartData;
  responseTimeData: TimeSeriesData;
} 