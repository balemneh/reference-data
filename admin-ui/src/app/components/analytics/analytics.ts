import { Component, OnInit, OnDestroy, AfterViewInit, ViewChild, ElementRef, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { ToastService } from '../../services/toast.service';
import { forkJoin, interval, Subject, timer, of } from 'rxjs';
import { startWith, switchMap, takeUntil, tap, catchError } from 'rxjs/operators';
import { Chart, registerables } from 'chart.js';
import { SkeletonLoaderComponent } from '../skeleton-loader/skeleton-loader';

interface AnalyticsData {
  entityGrowth: EntityGrowthData[];
  usagePatterns: UsagePatternData[];
  dataQuality: QualityMetricsData[];
  changeRequests: ChangeRequestAnalytics;
  geographic: GeographicData[];
  performance: PerformanceMetrics;
  userBehavior: UserBehaviorData;
  realTimeStats: RealTimeStats;
}

interface EntityGrowthData {
  date: string;
  countries: number;
  ports: number;
  airports: number;
  carriers: number;
  mappings: number;
}

interface UsagePatternData {
  hour: number;
  date: string;
  apiCalls: number;
  users: number;
  entities: string[];
  responseTime: number;
}

interface QualityMetricsData {
  date: string;
  completeness: number;
  consistency: number;
  validity: number;
  uniqueness: number;
  accuracy: number;
  overall: number;
}

interface ChangeRequestAnalytics {
  totalRequests: number;
  approvalRate: number;
  avgProcessingTime: number;
  statusDistribution: { [key: string]: number };
  monthlyTrends: { month: string; count: number; approvalRate: number }[];
  typeDistribution: { [key: string]: number };
}

interface GeographicData {
  country: string;
  countryCode: string;
  latitude: number;
  longitude: number;
  entities: number;
  activity: number;
  region: string;
}

interface PerformanceMetrics {
  avgResponseTime: number;
  p95ResponseTime: number;
  p99ResponseTime: number;
  errorRate: number;
  throughput: number;
  uptime: number;
  hourlyMetrics: { hour: number; responseTime: number; throughput: number; errors: number }[];
}

interface UserBehaviorData {
  totalUsers: number;
  activeUsers: number;
  sessionDuration: number;
  topPages: { page: string; visits: number; bounceRate: number }[];
  userJourney: { step: string; users: number; dropoff: number }[];
  deviceTypes: { [key: string]: number };
  browserTypes: { [key: string]: number };
}

interface RealTimeStats {
  currentUsers: number;
  currentApiCalls: number;
  currentResponseTime: number;
  recentActivity: ActivityItem[];
}

interface ActivityItem {
  id: string;
  type: 'VIEW' | 'CREATE' | 'UPDATE' | 'DELETE' | 'EXPORT' | 'IMPORT';
  entity: string;
  user: string;
  timestamp: Date;
  details: any;
}

interface TimeRange {
  label: string;
  value: string;
  days: number;
}

interface ChartFilter {
  type: 'entity' | 'metric' | 'region';
  values: string[];
  enabled: boolean;
}

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule, FormsModule, SkeletonLoaderComponent],
  templateUrl: './analytics.html',
  styleUrl: './analytics.scss'
})
export class AnalyticsComponent implements OnInit, OnDestroy, AfterViewInit {
  // Chart References
  @ViewChild('entityGrowthChart', { static: false }) entityGrowthChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('usagePatternChart', { static: false }) usagePatternChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('dataQualityChart', { static: false }) dataQualityChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('changeRequestChart', { static: false }) changeRequestChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('geographicChart', { static: false }) geographicChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('performanceChart', { static: false }) performanceChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('userBehaviorChart', { static: false }) userBehaviorChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('realTimeChart', { static: false }) realTimeChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('apiHeatmapChart', { static: false }) apiHeatmapChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('qualityTrendChart', { static: false }) qualityTrendChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('responseTimeChart', { static: false }) responseTimeChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('userFlowChart', { static: false }) userFlowChartRef!: ElementRef<HTMLCanvasElement>;

  private destroy$ = new Subject<void>();
  private charts: { [key: string]: Chart | null } = {};
  private refreshInterval = 30000; // 30 seconds
  private realTimeInterval = 5000; // 5 seconds for real-time data

  // Component State
  loading = true;
  error: string | null = null;
  isRefreshing = false;
  
  // Analytics Data
  analyticsData: AnalyticsData | null = null;
  
  // UI State
  selectedTimeRange: TimeRange = { label: 'Last 7 Days', value: '7d', days: 7 };
  timeRanges: TimeRange[] = [
    { label: 'Last 24 Hours', value: '24h', days: 1 },
    { label: 'Last 7 Days', value: '7d', days: 7 },
    { label: 'Last 30 Days', value: '30d', days: 30 },
    { label: 'Last 90 Days', value: '90d', days: 90 },
    { label: 'Last 365 Days', value: '365d', days: 365 }
  ];
  
  activeFilters: ChartFilter[] = [
    { type: 'entity', values: ['countries', 'ports', 'airports', 'carriers'], enabled: true },
    { type: 'metric', values: ['completeness', 'consistency', 'validity'], enabled: true },
    { type: 'region', values: ['North America', 'Europe', 'Asia', 'Other'], enabled: true }
  ];
  
  // Drill-down State
  selectedChart: string | null = null;
  drillDownLevel = 0;
  drillDownHistory: any[] = [];
  
  // Export State
  exportProgress = 0;
  isExporting = false;
  exportFormat: 'png' | 'pdf' | 'csv' | 'xlsx' = 'png';
  
  // Real-time State
  realTimeEnabled = true;
  realTimeData: RealTimeStats = {
    currentUsers: 0,
    currentApiCalls: 0,
    currentResponseTime: 0,
    recentActivity: []
  };
  
  // Dashboard Configuration
  dashboardLayout: string[] = [
    'entityGrowth',
    'usagePattern', 
    'dataQuality',
    'changeRequest',
    'geographic',
    'performance',
    'userBehavior',
    'realTime'
  ];
  
