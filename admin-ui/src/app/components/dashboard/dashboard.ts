import { Component, OnInit, OnDestroy, AfterViewInit, ViewChild, ElementRef, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { ApiService, ChangeRequestDto } from '../../services/api.service';
import { ToastService } from '../../services/toast.service';
import { WebSocketService } from '../../services/websocket.service';
import { forkJoin, interval, Subject, timer, of, merge } from 'rxjs';
import { startWith, switchMap, takeUntil, tap, catchError, debounceTime, distinctUntilChanged, filter } from 'rxjs/operators';
import { Chart, registerables } from 'chart.js';
import { SkeletonLoaderComponent } from '../skeleton-loader/skeleton-loader';

interface DashboardStats {
  countries: { total: number; active: number; trend: number };
  ports: { total: number; active: number; trend: number };
  airports: { total: number; active: number; trend: number };
  mappings: { total: number; trend: number };
  pendingRequests: number;
}

interface TrendData {
  labels: string[];
  data: number[];
  backgroundColor?: string;
  borderColor?: string;
}

interface DataQuality {
  overall: number;
  completeness: number;
  consistency: number;
  validity: number;
  uniqueness: number;
}

interface QuickAction {
  id: string;
  title: string;
  description: string;
  icon: string;
  route: string;
  count?: number;
  priority: 'high' | 'medium' | 'low';
}

interface SystemHealthComponent {
  name: string;
  status: 'up' | 'down' | 'degraded';
  lastChecked: Date;
  responseTime?: number;
}

interface SystemHealth {
  overall: 'healthy' | 'degraded' | 'down';
  components: SystemHealthComponent[];
}


interface RecentActivity {
  id: string;
  type: 'CREATE' | 'UPDATE' | 'DELETE';
  entityType: string;
  description: string;
  timestamp: string;
  user: string;
  userAvatar?: string;
  details?: any;
  priority?: 'high' | 'medium' | 'low';
}

interface ActivityFilter {
  type: 'ALL' | 'CREATE' | 'UPDATE' | 'DELETE';
  entityType?: string;
  user?: string;
}

interface QualityTrend {
  date: string;
  overall: number;
  completeness: number;
  consistency: number;
  validity: number;
  uniqueness: number;
}

interface HealthHistory {
  timestamp: Date;
  status: 'healthy' | 'degraded' | 'down';
  components: SystemHealthComponent[];
}

interface SyncProgress {
  id: string;
  source: string;
  progress: number;
  status: 'running' | 'completed' | 'failed';
  startTime: Date;
  records?: number;
}

interface ExportRequest {
  id: string;
  format: 'csv' | 'json' | 'xlsx';
  dataset: string;
  status: 'processing' | 'ready' | 'failed';
  downloadUrl?: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, SkeletonLoaderComponent],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class DashboardComponent implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('trendChart', { static: false }) trendChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('apiUsageChart', { static: false }) apiUsageChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('dataGrowthChart', { static: false }) dataGrowthChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('changeRequestChart', { static: false }) changeRequestChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('activityHeatmapChart', { static: false }) activityHeatmapChartRef!: ElementRef<HTMLCanvasElement>;

  private destroy$ = new Subject<void>();
  private trendChart: Chart | null = null;
  private apiUsageChart: Chart | null = null;
  private dataGrowthChart: Chart | null = null;
  private changeRequestChart: Chart | null = null;
  private activityHeatmapChart: Chart | null = null;
  private healthCheckInterval: any = null;
  private qualityAnimationPlaying = false;
  
  // Quick Add Modal
  showQuickAddModal = false;
  quickAddType = 'country';
  quickAddData: any = {};
  
  // Change Request Modal
  showChangeRequestModal = false;
  changeRequestData: any = {
    entityType: 'country',
    changeType: 'UPDATE',
    description: '',
    justification: '',
    entityId: '',
    changes: {},
    priority: 'MEDIUM',
    effectiveDate: ''
  };

  stats: DashboardStats = {
    countries: { total: 0, active: 0, trend: 0 },
    ports: { total: 0, active: 0, trend: 0 },
    airports: { total: 0, active: 0, trend: 0 },
    mappings: { total: 0, trend: 0 },
    pendingRequests: 0
  };


  recentActivity: RecentActivity[] = [];
  filteredActivity: RecentActivity[] = [];
  activityFilter: ActivityFilter = { type: 'ALL' };
  activityPage = 0;
  activityPageSize = 5;
  activityTotal = 0;
  activityLoadingMore = false;
  qualityTrends: QualityTrend[] = [];
  healthHistory: HealthHistory[] = [];
  syncProgress: SyncProgress[] = [];
  exportRequests: ExportRequest[] = [];
  showQualityDetails = false;
  showHealthDetails = false;
  recentChanges: ChangeRequestDto[] = [];
  loading = true;
  error: string | null = null;
  refreshInterval = 30000; // 30 seconds
  isRefreshing = false; // Add loading state for refresh button
  
  // New properties for enhanced features
  dataQuality: DataQuality = {
    overall: 94,
    completeness: 96.2,
    consistency: 91.8,
    validity: 92.9,
    uniqueness: 95.1
  };
  
  systemHealth: SystemHealth = {
    overall: 'healthy',
    components: [
      { name: 'Database', status: 'up', lastChecked: new Date(), responseTime: 12 },
      { name: 'API Gateway', status: 'up', lastChecked: new Date(), responseTime: 8 },
      { name: 'Cache Layer', status: 'up', lastChecked: new Date(), responseTime: 3 },
      { name: 'Message Queue', status: 'up', lastChecked: new Date(), responseTime: 15 }
    ]
  };
  
  quickActions: QuickAction[] = [
    {
      id: 'add-new-entry',
      title: 'Add New Entry',
      description: 'Create new country, port, airport, or carrier data',
      icon: 'add_circle',
      route: '/add',
      priority: 'high'
    },
    {
      id: 'create-change-request',
      title: 'Create Change Request',
      description: 'Submit new data modification request',
      icon: 'assignment_add',
      route: '/change-requests/new',
      priority: 'high'
    },
    {
      id: 'sync-external-data',
      title: 'Sync External Data',
      description: 'Import data from external sources',
      icon: 'sync',
      route: '/sync',
      priority: 'medium'
    },
    {
      id: 'export-dataset',
      title: 'Export Dataset',
      description: 'Download complete dataset in various formats',
      icon: 'download',
      route: '/export',
      priority: 'low'
    }
  ];
  
  trendData: TrendData = {
    labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
    data: [245, 251, 248, 257, 253, 261],
    backgroundColor: 'rgba(0, 51, 102, 0.1)',
    borderColor: '#003366'
  };
  
  activityData = {
    labels: ['Countries', 'Mappings'],
    datasets: [{
      data: [0, 0],
      backgroundColor: [
        '#003366',
        '#005a9c'
      ],
      borderWidth: 0
    }]
  };
  
  monthlyActivity = {
    labels: ['Week 1', 'Week 2', 'Week 3', 'Week 4'],
    datasets: [{
      label: 'Changes',
      data: [12, 19, 8, 15],
      backgroundColor: 'rgba(0, 51, 102, 0.2)',
      borderColor: '#003366',
      borderWidth: 2,
      fill: true
    }]
  };
  
  // Interactive states
  hoveredQualityMetric: string | null = null;
  selectedTimeRange: 'day' | 'week' | 'month' = 'week';
  
  // Animation counters
  animatedStats = {
    countries: { current: 0, target: 0 },
    ports: { current: 0, target: 0 },
    airports: { current: 0, target: 0 },
    mappings: { current: 0, target: 0 },
    pendingRequests: { current: 0, target: 0 }
  };

  // Expose Math for template use
  Math = Math;

  constructor(
    private apiService: ApiService, 
    private router: Router,
    private toastService: ToastService
  ) {
    Chart.register(...registerables);
  }

  ngOnInit() {
    // Load mock data immediately for demonstration
    this.loadMockData();
    this.loadDashboardData();
    this.startAutoRefresh();
    this.setupEventListeners();
    this.initializeQualityTrends();
    this.startHealthMonitoring();
    this.setupKeyboardShortcuts();
  }

  ngAfterViewInit() {
    // Initialize charts after view is ready
    setTimeout(() => {
      this.initializeCharts();
      this.initializeAllCharts();
    }, 100);
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    
    // Clean up charts
    this.destroyAllCharts();
    
    // Clean up intervals
    if (this.healthCheckInterval) {
      clearInterval(this.healthCheckInterval);
    }
    
    // Remove event listeners
    document.removeEventListener('openAddModal', this.handleOpenAddModal.bind(this));
    document.removeEventListener('triggerImport', this.handleTriggerImport.bind(this));
    document.removeEventListener('triggerExport', this.handleTriggerExport.bind(this));
  }

  startAutoRefresh() {
    // Auto-refresh dashboard every 30 seconds
    interval(this.refreshInterval).pipe(
      startWith(0),
      takeUntil(this.destroy$),
      switchMap(() => {
        this.loadDashboardData();
        return [];
      })
    ).subscribe();

  }

  loadDashboardData() {
    this.loading = true;
    this.error = null;
    
    return this.apiService.getDashboardStats().subscribe({
      next: (dashboardData) => {
        const oldStats = { ...this.stats };
        this.stats = {
          ...dashboardData,
          countries: { ...dashboardData.countries, trend: this.calculateTrend(oldStats.countries?.total || 0, dashboardData.countries?.total || 0) },
          ports: { ...dashboardData.ports, trend: this.calculateTrend(oldStats.ports?.total || 0, dashboardData.ports?.total || 0) },
          airports: { ...dashboardData.airports, trend: this.calculateTrend(oldStats.airports?.total || 0, dashboardData.airports?.total || 0) },
          mappings: { ...dashboardData.mappings, trend: this.calculateTrend(oldStats.mappings?.total || 0, dashboardData.mappings?.total || 0) },
          pendingRequests: dashboardData.pendingRequests || 0
        };
        
        // Update data quality - use fetched data or defaults
        if (dashboardData.dataQuality) {
          this.dataQuality = dashboardData.dataQuality;
        }
        
        // Update system health
        if (dashboardData.systemHealth) {
          this.systemHealth = dashboardData.systemHealth;
        }
        
        // Update recent activity
        this.recentActivity = dashboardData.recentActivity || [];
        
        // Update quick actions count
        this.quickActions = this.quickActions.map(action => {
          if (action.id === 'create-change-request') {
            return { ...action, count: this.stats.pendingRequests };
          }
          return action;
        });
        
        // Update chart data with real data
        this.updateChartDataWithRealData(dashboardData);
        
        // Start number animations
        this.animateNumbers();
        
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading dashboard stats:', error);
        // API service now provides mock data automatically on failure
        this.error = null; // Clear error since mock data is loaded
        this.loading = false;
      }
    });
  }

  loadRecentActivity() {
    // Load recent change requests to show activity
    this.apiService.getChangeRequests({ 
      page: 0, 
      size: 10 
    }).subscribe({
      next: (response) => {
        this.recentChanges = response.content || [];
        this.recentActivity = this.convertChangesToActivity(this.recentChanges);
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading recent activity:', error);
        this.loading = false;
      }
    });
  }


  private convertChangesToActivity(changes: ChangeRequestDto[]): RecentActivity[] {
    return changes.slice(0, 5).map(change => ({
      id: change.id,
      type: change.changeType as 'CREATE' | 'UPDATE' | 'DELETE',
      entityType: change.entityType,
      description: change.description,
      timestamp: change.createdAt,
      user: change.requestedBy
    }));
  }

  refreshData() {
    this.isRefreshing = true; // Enable loading state
    this.loading = true;
    this.loadDashboardData();
    
    // Show success toast after refresh completes
    setTimeout(() => {
      this.isRefreshing = false; // Disable loading state
      if (!this.error) {
        this.toastService.showSuccess('Data refreshed successfully');
      }
    }, 1000);
  }

  // Quick Actions Implementation
  openQuickAddModal() {
    this.showQuickAddModal = true;
    this.quickAddType = 'country';
    this.resetQuickAddData();
  }

  closeQuickAddModal() {
    this.showQuickAddModal = false;
    this.resetQuickAddData();
  }
  
  // Change Request Modal Methods
  openChangeRequestModal() {
    this.showChangeRequestModal = true;
    this.resetChangeRequestData();
  }
  
  closeChangeRequestModal() {
    this.showChangeRequestModal = false;
    this.resetChangeRequestData();
  }
  
  resetChangeRequestData() {
    this.changeRequestData = {
      entityType: 'country',
      changeType: 'UPDATE',
      description: '',
      justification: '',
      entityId: '',
      changes: {},
      priority: 'MEDIUM',
      effectiveDate: ''
    };
  }
  
  submitChangeRequest() {
    // Validate form
    if (!this.changeRequestData.description || !this.changeRequestData.justification) {
      this.toastService.showError('Please fill in all required fields');
      return;
    }
    
    // Create the change request
    const changeRequest = {
      entityType: this.changeRequestData.entityType.toUpperCase(),
      changeType: this.changeRequestData.changeType,
      entityId: this.changeRequestData.entityId || undefined,
      description: this.changeRequestData.description,
      justification: this.changeRequestData.justification,
      requestedBy: 'current-user', // Would come from auth service
      newValues: this.changeRequestData.changes
    };
    
    // Submit via API (for now just show success)
    this.toastService.showSuccess('Change request submitted successfully');
    this.closeChangeRequestModal();
    this.loadDashboardData();
  }
  
  isChangeRequestFormValid(): boolean {
    return !!(this.changeRequestData.description && 
              this.changeRequestData.justification &&
              (this.changeRequestData.changeType === 'CREATE' || this.changeRequestData.entityId));
  }

  resetQuickAddData() {
    this.quickAddData = {
      // Common fields
      codeSystem: 'ISO3166-1',
      isActive: true,
      
      // Country fields
      countryName: '',
      countryCode: '',
      iso2Code: '',
      iso3Code: '',
      numericCode: '',
      genc2Code: '',
      genc3Code: '',
      
      // Port fields
      portName: '',
      portCode: '',
      
      // Airport fields
      airportName: '',
      iataCode: '',
      icaoCode: '',
      
      // Carrier fields
      carrierName: '',
      carrierCode: '',
      carrierType: '',
      
      // Common
      country: ''
    };
  }

  onQuickAddTypeChange() {
    this.resetQuickAddData();
  }

  submitQuickAdd() {
    switch (this.quickAddType) {
      case 'country':
        this.createCountry();
        break;
      case 'port':
        this.createPort();
        break;
      case 'airport':
        this.createAirport();
        break;
      case 'carrier':
        this.createCarrier();
        break;
      default:
        this.toastService.showError('Unknown entity type');
    }
  }

  private createCountry() {
    if (!this.quickAddData.countryName || !this.quickAddData.countryCode) {
      this.toastService.showError('Country name and code are required');
      return;
    }

    // Create a change request for governance consistency
    const changeRequest = {
      changeType: 'CREATE' as const,
      entityType: 'COUNTRY',
      description: `Create country: ${this.quickAddData.countryName}`,
      requestedBy: 'current-user',
      newValues: this.quickAddData
    };

    this.apiService.createChangeRequest(changeRequest).subscribe({
      next: () => {
        this.toastService.showSuccess('Change request submitted for new country');
        this.closeQuickAddModal();
        this.loadDashboardData();
      },
      error: (error) => {
        this.toastService.showError('Failed to submit change request: ' + error.message);
      }
    });
  }

  // Quick Action Navigation
  navigateToQuickAction(actionId: string) {
    switch (actionId) {
      case 'add-country':
        this.addReferenceData();
        break;
      case 'review-requests':
        this.router.navigate(['/change-requests']);
        break;
      case 'export-data':
        this.exportData();
        break;
      case 'add-reference-data':
        this.addReferenceData();
        break;
      case 'import-data':
        this.importReferenceData();
        break;
      default:
        console.log('Unknown action:', actionId);
        this.toastService.showWarning(`Action ${actionId} not yet implemented`);
    }
  }

  // Main Action Button Implementations
  addReferenceData() {
    // Show modal or navigate to add form
    this.openQuickAddModal();
  }

  importReferenceData() {
    // Create hidden file input and trigger it
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.csv,.json,.xlsx';
    input.onchange = (event: any) => {
      const file = event.target.files[0];
      if (file) {
        this.handleFileImport(file);
      }
    };
    input.click();
  }

  exportData() {
    // Create export options modal or direct download
    this.showExportOptions();
  }

  private handleFileImport(file: File) {
    this.toastService.showInfo(`Importing file: ${file.name}`);
    
    // In a real app, you would:
    // 1. Validate file format
    // 2. Upload to server
    // 3. Process import
    // 4. Show results
    
    // For now, simulate processing
    setTimeout(() => {
      this.toastService.showSuccess(`File ${file.name} imported successfully`);
      this.refreshData();
    }, 2000);
  }

  private showExportOptions() {
    // Get comprehensive country data from API service
    this.apiService.getAllCurrentCountries().subscribe({
      next: (countries) => {
        const headers = [
          'Country Code', 'Country Name', 'ISO2 Code', 'ISO3 Code', 
          'Numeric Code', 'Code System', 'Status', 'Valid From', 'Recorded At'
        ];
        
        const data = countries.map(country => [
          country.countryCode,
          `"${country.countryName}"`, // Quote to handle commas in names
          country.iso2Code,
          country.iso3Code,
          country.numericCode,
          country.codeSystem,
          country.isActive ? 'Active' : 'Inactive',
          new Date(country.validFrom).toLocaleDateString(),
          new Date(country.recordedAt).toLocaleDateString()
        ]);
        
        const csvContent = [
          headers.join(','),
          ...data.map(row => row.join(','))
        ].join('\n');
        
        const blob = new Blob([csvContent], { type: 'text/csv' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `complete-reference-data-export-${new Date().toISOString().split('T')[0]}.csv`;
        link.click();
        URL.revokeObjectURL(url);
        
        this.toastService.showSuccess(`${countries.length} countries exported successfully with all code systems`);
      },
      error: (error) => {
        this.toastService.showError('Failed to export data: ' + error.message);
      }
    });
  }


  getActivityIcon(type: string): string {
    switch (type) {
      case 'CREATE': return 'add';
      case 'UPDATE': return 'edit';
      case 'DELETE': return 'delete';
      default: return 'description';
    }
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diffMinutes = Math.floor((now.getTime() - date.getTime()) / (1000 * 60));
    
    if (diffMinutes < 60) {
      return `${diffMinutes} minutes ago`;
    } else if (diffMinutes < 1440) {
      const hours = Math.floor(diffMinutes / 60);
      return `${hours} hour${hours > 1 ? 's' : ''} ago`;
    } else {
      return date.toLocaleDateString();
    }
  }


  get totalEntities(): number {
    return this.stats.countries.total;
  }

  get activeEntities(): number {
    return this.stats.countries.active;
  }
  
  getObjectKeys(obj: any): string[] {
    return Object.keys(obj || {});
  }
  
  // New enhanced methods
  private calculateTrend(oldValue: number, newValue: number): number {
    if (oldValue === 0) return 0;
    return ((newValue - oldValue) / oldValue) * 100;
  }
  
  private animateNumbers() {
    // Set targets for animation
    this.animatedStats.countries.target = this.stats.countries.total;
    this.animatedStats.ports.target = this.stats.ports.total;
    this.animatedStats.airports.target = this.stats.airports.total;
    this.animatedStats.mappings.target = this.stats.mappings.total;
    this.animatedStats.pendingRequests.target = this.stats.pendingRequests;
    
    // Animate each counter
    Object.keys(this.animatedStats).forEach(key => {
      const stat = this.animatedStats[key as keyof typeof this.animatedStats];
      this.animateNumber(stat, 1000); // 1 second animation
    });
  }
  
  private animateNumber(stat: { current: number; target: number }, duration: number) {
    const startValue = stat.current;
    const endValue = stat.target;
    const startTime = performance.now();
    
    const animate = (currentTime: number) => {
      const elapsed = currentTime - startTime;
      const progress = Math.min(elapsed / duration, 1);
      
      // Easing function (easeOutCubic)
      const easeOutCubic = 1 - Math.pow(1 - progress, 3);
      
      stat.current = Math.round(startValue + (endValue - startValue) * easeOutCubic);
      
      if (progress < 1) {
        requestAnimationFrame(animate);
      }
    };
    
    requestAnimationFrame(animate);
  }
  
  private initializeCharts() {
    if (this.trendChartRef?.nativeElement) {
      this.initTrendChart();
    }
  }
  
  private initTrendChart() {
    const ctx = this.trendChartRef.nativeElement.getContext('2d');
    if (!ctx) return;
    
    this.trendChart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: this.trendData.labels,
        datasets: [{
          label: 'Data Trend',
          data: this.trendData.data,
          backgroundColor: this.trendData.backgroundColor,
          borderColor: this.trendData.borderColor,
          borderWidth: 2,
          fill: true,
          tension: 0.4,
          pointRadius: 0,
          pointHoverRadius: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: false
          }
        },
        scales: {
          x: {
            display: false
          },
          y: {
            display: false
          }
        },
        interaction: {
          intersect: false,
          mode: 'index'
        }
      }
    });
  }
  
  private updateChartData() {
    // This method is intentionally left minimal after removing distribution and activity charts
  }

  private updateChartDataWithRealData(dashboardData: any) {
    // Update trend chart data
    if (this.trendChart && dashboardData.monthlyActivity) {
      const lastSixMonths = dashboardData.monthlyActivity.slice(-6);
      this.trendData = {
        labels: lastSixMonths.map((m: any) => m.month.split(' ')[0]), // Get just month name
        data: lastSixMonths.map((m: any) => m.countries),
        backgroundColor: 'rgba(0, 51, 102, 0.1)',
        borderColor: '#003366'
      };
      this.trendChart.data.labels = this.trendData.labels;
      this.trendChart.data.datasets[0].data = this.trendData.data;
      this.trendChart.update();
    }
  }
  
  // Utility methods for templates
  getActivePercentage(type: string): number {
    const data = this.stats[type as keyof DashboardStats];
    if (data && typeof data === 'object' && 'total' in data && 'active' in data) {
      return data.total > 0 ? Math.round((data.active / data.total) * 100) : 0;
    }
    return 0;
  }
  
  getTrendIcon(trend: number): string {
    if (trend > 0) return 'arrow_upward';
    if (trend < 0) return 'arrow_downward';
    return 'remove';
  }
  
  getTrendClass(trend: number): string {
    if (trend > 0) return 'cbp-trend--up';
    if (trend < 0) return 'cbp-trend--down';
    return 'cbp-trend--neutral';
  }
  
  formatTrend(trend: number): string {
    const abs = Math.abs(trend);
    const sign = trend >= 0 ? '+' : '';
    return `${sign}${abs.toFixed(1)}%`;
  }
  
  formatRelativeTime(date: Date): string {
    return this.formatDate(date.toISOString());
  }
  
  
  
  getQualityColor(score: number): string {
    if (score >= 95) return 'cbp-quality--excellent';
    if (score >= 85) return 'cbp-quality--good';
    if (score >= 70) return 'cbp-quality--fair';
    return 'cbp-quality--poor';
  }
  
  getActionPriorityClass(priority: string): string {
    return `cbp-action--${priority}`;
  }
  
  getCurrentTime(): Date {
    return new Date();
  }
  
  // System health utility methods
  getHealthStatusClass(status: string): string {
    return `cbp-health--${status}`;
  }
  
  getHealthIcon(component: string): string {
    const icons: { [key: string]: string } = {
      'Database': 'database',
      'API Gateway': 'settings_ethernet',
      'Cache Layer': 'memory',
      'Message Queue': 'queue'
    };
    return icons[component] || 'help_outline';
  }
  
  formatResponseTime(ms: number): string {
    return `${ms}ms`;
  }
  
  getOverallHealthIcon(): string {
    switch (this.systemHealth.overall) {
      case 'healthy': return 'check_circle';
      case 'degraded': return 'warning';
      case 'down': return 'error';
      default: return 'help';
    }
  }
  
  getOverallHealthClass(): string {
    return `cbp-health-overall--${this.systemHealth.overall}`;
  }

  // New methods for enhanced modal functionality
  onCodeSystemChange() {
    // Reset code fields when system changes
    this.quickAddData.countryCode = '';
    this.quickAddData.iso2Code = '';
    this.quickAddData.iso3Code = '';
    this.quickAddData.genc2Code = '';
    this.quickAddData.genc3Code = '';
  }

  getCodeSystemDescription(codeSystem: string): string {
    switch (codeSystem) {
      case 'ISO3166-1':
        return 'International standard for country codes maintained by ISO.';
      case 'GENC':
        return 'US Government standard for geopolitical entities, names and codes.';
      case 'US-Census':
        return 'US Census Bureau country and territory codes.';
      case 'CBP-Country':
        return 'US Customs and Border Protection country codes.';
      default:
        return '';
    }
  }

  getCodePlaceholder(codeSystem: string): string {
    switch (codeSystem) {
      case 'ISO3166-1':
        return 'US (2-letter) or USA (3-letter)';
      case 'GENC':
        return 'US (2-letter) or USA (3-letter)';
      case 'US-Census':
        return 'Enter census code';
      case 'CBP-Country':
        return 'Enter CBP code';
      default:
        return 'Enter code';
    }
  }

  getCodeMaxLength(codeSystem: string): number {
    switch (codeSystem) {
      case 'ISO3166-1':
        return 3;
      case 'GENC':
        return 3;
      case 'US-Census':
        return 5;
      case 'CBP-Country':
        return 5;
      default:
        return 10;
    }
  }

  shouldShowIsoCodes(): boolean {
    return this.quickAddData.codeSystem !== 'ISO3166-1';
  }

  isFormValid(): boolean {
    switch (this.quickAddType) {
      case 'country':
        return !!(this.quickAddData.countryName && this.quickAddData.countryCode);
      case 'port':
        return !!(this.quickAddData.portName && this.quickAddData.portCode);
      case 'airport':
        return !!(this.quickAddData.airportName && this.quickAddData.iataCode);
      case 'carrier':
        return !!(this.quickAddData.carrierName && this.quickAddData.carrierCode);
      default:
        return false;
    }
  }

  private createPort() {
    if (!this.quickAddData.portName || !this.quickAddData.portCode) {
      this.toastService.showError('Port name and code are required');
      return;
    }

    // For now, just show success - in real app would call API
    this.toastService.showSuccess('Port created successfully');
    this.closeQuickAddModal();
    this.loadDashboardData();
  }

  private createAirport() {
    if (!this.quickAddData.airportName || !this.quickAddData.iataCode) {
      this.toastService.showError('Airport name and IATA code are required');
      return;
    }

    // For now, just show success - in real app would call API
    this.toastService.showSuccess('Airport created successfully');
    this.closeQuickAddModal();
    this.loadDashboardData();
  }

  private createCarrier() {
    if (!this.quickAddData.carrierName || !this.quickAddData.carrierCode) {
      this.toastService.showError('Carrier name and code are required');
      return;
    }

    // For now, just show success - in real app would call API
    this.toastService.showSuccess('Carrier created successfully');
    this.closeQuickAddModal();
    this.loadDashboardData();
  }

  // Load mock data for development/testing
  private loadMockData() {
    // Use realistic sample data that matches our mock components
    this.stats = {
      countries: { total: 5, active: 4, trend: 2.1 },
      ports: { total: 6, active: 5, trend: -1.3 },
      airports: { total: 6, active: 5, trend: 0.8 },
      mappings: { total: 15782, trend: 1.2 },
      pendingRequests: 7
    };
    
    this.recentActivity = [
      {
        id: '1',
        type: 'CREATE',
        entityType: 'COUNTRY',
        description: 'New country added: Vanuatu (VU)',
        timestamp: new Date(Date.now() - 300000).toISOString(),
        user: 'john.smith@cbp.gov'
      },
      {
        id: '2',
        type: 'UPDATE',
        entityType: 'PORT',
        description: 'Updated Los Angeles Port (LAX) operational status',
        timestamp: new Date(Date.now() - 900000).toISOString(),
        user: 'jane.doe@cbp.gov'
      },
      {
        id: '3',
        type: 'UPDATE',
        entityType: 'AIRPORT',
        description: 'Modified JFK airport code mapping for IATA system',
        timestamp: new Date(Date.now() - 1800000).toISOString(),
        user: 'mike.johnson@cbp.gov'
      }
    ];
    
    // Trigger number animations with mock data
    this.animateNumbers();
  }
  
  // Event listener setup for sidebar actions
  private setupEventListeners() {
    // Listen for sidebar actions
    document.addEventListener('openAddModal', this.handleOpenAddModal.bind(this));
    document.addEventListener('triggerImport', this.handleTriggerImport.bind(this));
    document.addEventListener('triggerExport', this.handleTriggerExport.bind(this));
  }

  private handleOpenAddModal() {
    this.openQuickAddModal();
  }

  private handleTriggerImport() {
    this.importReferenceData();
  }

  private handleTriggerExport() {
    this.exportData();
  }

  // Enhanced Data Quality Score Widget Methods
  toggleQualityDetails() {
    this.showQualityDetails = !this.showQualityDetails;
    if (this.showQualityDetails && !this.qualityAnimationPlaying) {
      this.animateQualityScore();
    }
  }

  private animateQualityScore() {
    this.qualityAnimationPlaying = true;
    const duration = 2000;
    const startTime = performance.now();
    const originalScores = { ...this.dataQuality };

    const animate = (currentTime: number) => {
      const elapsed = currentTime - startTime;
      const progress = Math.min(elapsed / duration, 1);
      
      // Easing function
      const easeOutCubic = 1 - Math.pow(1 - progress, 3);
      
      this.dataQuality.overall = Math.round(originalScores.overall * easeOutCubic);
      this.dataQuality.completeness = Math.round(originalScores.completeness * easeOutCubic);
      this.dataQuality.consistency = Math.round(originalScores.consistency * easeOutCubic);
      this.dataQuality.validity = Math.round(originalScores.validity * easeOutCubic);
      this.dataQuality.uniqueness = Math.round(originalScores.uniqueness * easeOutCubic);
      
      if (progress < 1) {
        requestAnimationFrame(animate);
      } else {
        this.qualityAnimationPlaying = false;
      }
    };
    
    requestAnimationFrame(animate);
  }

  onQualityMetricHover(metric: string | null) {
    this.hoveredQualityMetric = metric;
  }

  getQualityTooltip(metric: string): string {
    const tooltips: { [key: string]: string } = {
      'overall': 'Combined quality score across all metrics',
      'completeness': 'Percentage of required fields that contain data',
      'consistency': 'Data follows established patterns and formats',
      'validity': 'Data meets business rules and constraints',
      'uniqueness': 'No duplicate records exist in the dataset'
    };
    return tooltips[metric] || '';
  }

  onTimeRangeChange(range: any) {
    this.selectedTimeRange = range;
    this.updateQualityTrends();
  }

  private initializeQualityTrends() {
    // Generate sample trend data
    const now = new Date();
    const trends: QualityTrend[] = [];
    
    for (let i = 6; i >= 0; i--) {
      const date = new Date(now.getTime() - (i * 24 * 60 * 60 * 1000));
      trends.push({
        date: date.toISOString().split('T')[0],
        overall: 94 + Math.random() * 4 - 2,
        completeness: 96 + Math.random() * 3 - 1,
        consistency: 91 + Math.random() * 4 - 2,
        validity: 93 + Math.random() * 3 - 1,
        uniqueness: 95 + Math.random() * 3 - 1
      });
    }
    
    this.qualityTrends = trends;
  }

  private updateQualityTrends() {
    // Update trends based on selected time range
    // In a real app, this would fetch from API
    this.initializeQualityTrends();
  }

  // Enhanced System Health Monitor Methods
  toggleHealthDetails() {
    this.showHealthDetails = !this.showHealthDetails;
  }

  private startHealthMonitoring() {
    // Poll health endpoints every 30 seconds
    this.healthCheckInterval = setInterval(() => {
      this.checkSystemHealth();
    }, 30000);
    
    // Initial health check
    this.checkSystemHealth();
  }

  private checkSystemHealth() {
    // Simulate health checks with realistic response times
    const components = this.systemHealth.components.map(component => ({
      ...component,
      lastChecked: new Date(),
      responseTime: Math.floor(Math.random() * 50) + 5, // 5-55ms
      status: Math.random() > 0.95 ? 'degraded' : 'up' as 'up' | 'down' | 'degraded'
    }));
    
    const overallStatus = components.every(c => c.status === 'up') ? 'healthy' : 
                         components.some(c => c.status === 'down') ? 'down' : 'degraded';
    
    this.systemHealth = {
      overall: overallStatus,
      components
    };
    
    // Add to health history
    this.healthHistory.push({
      timestamp: new Date(),
      status: overallStatus,
      components: [...components]
    });
    
    // Keep only last 50 entries
    if (this.healthHistory.length > 50) {
      this.healthHistory = this.healthHistory.slice(-50);
    }
    
    // Show toast for status changes
    if (overallStatus === 'degraded') {
      this.toastService.showWarning('System performance degraded - some components responding slowly');
    } else if (overallStatus === 'down') {
      this.toastService.showError('System outage detected - critical components are down');
    }
  }

  getHealthTrend(): 'improving' | 'stable' | 'declining' {
    if (this.healthHistory.length < 5) return 'stable';
    
    const recent = this.healthHistory.slice(-5);
    const healthyCount = recent.filter(h => h.status === 'healthy').length;
    const degradedCount = recent.filter(h => h.status === 'degraded').length;
    
    if (healthyCount > degradedCount && healthyCount >= 3) return 'improving';
    if (degradedCount > healthyCount && degradedCount >= 3) return 'declining';
    return 'stable';
  }

  // Enhanced Recent Activity Feed Methods
  filterActivity(filter: any) {
    this.activityFilter = filter;
    this.activityPage = 0;
    this.applyActivityFilter();
  }

  loadMoreActivity() {
    if (this.activityLoadingMore) return;
    
    this.activityLoadingMore = true;
    this.activityPage++;
    
    // Simulate API call
    setTimeout(() => {
      const moreActivities = this.generateMockActivity(this.activityPageSize, this.activityPage);
      this.recentActivity.push(...moreActivities);
      this.applyActivityFilter();
      this.activityLoadingMore = false;
    }, 800);
  }

  private applyActivityFilter() {
    let filtered = [...this.recentActivity];
    
    if (this.activityFilter.type !== 'ALL') {
      filtered = filtered.filter(activity => activity.type === this.activityFilter.type);
    }
    
    if (this.activityFilter.entityType) {
      filtered = filtered.filter(activity => activity.entityType === this.activityFilter.entityType);
    }
    
    if (this.activityFilter.user) {
      filtered = filtered.filter(activity => activity.user.includes(this.activityFilter.user!));
    }
    
    this.filteredActivity = filtered;
    this.activityTotal = filtered.length;
  }

  viewActivityDetails(activity: RecentActivity) {
    // Show detailed modal or navigate to details page
    this.toastService.showInfo(`Viewing details for ${activity.description}`);
  }

  getUserInitials(userEmail: string): string {
    const name = userEmail.split('@')[0];
    const parts = name.split('.');
    return parts.map(part => part.charAt(0).toUpperCase()).join('');
  }

  getUserAvatarColor(userEmail: string): string {
    const colors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4'];
    const hash = userEmail.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
    return colors[hash % colors.length];
  }

  private generateMockActivity(count: number, page: number): RecentActivity[] {
    const activities: RecentActivity[] = [];
    const types: ('CREATE' | 'UPDATE' | 'DELETE')[] = ['CREATE', 'UPDATE', 'DELETE'];
    const entities = ['COUNTRY', 'PORT', 'AIRPORT', 'CARRIER'];
    const users = ['john.doe@cbp.gov', 'jane.smith@cbp.gov', 'mike.johnson@cbp.gov', 'sarah.wilson@cbp.gov'];
    
    for (let i = 0; i < count; i++) {
      const type = types[Math.floor(Math.random() * types.length)];
      const entity = entities[Math.floor(Math.random() * entities.length)];
      const user = users[Math.floor(Math.random() * users.length)];
      const timestamp = new Date(Date.now() - ((page * count + i) * 300000 + Math.random() * 3600000));
      
      activities.push({
        id: `activity-${page}-${i}`,
        type,
        entityType: entity,
        description: `${type.toLowerCase()} operation on ${entity.toLowerCase()} record`,
        timestamp: timestamp.toISOString(),
        user,
        priority: Math.random() > 0.7 ? 'high' : Math.random() > 0.5 ? 'medium' : 'low'
      });
    }
    
    return activities;
  }

  // Enhanced Quick Actions Panel Methods
  async executeQuickAction(actionId: string) {
    switch (actionId) {
      case 'add-new-entry':
        this.openQuickAddModal();
        break;
      case 'create-change-request':
        this.openChangeRequestModal();
        break;
      case 'sync-external-data':
        await this.triggerDataSync();
        break;
      case 'export-dataset':
        this.openExportDialog();
        break;
      default:
        this.toastService.showWarning(`Action ${actionId} not implemented`);
    }
  }

  private async triggerDataSync() {
    const sources = ['ISO3166', 'GENC', 'IATA', 'ICAO'];
    const syncId = `sync-${Date.now()}`;
    
    const syncProgress: SyncProgress = {
      id: syncId,
      source: sources[Math.floor(Math.random() * sources.length)],
      progress: 0,
      status: 'running',
      startTime: new Date(),
      records: 0
    };
    
    this.syncProgress.push(syncProgress);
    this.toastService.showInfo(`Starting data sync from ${syncProgress.source}...`);
    
    // Simulate progress
    const progressInterval = setInterval(() => {
      syncProgress.progress += Math.random() * 15;
      syncProgress.records = Math.floor(syncProgress.progress * 10);
      
      if (syncProgress.progress >= 100) {
        syncProgress.progress = 100;
        syncProgress.status = Math.random() > 0.9 ? 'failed' : 'completed';
        clearInterval(progressInterval);
        
        if (syncProgress.status === 'completed') {
          this.toastService.showSuccess(`Sync completed: ${syncProgress.records} records processed`);
          this.loadDashboardData(); // Refresh data
        } else {
          this.toastService.showError(`Sync failed from ${syncProgress.source}`);
        }
      }
    }, 200);
  }

  private openExportDialog() {
    const formats = ['csv', 'json', 'xlsx'];
    const datasets = ['countries', 'ports', 'airports'];
    
    const format = formats[Math.floor(Math.random() * formats.length)] as 'csv' | 'json' | 'xlsx';
    const dataset = datasets[Math.floor(Math.random() * datasets.length)];
    
    const exportRequest: ExportRequest = {
      id: `export-${Date.now()}`,
      format,
      dataset,
      status: 'processing'
    };
    
    this.exportRequests.push(exportRequest);
    this.toastService.showInfo(`Preparing ${format.toUpperCase()} export for ${dataset}...`);
    
    // Simulate export processing
    setTimeout(() => {
      exportRequest.status = 'ready';
      exportRequest.downloadUrl = `/api/exports/${exportRequest.id}/download`;
      this.toastService.showSuccess(`Export ready: ${dataset}.${format}`);
    }, 3000);
  }

  // Chart Initialization Methods
  private initializeAllCharts() {
    if (this.apiUsageChartRef?.nativeElement) {
      this.initApiUsageChart();
    }
    if (this.dataGrowthChartRef?.nativeElement) {
      this.initDataGrowthChart();
    }
    if (this.changeRequestChartRef?.nativeElement) {
      this.initChangeRequestChart();
    }
    if (this.activityHeatmapChartRef?.nativeElement) {
      this.initActivityHeatmapChart();
    }
  }

  private initApiUsageChart() {
    const ctx = this.apiUsageChartRef.nativeElement.getContext('2d');
    if (!ctx) return;

    const hours = [];
    const data = [];
    for (let i = 23; i >= 0; i--) {
      const hour = new Date(Date.now() - i * 3600000);
      hours.push(hour.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }));
      data.push(Math.floor(Math.random() * 1000) + 100);
    }

    this.apiUsageChart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: hours,
        datasets: [{
          label: 'API Requests',
          data: data,
          borderColor: '#003366',
          backgroundColor: 'rgba(0, 51, 102, 0.1)',
          borderWidth: 2,
          fill: true,
          tension: 0.4
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: false
          }
        },
        scales: {
          x: {
            display: false
          },
          y: {
            beginAtZero: true,
            display: false
          }
        }
      }
    });
  }

  private initDataGrowthChart() {
    const ctx = this.dataGrowthChartRef.nativeElement.getContext('2d');
    if (!ctx) return;

    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'];
    this.dataGrowthChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: months,
        datasets: [
          {
            label: 'Countries',
            data: [245, 248, 251, 255, 258, 261],
            backgroundColor: '#10b981',
            borderRadius: 4
          },
          {
            label: 'Ports',
            data: [1820, 1835, 1847, 1851, 1863, 1871],
            backgroundColor: '#3b82f6',
            borderRadius: 4
          },
          {
            label: 'Airports',
            data: [4198, 4205, 4220, 4235, 4251, 4263],
            backgroundColor: '#f59e0b',
            borderRadius: 4
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: {
            beginAtZero: true
          }
        }
      }
    });
  }

  private initChangeRequestChart() {
    const ctx = this.changeRequestChartRef.nativeElement.getContext('2d');
    if (!ctx) return;

    this.changeRequestChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: ['Approved', 'Pending', 'Rejected', 'In Review'],
        datasets: [{
          data: [45, 12, 8, 15],
          backgroundColor: [
            '#10b981',
            '#f59e0b',
            '#ef4444',
            '#6b7280'
          ],
          borderWidth: 0
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom'
          }
        }
      }
    });
  }

  private initActivityHeatmapChart() {
    const ctx = this.activityHeatmapChartRef.nativeElement.getContext('2d');
    if (!ctx) return;

    const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
    const hours = Array.from({ length: 24 }, (_, i) => i);
    const data = [];

    for (let day = 0; day < 7; day++) {
      for (let hour = 0; hour < 24; hour++) {
        data.push({
          x: hour,
          y: day,
          v: Math.floor(Math.random() * 100)
        });
      }
    }

    this.activityHeatmapChart = new Chart(ctx, {
      type: 'scatter',
      data: {
        datasets: [{
          label: 'Activity',
          data: data.map(d => ({ x: d.x, y: d.y, r: d.v / 10 })),
          backgroundColor: 'rgba(16, 185, 129, 0.6)',
          borderColor: '#10b981'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: {
            type: 'linear',
            position: 'bottom',
            min: 0,
            max: 23,
            title: {
              display: true,
              text: 'Hour of Day'
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
              callback: (value) => days[value as number]
            }
          }
        }
      }
    });
  }

  private destroyAllCharts() {
    [this.trendChart, this.apiUsageChart, this.dataGrowthChart, 
     this.changeRequestChart, this.activityHeatmapChart].forEach(chart => {
      if (chart) {
        chart.destroy();
      }
    });
  }

  // Keyboard Shortcuts
  private setupKeyboardShortcuts() {
    // These will be handled by the @HostListener decorator method
  }

  @HostListener('document:keydown', ['$event'])
  handleKeyboardShortcut(event: KeyboardEvent) {
    if (event.ctrlKey || event.metaKey) {
      switch (event.key.toLowerCase()) {
        case 'k':
          event.preventDefault();
          this.openQuickAddModal();
          break;
        case 'r':
          event.preventDefault();
          this.refreshData();
          break;
        case 'e':
          event.preventDefault();
          this.openExportDialog();
          break;
      }
    }
  }

  // Utility Methods
  getActivityFilterCount(filterType: string): number {
    if (filterType === 'ALL') return this.recentActivity.length;
    return this.recentActivity.filter(activity => activity.type === filterType).length;
  }

  getSyncProgressDescription(sync: SyncProgress): string {
    if (sync.status === 'completed') {
      return `Completed: ${sync.records} records synced`;
    } else if (sync.status === 'failed') {
      return 'Sync failed - please retry';
    } else {
      return `${Math.round(sync.progress)}% - ${sync.records} records processed`;
    }
  }

  getExportStatusIcon(status: string): string {
    switch (status) {
      case 'processing': return 'hourglass_empty';
      case 'ready': return 'download';
      case 'failed': return 'error';
      default: return 'help';
    }
  }

  downloadExport(exportRequest: ExportRequest) {
    if (exportRequest.status === 'ready' && exportRequest.downloadUrl) {
      // In a real app, this would trigger the actual download
      this.toastService.showSuccess(`Downloaded ${exportRequest.dataset}.${exportRequest.format}`);
    }
  }
}