  // KPIs
  kpis = {
    totalEntities: 0,
    monthlyGrowth: 0,
    dataQualityScore: 0,
    avgResponseTime: 0,
    userSatisfaction: 0,
    systemUptime: 0
  };

  // Expose Math for template
  Math = Math;

  constructor(
    private apiService: ApiService,
    private router: Router,
    private toastService: ToastService
  ) {
    Chart.register(...registerables);
  }

  ngOnInit() {
    this.loadAnalyticsData();
    this.startAutoRefresh();
    this.startRealTimeUpdates();
    this.setupKeyboardShortcuts();
  }

  ngAfterViewInit() {
    setTimeout(() => {
      this.initializeAllCharts();
    }, 100);
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    this.destroyAllCharts();
  }

  // Data Loading Methods
  loadAnalyticsData() {
    this.loading = true;
    this.error = null;

    // Mock analytics data for demonstration
    this.analyticsData = this.generateMockAnalyticsData();
    this.updateKPIs();
    this.loading = false;
    
    // In real implementation, uncomment below:
    /*
    this.apiService.getAnalyticsData(this.selectedTimeRange.value).subscribe({
      next: (data) => {
        this.analyticsData = data;
        this.updateKPIs();
        this.updateAllCharts();
        this.loading = false;
      },
      error: (error) => {
        this.error = 'Failed to load analytics data';
        console.error('Analytics error:', error);
        this.loading = false;
        this.loadMockData();
      }
    });
    */
  }

  private generateMockAnalyticsData(): AnalyticsData {
    const now = new Date();
    const days = this.selectedTimeRange.days;
    
    // Entity Growth Data
    const entityGrowth: EntityGrowthData[] = [];
    for (let i = days; i >= 0; i--) {
      const date = new Date(now.getTime() - i * 24 * 60 * 60 * 1000);
      entityGrowth.push({
        date: date.toISOString().split('T')[0],
        countries: 245 + Math.floor(Math.random() * 20),
        ports: 1820 + Math.floor(Math.random() * 100),
        airports: 4200 + Math.floor(Math.random() * 150),
        carriers: 850 + Math.floor(Math.random() * 50),
        mappings: 15780 + Math.floor(Math.random() * 500)
      });
    }

    // Usage Patterns
    const usagePatterns: UsagePatternData[] = [];
    for (let i = 0; i < 168; i++) { // Last week, hourly
      const date = new Date(now.getTime() - i * 60 * 60 * 1000);
      usagePatterns.push({
        hour: date.getHours(),
        date: date.toISOString().split('T')[0],
        apiCalls: Math.floor(Math.random() * 1000) + 100,
        users: Math.floor(Math.random() * 50) + 10,
        entities: ['countries', 'ports', 'airports'].slice(0, Math.floor(Math.random() * 3) + 1),
        responseTime: Math.floor(Math.random() * 100) + 50
      });
    }

    // Data Quality Metrics
    const dataQuality: QualityMetricsData[] = [];
    for (let i = days; i >= 0; i--) {
      const date = new Date(now.getTime() - i * 24 * 60 * 60 * 1000);
      dataQuality.push({
        date: date.toISOString().split('T')[0],
        completeness: 94 + Math.random() * 4,
        consistency: 91 + Math.random() * 6,
        validity: 92 + Math.random() * 5,
        uniqueness: 95 + Math.random() * 3,
        accuracy: 93 + Math.random() * 4,
        overall: 93 + Math.random() * 4
      });
    }

    // Change Request Analytics
    const changeRequests: ChangeRequestAnalytics = {
      totalRequests: 1247,
      approvalRate: 89.3,
      avgProcessingTime: 2.4,
      statusDistribution: {
        'Approved': 1113,
        'Pending': 89,
        'Rejected': 32,
        'In Review': 13
      },
      monthlyTrends: Array.from({ length: 12 }, (_, i) => ({
        month: new Date(now.getFullYear(), i, 1).toLocaleDateString('en-US', { month: 'short' }),
        count: Math.floor(Math.random() * 200) + 50,
        approvalRate: 85 + Math.random() * 10
      })),
      typeDistribution: {
        'CREATE': 456,
        'UPDATE': 623,
        'DELETE': 168
      }
    };

    // Geographic Data
    const geographic: GeographicData[] = [
      { country: 'United States', countryCode: 'US', latitude: 39.8283, longitude: -98.5795, entities: 8234, activity: 15670, region: 'North America' },
      { country: 'Canada', countryCode: 'CA', latitude: 56.1304, longitude: -106.3468, entities: 1456, activity: 2890, region: 'North America' },
      { country: 'Germany', countryCode: 'DE', latitude: 51.1657, longitude: 10.4515, entities: 2134, activity: 4567, region: 'Europe' },
      { country: 'United Kingdom', countryCode: 'GB', latitude: 55.3781, longitude: -3.4360, entities: 1789, activity: 3456, region: 'Europe' },
      { country: 'Japan', countryCode: 'JP', latitude: 36.2048, longitude: 138.2529, entities: 1234, activity: 2789, region: 'Asia' },
      { country: 'China', countryCode: 'CN', latitude: 35.8617, longitude: 104.1954, entities: 987, activity: 1234, region: 'Asia' }
    ];

    // Performance Metrics
    const performance: PerformanceMetrics = {
      avgResponseTime: 142,
      p95ResponseTime: 289,
      p99ResponseTime: 456,
      errorRate: 0.23,
      throughput: 1247.6,
      uptime: 99.94,
      hourlyMetrics: Array.from({ length: 24 }, (_, i) => ({
        hour: i,
        responseTime: Math.floor(Math.random() * 100) + 100,
        throughput: Math.floor(Math.random() * 500) + 800,
        errors: Math.floor(Math.random() * 5)
      }))
    };

    // User Behavior Data
    const userBehavior: UserBehaviorData = {
      totalUsers: 2456,
      activeUsers: 342,
      sessionDuration: 18.7,
      topPages: [
        { page: '/countries', visits: 12456, bounceRate: 23.4 },
        { page: '/ports', visits: 8934, bounceRate: 31.2 },
        { page: '/airports', visits: 6789, bounceRate: 28.9 },
        { page: '/change-requests', visits: 4567, bounceRate: 19.8 },
        { page: '/analytics', visits: 3456, bounceRate: 15.2 }
      ],
      userJourney: [
        { step: 'Landing', users: 1000, dropoff: 0 },
        { step: 'Browse Data', users: 856, dropoff: 14.4 },
        { step: 'Search/Filter', users: 678, dropoff: 20.8 },
        { step: 'View Details', users: 523, dropoff: 22.9 },
        { step: 'Export/Action', users: 234, dropoff: 55.3 }
      ],
      deviceTypes: {
        'Desktop': 1678,
        'Tablet': 456,
        'Mobile': 322
      },
      browserTypes: {
        'Chrome': 1234,
        'Firefox': 567,
        'Safari': 345,
        'Edge': 234,
        'Other': 76
      }
    };

    return {
      entityGrowth,
      usagePatterns,
      dataQuality,
      changeRequests,
      geographic,
      performance,
      userBehavior,
      realTimeStats: {
        currentUsers: 42,
        currentApiCalls: 156,
        currentResponseTime: 134,
        recentActivity: []
      }
    };
  }

  private updateKPIs() {
    if (!this.analyticsData) return;

    const latestGrowth = this.analyticsData.entityGrowth[this.analyticsData.entityGrowth.length - 1];
    const previousGrowth = this.analyticsData.entityGrowth[this.analyticsData.entityGrowth.length - 2];
    
    this.kpis = {
      totalEntities: latestGrowth.countries + latestGrowth.ports + latestGrowth.airports + latestGrowth.carriers,
      monthlyGrowth: previousGrowth ? 
        ((latestGrowth.countries + latestGrowth.ports + latestGrowth.airports + latestGrowth.carriers) - 
         (previousGrowth.countries + previousGrowth.ports + previousGrowth.airports + previousGrowth.carriers)) / 
         (previousGrowth.countries + previousGrowth.ports + previousGrowth.airports + previousGrowth.carriers) * 100 : 0,
      dataQualityScore: this.analyticsData.dataQuality[this.analyticsData.dataQuality.length - 1]?.overall || 0,
      avgResponseTime: this.analyticsData.performance.avgResponseTime,
      userSatisfaction: 4.2, // Mock satisfaction score
      systemUptime: this.analyticsData.performance.uptime
    };
  }

  // Auto-refresh and Real-time Methods
  private startAutoRefresh() {
    interval(this.refreshInterval).pipe(
      takeUntil(this.destroy$),
      switchMap(() => {
        if (!this.isRefreshing) {
          this.loadAnalyticsData();
        }
        return of(null);
      })
    ).subscribe();
  }

  private startRealTimeUpdates() {
    if (!this.realTimeEnabled) return;

    interval(this.realTimeInterval).pipe(
      takeUntil(this.destroy$),
      switchMap(() => {
        this.updateRealTimeData();
        return of(null);
      })
    ).subscribe();
  }

  private updateRealTimeData() {
    // Mock real-time updates
    this.realTimeData = {
      currentUsers: Math.floor(Math.random() * 100) + 20,
      currentApiCalls: Math.floor(Math.random() * 200) + 50,
      currentResponseTime: Math.floor(Math.random() * 100) + 80,
      recentActivity: this.generateRecentActivity()
    };

    // Update real-time chart if exists
    if (this.charts['realTime']) {
      this.updateRealTimeChart();
    }
  }

  private generateRecentActivity(): ActivityItem[] {
    const types: ActivityItem['type'][] = ['VIEW', 'CREATE', 'UPDATE', 'DELETE', 'EXPORT', 'IMPORT'];
    const entities = ['countries', 'ports', 'airports', 'carriers'];
    const users = ['john.doe@cbp.gov', 'jane.smith@cbp.gov', 'mike.johnson@cbp.gov'];
    
    return Array.from({ length: 10 }, (_, i) => ({
      id: `activity-${Date.now()}-${i}`,
      type: types[Math.floor(Math.random() * types.length)],
      entity: entities[Math.floor(Math.random() * entities.length)],
      user: users[Math.floor(Math.random() * users.length)],
      timestamp: new Date(Date.now() - Math.random() * 300000), // Last 5 minutes
      details: {}
    }));
  }

  // Chart Initialization Methods
  private initializeAllCharts() {
    if (!this.analyticsData) return;

    this.initializeEntityGrowthChart();
    this.initializeUsagePatternChart();
    this.initializeDataQualityChart();
    this.initializeChangeRequestChart();
    this.initializeGeographicChart();
    this.initializePerformanceChart();
    this.initializeUserBehaviorChart();
    this.initializeRealTimeChart();
    this.initializeAPIHeatmapChart();
    this.initializeQualityTrendChart();
    this.initializeResponseTimeChart();
    this.initializeUserFlowChart();
  }

  private initializeEntityGrowthChart() {
    if (!this.entityGrowthChartRef?.nativeElement || !this.analyticsData) return;

    const ctx = this.entityGrowthChartRef.nativeElement.getContext('2d');
    if (!ctx) return;

    const data = this.analyticsData.entityGrowth;
    const filteredEntities = this.getFilteredEntities();

    this.charts['entityGrowth'] = new Chart(ctx, {
      type: 'line',
      data: {
        labels: data.map(d => new Date(d.date).toLocaleDateString()),
        datasets: [
          ...(filteredEntities.includes('countries') ? [{
            label: 'Countries',
            data: data.map(d => d.countries),
            borderColor: '#10b981',
            backgroundColor: 'rgba(16, 185, 129, 0.1)',
            borderWidth: 3,
            fill: false,
            tension: 0.4
          }] : []),
          ...(filteredEntities.includes('ports') ? [{
            label: 'Ports',
            data: data.map(d => d.ports),
            borderColor: '#3b82f6',
            backgroundColor: 'rgba(59, 130, 246, 0.1)',
            borderWidth: 3,
            fill: false,
            tension: 0.4
          }] : []),
          ...(filteredEntities.includes('airports') ? [{
            label: 'Airports',
            data: data.map(d => d.airports),
            borderColor: '#f59e0b',
            backgroundColor: 'rgba(245, 158, 11, 0.1)',
            borderWidth: 3,
            fill: false,
            tension: 0.4
          }] : []),
          ...(filteredEntities.includes('carriers') ? [{
            label: 'Carriers',
            data: data.map(d => d.carriers),
            borderColor: '#8b5cf6',
            backgroundColor: 'rgba(139, 92, 246, 0.1)',
            borderWidth: 3,
            fill: false,
            tension: 0.4
          }] : [])
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: {
          mode: 'index',
          intersect: false,
        },
        plugins: {
          legend: {
            position: 'top',
          },
          title: {
            display: true,
            text: 'Entity Growth Over Time'
          },
          tooltip: {
            callbacks: {
              title: (context: any) => {
                return `Date: ${context[0].label}`;
              },
              label: (context: any) => {
                return `${context.dataset.label}: ${context.parsed.y.toLocaleString()}`;
              }
            }
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
            beginAtZero: false
          }
        },
        onClick: (event, elements) => {
          if (elements.length > 0) {
            this.drillDownEntityGrowth(elements[0].index);
          }
        }
      }
    });
  }

  private initializeUsagePatternChart() {
    if (!this.usagePatternChartRef?.nativeElement || !this.analyticsData) return;

    const ctx = this.usagePatternChartRef.nativeElement.getContext('2d');
    if (!ctx) return;

    // Aggregate hourly data
    const hourlyData = Array.from({ length: 24 }, (_, hour) => {
      const hourData = this.analyticsData!.usagePatterns.filter(d => d.hour === hour);
      return {
        hour,
        avgApiCalls: hourData.reduce((sum, d) => sum + d.apiCalls, 0) / Math.max(hourData.length, 1),
        avgUsers: hourData.reduce((sum, d) => sum + d.users, 0) / Math.max(hourData.length, 1),
        avgResponseTime: hourData.reduce((sum, d) => sum + d.responseTime, 0) / Math.max(hourData.length, 1)
      };
    });

    this.charts['usagePattern'] = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: hourlyData.map(d => `${d.hour.toString().padStart(2, '0')}:00`),
        datasets: [
          {
            label: 'API Calls',
            data: hourlyData.map(d => d.avgApiCalls),
            backgroundColor: 'rgba(59, 130, 246, 0.8)',
            borderColor: '#3b82f6',
            borderWidth: 1,
            yAxisID: 'y'
          },
          {
            label: 'Active Users',
            data: hourlyData.map(d => d.avgUsers),
            backgroundColor: 'rgba(16, 185, 129, 0.8)',
            borderColor: '#10b981',
            borderWidth: 1,
            yAxisID: 'y1'
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'top',
          },
          title: {
            display: true,
            text: 'Usage Patterns by Hour'
          }
        },
        scales: {
          x: {
            display: true,
            title: {
              display: true,
              text: 'Hour of Day'
            }
          },
          y: {
            type: 'linear',
            display: true,
            position: 'left',
            title: {
              display: true,
              text: 'API Calls'
            }
          },
          y1: {
            type: 'linear',
            display: true,
            position: 'right',
            title: {
              display: true,
              text: 'Active Users'
            },
            grid: {
              drawOnChartArea: false,
            },
          }
        },
        onClick: (event, elements) => {
          if (elements.length > 0) {
            this.drillDownUsagePattern(elements[0].index);
          }
        }
      }
    });
  }

  private initializeDataQualityChart() {
    if (!this.dataQualityChartRef?.nativeElement || !this.analyticsData) return;

    const ctx = this.dataQualityChartRef.nativeElement.getContext('2d');
    if (!ctx) return;

    const data = this.analyticsData.dataQuality;
    const latest = data[data.length - 1];

    this.charts['dataQuality'] = new Chart(ctx, {
      type: 'radar',
      data: {
        labels: ['Completeness', 'Consistency', 'Validity', 'Uniqueness', 'Accuracy'],
        datasets: [{
          label: 'Quality Metrics',
          data: [
            latest.completeness,
            latest.consistency,
            latest.validity,
            latest.uniqueness,
            latest.accuracy
          ],
          backgroundColor: 'rgba(16, 185, 129, 0.2)',
          borderColor: '#10b981',
          borderWidth: 3,
          pointBackgroundColor: '#10b981',
          pointBorderColor: '#ffffff',
          pointBorderWidth: 2,
          pointRadius: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'top',
          },
          title: {
            display: true,
            text: 'Data Quality Score'
          }
        },
        scales: {
          r: {
            beginAtZero: true,
            max: 100,
            ticks: {
              stepSize: 20
            }
          }
        },
        onClick: (event, elements) => {
          if (elements.length > 0) {
            this.drillDownDataQuality();
          }
        }
      }
    });
  }

  private initializeChangeRequestChart() {
    if (!this.changeRequestChartRef?.nativeElement || !this.analyticsData) return;

    const ctx = this.changeRequestChartRef.nativeElement.getContext('2d');
    if (!ctx) return;

    const data = this.analyticsData.changeRequests;

    this.charts['changeRequest'] = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: Object.keys(data.statusDistribution),
        datasets: [{
          data: Object.values(data.statusDistribution),
          backgroundColor: [
            '#10b981', // Approved - Green
            '#f59e0b', // Pending - Orange
            '#ef4444', // Rejected - Red
            '#6b7280'  // In Review - Gray
          ],
          borderWidth: 0,
          hoverBorderWidth: 3,
          hoverBorderColor: '#ffffff'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
          },
          title: {
            display: true,
            text: 'Change Request Status Distribution'
          },
          tooltip: {
            callbacks: {
              label: (context: any) => {
                const label = context.label || '';
                const value = context.parsed;
                const total = context.dataset.data.reduce((a: number, b: number) => a + b, 0);
                const percentage = ((value / total) * 100).toFixed(1);
                return `${label}: ${value} (${percentage}%)`;
              }
            }
          }
        },
        onClick: (event, elements) => {
          if (elements.length > 0) {
            this.drillDownChangeRequests(elements[0].index);
          }
        }
      }
    });
  }

  private initializeGeographicChart() {
    if (!this.geographicChartRef?.nativeElement || !this.analyticsData) return;

    const ctx = this.geographicChartRef.nativeElement.getContext('2d');
    if (!ctx) return;

    const data = this.analyticsData.geographic;

    this.charts['geographic'] = new Chart(ctx, {
      type: 'scatter',
      data: {
        datasets: [{
          label: 'Geographic Distribution',
          data: data.map(d => ({
            x: d.longitude,
            y: d.latitude,
            country: d.country,
            entities: d.entities,
            activity: d.activity
          })),
          backgroundColor: data.map(d => {
            const opacity = (d.activity / Math.max(...data.map(x => x.activity)));
            return `rgba(59, 130, 246, ${0.3 + opacity * 0.7})`;
          }),
          borderColor: '#3b82f6',
          borderWidth: 2,
          pointRadius: data.map(d => Math.max(5, Math.min(20, d.entities / 100)))
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: false
          },
          title: {
            display: true,
            text: 'Geographic Data Distribution'
          },
          tooltip: {
            callbacks: {
              title: (context: any) => {
                return (context[0].raw as any).country;
              },
              label: (context: any) => {
                const data = context.raw as any;
                return [
                  `Entities: ${data.entities.toLocaleString()}`,
                  `Activity: ${data.activity.toLocaleString()}`
                ];
              }
            }
          }
        },
        scales: {
          x: {
            type: 'linear',
            position: 'bottom',
            title: {
              display: true,
              text: 'Longitude'
            }
          },
          y: {
            title: {
              display: true,
              text: 'Latitude'
            }
          }
        },
        onClick: (event, elements) => {
          if (elements.length > 0) {
            this.drillDownGeographic(elements[0].index);
          }
        }
      }
    });
  }

  private initializePerformanceChart() {
    if (!this.performanceChartRef?.nativeElement || !this.analyticsData) return;

    const ctx = this.performanceChartRef.nativeElement.getContext('2d');
    if (!ctx) return;

    const data = this.analyticsData.performance.hourlyMetrics;

    this.charts['performance'] = new Chart(ctx, {
      type: 'line',
      data: {
        labels: data.map(d => `${d.hour.toString().padStart(2, '0')}:00`),
        datasets: [
          {
            label: 'Response Time (ms)',
            data: data.map(d => d.responseTime),
            borderColor: '#ef4444',
            backgroundColor: 'rgba(239, 68, 68, 0.1)',
            borderWidth: 2,
            yAxisID: 'y',
            tension: 0.4
          },
          {
            label: 'Throughput (req/min)',
            data: data.map(d => d.throughput),
            borderColor: '#10b981',
            backgroundColor: 'rgba(16, 185, 129, 0.1)',
            borderWidth: 2,
            yAxisID: 'y1',
            tension: 0.4
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'top',
          },
          title: {
            display: true,
            text: 'System Performance Metrics'
          }
        },
        scales: {
          x: {
            display: true,
            title: {
              display: true,
              text: 'Hour of Day'
            }
          },
          y: {
            type: 'linear',
            display: true,
            position: 'left',
            title: {
              display: true,
              text: 'Response Time (ms)'
            }
          },
          y1: {
            type: 'linear',
            display: true,
            position: 'right',
            title: {
              display: true,
              text: 'Throughput (req/min)'
            },
            grid: {
              drawOnChartArea: false,
            },
          }
        }
      }
    });
  }

  private initializeUserBehaviorChart() {
    if (!this.userBehaviorChartRef?.nativeElement || !this.analyticsData) return;

    const ctx = this.userBehaviorChartRef.nativeElement.getContext('2d');
    if (!ctx) return;

    const data = this.analyticsData.userBehavior.topPages;

    this.charts['userBehavior'] = new Chart(ctx, {
      type: 'horizontalBar' as any,
      data: {
        labels: data.map(d => d.page),
        datasets: [
          {
            label: 'Page Visits',
            data: data.map(d => d.visits),
            backgroundColor: 'rgba(59, 130, 246, 0.8)',
            borderColor: '#3b82f6',
            borderWidth: 1
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        indexAxis: 'y' as const,
        plugins: {
          legend: {
            position: 'top',
          },
          title: {
            display: true,
            text: 'Top Pages by Visits'
          },
          tooltip: {
            callbacks: {
              afterLabel: (context: any) => {
                const pageData = data[context.dataIndex];
                return `Bounce Rate: ${pageData.bounceRate}%`;
              }
            }
          }
        },
        scales: {
          x: {
            beginAtZero: true,
            title: {
              display: true,
              text: 'Number of Visits'
            }
          }
        }
      }
    });
  }

  private initializeRealTimeChart() {
    if (!this.realTimeChartRef?.nativeElement) return;

    const ctx = this.realTimeChartRef.nativeElement.getContext('2d');
    if (!ctx) return;

    // Initialize with empty data - will be updated by real-time updates
    this.charts['realTime'] = new Chart(ctx, {
      type: 'line',
      data: {
        labels: [],
        datasets: [
          {
            label: 'Active Users',
            data: [],
            borderColor: '#10b981',
            backgroundColor: 'rgba(16, 185, 129, 0.1)',
            borderWidth: 2,
            fill: true,
            tension: 0.4
          },
          {
            label: 'API Calls/min',
            data: [],
            borderColor: '#3b82f6',
            backgroundColor: 'rgba(59, 130, 246, 0.1)',
            borderWidth: 2,
            fill: false,
            tension: 0.4
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'top',
          },
          title: {
            display: true,
            text: 'Real-Time System Activity'
          }
        },
        scales: {
          x: {
            display: true,
            title: {
              display: true,
              text: 'Time'
            }
          },
          y: {
            beginAtZero: true,
            title: {
              display: true,
              text: 'Count'
            }
          }
        },
        animation: {
          duration: 0 // Disable animation for real-time updates
        }
      }
    });
  }

  // Additional chart initialization methods (API Heatmap, Quality Trend, etc.)
  private initializeAPIHeatmapChart() {
    if (!this.apiHeatmapChartRef?.nativeElement || !this.analyticsData) return;

    const ctx = this.apiHeatmapChartRef.nativeElement.getContext('2d');
    if (!ctx) return;

    // Create heatmap data (day of week vs hour of day)
    const heatmapData: any[] = [];
    const days = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
    
    for (let day = 0; day < 7; day++) {
      for (let hour = 0; hour < 24; hour++) {
        const intensity = Math.floor(Math.random() * 100);
        heatmapData.push({
          x: hour,
          y: day,
          v: intensity
        });
      }
    }

    this.charts['apiHeatmap'] = new Chart(ctx, {
      type: 'scatter',
      data: {
        datasets: [{
          label: 'API Activity',
          data: heatmapData.map(d => ({
            x: d.x,
            y: d.y,
            r: Math.max(3, d.v / 10)
          })),
          backgroundColor: heatmapData.map(d => {
            const opacity = d.v / 100;
            return `rgba(16, 185, 129, ${0.2 + opacity * 0.8})`;
          }),
          borderColor: '#10b981',
          borderWidth: 1
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: false
          },
          title: {
            display: true,
            text: 'API Activity Heatmap'
          }
        },
        scales: {
          x: {
            type: 'linear',
            position: 'bottom',
            min: 0,
            max: 23,
            title: {
              display: true,
              text: 'Hour of Day'
            },
            ticks: {
              stepSize: 2,
              callback: (value) => `${value}:00`
            }
          },
          y: {
            type: 'linear',
            min: 0,
            max: 6,
            title: {
              display: true,
              text: 'Day of Week'
            },
            ticks: {
              stepSize: 1,
              callback: (value) => days[value as number]
            }
          }
        }
      }
    });
  }

  private initializeQualityTrendChart() {
    if (!this.qualityTrendChartRef?.nativeElement || !this.analyticsData) return;

    const ctx = this.qualityTrendChartRef.nativeElement.getContext('2d');
    if (!ctx) return;

    const data = this.analyticsData.dataQuality;

    this.charts['qualityTrend'] = new Chart(ctx, {
      type: 'line',
      data: {
        labels: data.map(d => new Date(d.date).toLocaleDateString()),
        datasets: [
          {
            label: 'Overall',
            data: data.map(d => d.overall),
            borderColor: '#8b5cf6',
            backgroundColor: 'rgba(139, 92, 246, 0.1)',
            borderWidth: 3,
            fill: true,
            tension: 0.4
          },
          {
            label: 'Completeness',
            data: data.map(d => d.completeness),
            borderColor: '#10b981',
            backgroundColor: 'rgba(16, 185, 129, 0.1)',
            borderWidth: 2,
            fill: false,
            tension: 0.4
          },
          {
            label: 'Consistency',
            data: data.map(d => d.consistency),
            borderColor: '#3b82f6',
            backgroundColor: 'rgba(59, 130, 246, 0.1)',
            borderWidth: 2,
            fill: false,
            tension: 0.4
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'top',
          },
          title: {
            display: true,
            text: 'Data Quality Trends Over Time'
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
            min: 80,
            max: 100,
            title: {
              display: true,
              text: 'Quality Score (%)'
            }
          }
        }
      }
    });
  }

  private initializeResponseTimeChart() {
    if (!this.responseTimeChartRef?.nativeElement || !this.analyticsData) return;

    const ctx = this.responseTimeChartRef.nativeElement.getContext('2d');
    if (!ctx) return;

    const data = this.analyticsData.performance.hourlyMetrics;

    this.charts['responseTime'] = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: data.map(d => `${d.hour.toString().padStart(2, '0')}:00`),
        datasets: [
          {
            label: 'Average Response Time',
            data: data.map(d => d.responseTime),
            backgroundColor: data.map(d => {
              if (d.responseTime > 200) return 'rgba(239, 68, 68, 0.8)'; // Red for slow
              if (d.responseTime > 150) return 'rgba(245, 158, 11, 0.8)'; // Orange for moderate
              return 'rgba(16, 185, 129, 0.8)'; // Green for fast
            }),
            borderColor: data.map(d => {
              if (d.responseTime > 200) return '#ef4444';
              if (d.responseTime > 150) return '#f59e0b';
              return '#10b981';
            }),
            borderWidth: 1
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: false
          },
          title: {
            display: true,
            text: 'Response Time Distribution by Hour'
          }
        },
        scales: {
          x: {
            display: true,
            title: {
              display: true,
              text: 'Hour of Day'
            }
          },
          y: {
            beginAtZero: true,
            title: {
              display: true,
              text: 'Response Time (ms)'
            }
          }
        }
      }
    });
  }

  private initializeUserFlowChart() {
    if (!this.userFlowChartRef?.nativeElement || !this.analyticsData) return;

    const ctx = this.userFlowChartRef.nativeElement.getContext('2d');
    if (!ctx) return;

    const data = this.analyticsData.userBehavior.userJourney;

    this.charts['userFlow'] = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: data.map(d => d.step),
        datasets: [
          {
            label: 'Users',
            data: data.map(d => d.users),
            backgroundColor: 'rgba(59, 130, 246, 0.8)',
            borderColor: '#3b82f6',
            borderWidth: 1
          },
          {
            label: 'Dropoff Rate (%)',
            data: data.map(d => d.dropoff),
            backgroundColor: 'rgba(239, 68, 68, 0.8)',
            borderColor: '#ef4444',
            borderWidth: 1,
            yAxisID: 'y1'
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'top',
          },
          title: {
            display: true,
            text: 'User Journey Funnel'
          }
        },
        scales: {
          x: {
            display: true,
            title: {
              display: true,
              text: 'User Journey Steps'
            }
          },
          y: {
            type: 'linear',
            display: true,
            position: 'left',
            title: {
              display: true,
              text: 'Number of Users'
            },
            beginAtZero: true
          },
          y1: {
            type: 'linear',
            display: true,
            position: 'right',
            title: {
              display: true,
              text: 'Dropoff Rate (%)'
            },
            beginAtZero: true,
            max: 100,
            grid: {
              drawOnChartArea: false,
            }
          }
        }
      }
    });
  }

  // Chart Update Methods
  private updateAllCharts() {
    Object.values(this.charts).forEach(chart => {
      if (chart) {
        chart.update();
      }
    });
  }

  private updateRealTimeChart() {
    const chart = this.charts['realTime'];
    if (!chart) return;

    const now = new Date();
    const timeLabel = now.toLocaleTimeString();

    // Add new data point
    chart.data.labels?.push(timeLabel);
    chart.data.datasets[0].data.push(this.realTimeData.currentUsers);
    chart.data.datasets[1].data.push(this.realTimeData.currentApiCalls);

    // Keep only last 20 data points
    if (chart.data.labels && chart.data.labels.length > 20) {
      chart.data.labels.shift();
      chart.data.datasets[0].data.shift();
      chart.data.datasets[1].data.shift();
    }

    chart.update('none'); // No animation for real-time
  }

  private destroyAllCharts() {
    Object.values(this.charts).forEach(chart => {
      if (chart) {
        chart.destroy();
      }
    });
    this.charts = {};
  }

  // Filter Methods
  private getFilteredEntities(): string[] {
    const entityFilter = this.activeFilters.find(f => f.type === 'entity' && f.enabled);
    return entityFilter ? entityFilter.values : [];
  }

  // Drill-down Methods
  drillDownEntityGrowth(dataIndex: number) {
    if (!this.analyticsData) return;
    
    const selectedDate = this.analyticsData.entityGrowth[dataIndex].date;
    this.selectedChart = 'entityGrowth';
    this.drillDownLevel = 1;
    this.drillDownHistory.push({
      chart: 'entityGrowth',
      date: selectedDate,
      level: 0
    });
    
    this.toastService.showInfo(`Drilling down to ${selectedDate} data`);
    // In real implementation, load detailed data for this date
  }

  drillDownUsagePattern(hourIndex: number) {
    this.selectedChart = 'usagePattern';
    this.drillDownLevel = 1;
    this.drillDownHistory.push({
      chart: 'usagePattern',
      hour: hourIndex,
      level: 0
    });
    
    this.toastService.showInfo(`Drilling down to hour ${hourIndex}:00 usage details`);
  }

  drillDownDataQuality() {
    this.selectedChart = 'dataQuality';
    this.drillDownLevel = 1;
    this.drillDownHistory.push({
      chart: 'dataQuality',
      level: 0
    });
    
    this.toastService.showInfo('Showing detailed quality breakdown');
  }

  drillDownChangeRequests(statusIndex: number) {
    if (!this.analyticsData) return;
    
    const statuses = Object.keys(this.analyticsData.changeRequests.statusDistribution);
    const selectedStatus = statuses[statusIndex];
    
    this.selectedChart = 'changeRequest';
    this.drillDownLevel = 1;
    this.drillDownHistory.push({
      chart: 'changeRequest',
      status: selectedStatus,
      level: 0
    });
    
    this.toastService.showInfo(`Drilling down to ${selectedStatus} change requests`);
  }

  drillDownGeographic(countryIndex: number) {
    if (!this.analyticsData) return;
    
    const selectedCountry = this.analyticsData.geographic[countryIndex];
    
    this.selectedChart = 'geographic';
    this.drillDownLevel = 1;
    this.drillDownHistory.push({
      chart: 'geographic',
      country: selectedCountry.country,
      level: 0
    });
    
    this.toastService.showInfo(`Drilling down to ${selectedCountry.country} details`);
  }

  // Navigation Methods
  goBackToPrevious() {
    if (this.drillDownHistory.length > 0) {
      this.drillDownHistory.pop();
      this.drillDownLevel = Math.max(0, this.drillDownLevel - 1);
      
      if (this.drillDownLevel === 0) {
        this.selectedChart = null;
      }
      
      this.toastService.showInfo('Navigated back to previous level');
    }
  }

  resetToOverview() {
    this.selectedChart = null;
    this.drillDownLevel = 0;
    this.drillDownHistory = [];
    this.toastService.showInfo('Reset to overview');
  }

  // Time Range Methods
  changeTimeRange(range: TimeRange) {
    this.selectedTimeRange = range;
    this.loadAnalyticsData();
    this.toastService.showInfo(`Time range changed to ${range.label}`);
  }

  // Filter Methods
  toggleFilter(filterType: 'entity' | 'metric' | 'region', value: string) {
    const filter = this.activeFilters.find(f => f.type === filterType);
    if (!filter) return;

    const index = filter.values.indexOf(value);
    if (index > -1) {
      filter.values.splice(index, 1);
    } else {
      filter.values.push(value);
    }

    // Refresh affected charts
    this.updateAllCharts();
    this.toastService.showInfo(`Filter updated: ${filterType} - ${value}`);
  }

  toggleFilterCategory(filterType: 'entity' | 'metric' | 'region') {
    const filter = this.activeFilters.find(f => f.type === filterType);
    if (filter) {
      filter.enabled = !filter.enabled;
      this.updateAllCharts();
      this.toastService.showInfo(`${filterType} filter ${filter.enabled ? 'enabled' : 'disabled'}`);
    }
  }

  // Track by methods for ngFor
  trackByCountry(index: number, item: any): number {
    return index;
  }

  trackByPage(index: number, item: any): number {
    return index;
  }

  // Export Methods
  async exportVisualization(format: 'png' | 'pdf' | 'csv' | 'xlsx') {
    this.isExporting = true;
    this.exportFormat = format;
    this.exportProgress = 0;

    try {
      // Simulate export progress
      const progressInterval = setInterval(() => {
        this.exportProgress += Math.random() * 20;
        if (this.exportProgress >= 100) {
          this.exportProgress = 100;
          clearInterval(progressInterval);
        }
      }, 200);

      // Wait for progress to complete
      await new Promise(resolve => {
        const checkProgress = () => {
          if (this.exportProgress >= 100) {
            resolve(true);
          } else {
            setTimeout(checkProgress, 100);
          }
        };
        checkProgress();
      });

      if (format === 'png') {
        await this.exportChartsAsPNG();
      } else if (format === 'pdf') {
        await this.exportChartsAsPDF();
      } else if (format === 'csv') {
        this.exportDataAsCSV();
      } else if (format === 'xlsx') {
        this.exportDataAsXLSX();
      }

      this.toastService.showSuccess(`Analytics exported as ${format.toUpperCase()}`);
    } catch (error) {
      this.toastService.showError('Export failed');
      console.error('Export error:', error);
    } finally {
      this.isExporting = false;
      this.exportProgress = 0;
    }
  }

  private async exportChartsAsPNG() {
    // In real implementation, iterate through charts and export each as PNG
    const link = document.createElement('a');
    link.download = `analytics-charts-${new Date().toISOString().split('T')[0]}.png`;
    
    // Mock download - in real implementation, combine all chart canvases
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');
    if (ctx) {
      canvas.width = 1200;
      canvas.height = 800;
      ctx.fillStyle = '#ffffff';
      ctx.fillRect(0, 0, canvas.width, canvas.height);
      ctx.fillStyle = '#000000';
      ctx.font = '24px Arial';
      ctx.fillText('CBP Reference Data Analytics', 50, 50);
    }
    
    link.href = canvas.toDataURL();
    link.click();
  }

  private async exportChartsAsPDF() {
    // Mock PDF export - in real implementation, use libraries like jsPDF
    const blob = new Blob(['Mock PDF content'], { type: 'application/pdf' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `analytics-report-${new Date().toISOString().split('T')[0]}.pdf`;
    link.click();
    URL.revokeObjectURL(url);
  }

  private exportDataAsCSV() {
    if (!this.analyticsData) return;

    const csvContent = this.convertAnalyticsDataToCSV();
    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `analytics-data-${new Date().toISOString().split('T')[0]}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  }

  private exportDataAsXLSX() {
    // Mock XLSX export - in real implementation, use libraries like SheetJS
    const csvContent = this.convertAnalyticsDataToCSV();
    const blob = new Blob([csvContent], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `analytics-data-${new Date().toISOString().split('T')[0]}.xlsx`;
    link.click();
    URL.revokeObjectURL(url);
  }

  private convertAnalyticsDataToCSV(): string {
    if (!this.analyticsData) return '';

    const headers = ['Date', 'Countries', 'Ports', 'Airports', 'Carriers', 'Quality Score'];
    const rows = this.analyticsData.entityGrowth.map((row, index) => {
      const qualityData = this.analyticsData!.dataQuality[index] || { overall: 0 };
      return [
        row.date,
        row.countries.toString(),
        row.ports.toString(),
        row.airports.toString(),
        row.carriers.toString(),
        qualityData.overall.toFixed(2)
      ].join(',');
    });

    return [headers.join(','), ...rows].join('\n');
  }

  // Real-time Control Methods
  toggleRealTime() {
    this.realTimeEnabled = !this.realTimeEnabled;
    
    if (this.realTimeEnabled) {
      this.startRealTimeUpdates();
      this.toastService.showInfo('Real-time updates enabled');
    } else {
      this.toastService.showInfo('Real-time updates disabled');
    }
  }

  // Dashboard Configuration Methods
  reorderDashboard(fromIndex: number, toIndex: number) {
    const item = this.dashboardLayout.splice(fromIndex, 1)[0];
    this.dashboardLayout.splice(toIndex, 0, item);
    this.toastService.showInfo('Dashboard layout updated');
  }

  // Keyboard Shortcuts
  private setupKeyboardShortcuts() {
    // Handled by HostListener below
  }

  @HostListener('document:keydown', ['$event'])
  handleKeyboardShortcut(event: KeyboardEvent) {
    if (event.ctrlKey || event.metaKey) {
      switch (event.key.toLowerCase()) {
        case 'r':
          event.preventDefault();
          this.refreshData();
          break;
        case 'e':
          event.preventDefault();
          this.exportVisualization('png');
          break;
        case 'f':
          event.preventDefault();
          // Toggle first filter as example
          if (this.activeFilters.length > 0) {
            this.toggleFilterCategory('entity');
          }
          break;
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
          event.preventDefault();
          const index = parseInt(event.key) - 1;
          if (this.timeRanges[index]) {
            this.changeTimeRange(this.timeRanges[index]);
          }
          break;
      }
    }

    // Navigate drill-down with arrow keys
    if (event.key === 'Escape' && this.drillDownLevel > 0) {
      event.preventDefault();
      this.goBackToPrevious();
    }
  }

  // Utility Methods
  refreshData() {
    this.isRefreshing = true;
    this.loadAnalyticsData();
    setTimeout(() => {
      this.isRefreshing = false;
      this.toastService.showSuccess('Analytics data refreshed');
    }, 1000);
  }

  formatNumber(value: number): string {
    if (value >= 1000000) {
      return (value / 1000000).toFixed(1) + 'M';
    } else if (value >= 1000) {
      return (value / 1000).toFixed(1) + 'K';
    }
    return value.toString();
  }

  formatPercentage(value: number): string {
    return `${value.toFixed(1)}%`;
  }

  formatDuration(seconds: number): string {
    if (seconds < 60) {
      return `${seconds.toFixed(1)}s`;
    } else if (seconds < 3600) {
      return `${(seconds / 60).toFixed(1)}m`;
    } else {
      return `${(seconds / 3600).toFixed(1)}h`;
    }
  }

  getTimeRangeDescription(): string {
    return `Showing data for ${this.selectedTimeRange.label.toLowerCase()}`;
  }

  isFilterActive(filterType: 'entity' | 'metric' | 'region'): boolean {
    const filter = this.activeFilters.find(f => f.type === filterType);
    return filter ? filter.enabled : false;
  }

  getActiveFilterCount(filterType: 'entity' | 'metric' | 'region'): number {
    const filter = this.activeFilters.find(f => f.type === filterType);
    return filter ? filter.values.length : 0;
  }

  private loadMockData() {
    // Fallback mock data if API fails
    this.analyticsData = this.generateMockAnalyticsData();
    this.updateKPIs();
  }
}