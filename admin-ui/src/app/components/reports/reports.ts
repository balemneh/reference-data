import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { ToastService } from '../../services/toast.service';
import { Subject, forkJoin, interval, timer } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged, switchMap, catchError } from 'rxjs/operators';
import { Chart, registerables } from 'chart.js';

// Register Chart.js components
Chart.register(...registerables);

// Report Interfaces
export interface ReportTemplate {
  id: string;
  name: string;
  description: string;
  category: ReportCategory;
  type: ReportType;
  fields: ReportField[];
  visualizationType: VisualizationType;
  icon: string;
  estimatedTime: string;
  isDefault: boolean;
  permissions: string[];
  tags: string[];
}

export interface ReportField {
  id: string;
  name: string;
  label: string;
  type: 'string' | 'number' | 'date' | 'boolean' | 'enum';
  required: boolean;
  defaultValue?: any;
  options?: string[];
  format?: string;
  validation?: ReportFieldValidation;
}

export interface ReportFieldValidation {
  min?: number;
  max?: number;
  pattern?: string;
  message?: string;
}

export interface ReportData {
  headers: string[];
  rows: any[][];
  totalRecords: number;
  executionTime: number;
  generatedAt: Date;
  metadata: ReportMetadata;
}

export interface ReportMetadata {
  reportId: string;
  name: string;
  description: string;
  parameters: { [key: string]: any };
  dataSource: string;
  refreshRate?: number;
  lastRefresh?: Date;
}

export interface ReportExecution {
  id: string;
  reportId: string;
  status: ReportStatus;
  progress: number;
  startTime: Date;
  endTime?: Date;
  parameters: { [key: string]: any };
  result?: ReportData;
  error?: string;
  userId: string;
  downloadUrl?: string;
}

export interface ScheduledReport {
  id: string;
  reportId: string;
  name: string;
  schedule: ReportSchedule;
  recipients: string[];
  format: ExportFormat;
  isActive: boolean;
  lastRun?: Date;
  nextRun?: Date;
  parameters: { [key: string]: any };
  createdBy: string;
  createdAt: Date;
}

export interface ReportSchedule {
  type: 'once' | 'daily' | 'weekly' | 'monthly' | 'quarterly' | 'yearly';
  time: string; // HH:mm format
  dayOfWeek?: number; // 0-6 for weekly
  dayOfMonth?: number; // 1-31 for monthly
  timezone: string;
  endDate?: Date;
}

export interface CustomReportBuilder {
  name: string;
  description: string;
  dataSource: string;
  fields: string[];
  filters: ReportFilter[];
  groupBy: string[];
  orderBy: { field: string; direction: 'asc' | 'desc' }[];
  visualization: VisualizationType;
  refreshInterval?: number;
}

export interface ReportFilter {
  field: string;
  operator: FilterOperator;
  value: any;
  logicalOperator?: 'AND' | 'OR';
}

export interface DashboardWidget {
  id: string;
  title: string;
  reportId: string;
  visualization: VisualizationType;
  position: { x: number; y: number; width: number; height: number };
  refreshInterval: number;
  isVisible: boolean;
}

// Enums
export type ReportCategory = 'data-quality' | 'audit' | 'statistical' | 'compliance' | 'performance' | 'custom';
export type ReportType = 'tabular' | 'summary' | 'trend' | 'comparison' | 'drill-down';
export type VisualizationType = 'table' | 'bar-chart' | 'line-chart' | 'pie-chart' | 'donut-chart' | 'area-chart' | 'scatter-plot' | 'heatmap' | 'gauge' | 'card';
export type ReportStatus = 'pending' | 'running' | 'completed' | 'failed' | 'cancelled';
export type ExportFormat = 'pdf' | 'excel' | 'csv' | 'json' | 'xml';
export type FilterOperator = 'equals' | 'not-equals' | 'contains' | 'not-contains' | 'starts-with' | 'ends-with' | 'greater-than' | 'less-than' | 'between' | 'in' | 'not-in' | 'is-null' | 'is-not-null';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './reports.html',
  styleUrl: './reports.scss'
})
export class ReportsComponent implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('chartCanvas') chartCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('searchInput') searchInput!: ElementRef;

  private destroy$ = new Subject<void>();
  private chart: Chart | null = null;

  // Component State
  loading = false;
  error: string | null = null;
  activeView: 'templates' | 'builder' | 'scheduled' | 'dashboard' | 'executions' = 'templates';
  
  // Report Templates
  reportTemplates: ReportTemplate[] = [];
  filteredTemplates: ReportTemplate[] = [];
  selectedTemplate: ReportTemplate | null = null;
  
  // Custom Report Builder
  customReportForm: FormGroup;
  availableFields: ReportField[] = [];
  selectedFields: string[] = [];
  reportFilters: ReportFilter[] = [];
  
  // Report Execution
  currentExecution: ReportExecution | null = null;
  reportExecutions: ReportExecution[] = [];
  reportData: ReportData | null = null;
  
  // Scheduled Reports
  scheduledReports: ScheduledReport[] = [];
  scheduleForm: FormGroup;
  showScheduleModal = false;
  
  // Dashboard
  dashboardWidgets: DashboardWidget[] = [];
  isEditingDashboard = false;
  
  // Search and Filtering
  searchTerm = '';
  searchSubject = new Subject<string>();
  selectedCategory: ReportCategory | 'all' = 'all';
  selectedType: ReportType | 'all' = 'all';
  
  // Pagination
  currentPage = 0;
  pageSize = 12;
  totalElements = 0;
  totalPages = 0;
  
  // Export Options
  exportFormats: { value: ExportFormat; label: string; icon: string }[] = [
    { value: 'pdf', label: 'PDF', icon: 'file-pdf' },
    { value: 'excel', label: 'Excel', icon: 'file-excel' },
    { value: 'csv', label: 'CSV', icon: 'file-csv' },
    { value: 'json', label: 'JSON', icon: 'file-code' }
  ];

  // Report Categories
  reportCategories: { value: ReportCategory | 'all'; label: string; icon: string; count: number }[] = [
    { value: 'all', label: 'All Reports', icon: 'chart-bar', count: 0 },
    { value: 'data-quality', label: 'Data Quality', icon: 'shield-check', count: 0 },
    { value: 'audit', label: 'Audit Reports', icon: 'document-text', count: 0 },
    { value: 'statistical', label: 'Statistics', icon: 'chart-line', count: 0 },
    { value: 'compliance', label: 'Compliance', icon: 'exclamation-triangle', count: 0 },
    { value: 'performance', label: 'Performance', icon: 'clock', count: 0 },
    { value: 'custom', label: 'Custom', icon: 'cog', count: 0 }
  ];

  // Visualization Types
  visualizationTypes: { value: VisualizationType; label: string; icon: string }[] = [
    { value: 'table', label: 'Table', icon: 'table' },
    { value: 'bar-chart', label: 'Bar Chart', icon: 'chart-bar' },
    { value: 'line-chart', label: 'Line Chart', icon: 'chart-line' },
    { value: 'pie-chart', label: 'Pie Chart', icon: 'chart-pie' },
    { value: 'area-chart', label: 'Area Chart', icon: 'chart-area' },
    { value: 'card', label: 'Summary Card', icon: 'id-card' }
  ];

  constructor(
    private apiService: ApiService,
    private toastService: ToastService,
    private router: Router,
    private formBuilder: FormBuilder
  ) {
    this.customReportForm = this.formBuilder.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      description: ['', Validators.maxLength(500)],
      dataSource: ['', Validators.required],
      visualization: ['table', Validators.required],
      refreshInterval: [null, [Validators.min(1), Validators.max(1440)]]
    });

    this.scheduleForm = this.formBuilder.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      scheduleType: ['daily', Validators.required],
      time: ['09:00', Validators.required],
      dayOfWeek: [1],
      dayOfMonth: [1],
      timezone: ['UTC', Validators.required],
      recipients: ['', [Validators.required, Validators.email]],
      format: ['pdf', Validators.required],
      endDate: [null]
    });
  }

  ngOnInit(): void {
    this.initializeSearchSubscription();
    this.loadReportTemplates();
    this.loadScheduledReports();
    this.loadReportExecutions();
    this.loadAvailableFields();
    this.startExecutionPolling();
  }

  ngAfterViewInit(): void {
    // Initialize chart after view is ready
    if (this.reportData && this.chartCanvas) {
      this.createVisualization();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    
    if (this.chart) {
      this.chart.destroy();
    }
  }

  private initializeSearchSubscription(): void {
    this.searchSubject
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        takeUntil(this.destroy$)
      )
      .subscribe(term => {
        this.searchTerm = term;
        this.filterTemplates();
      });
  }

  private loadReportTemplates(): void {
    this.loading = true;
    
    // Mock data - replace with actual API call
    this.reportTemplates = this.getDefaultReportTemplates();
    this.filteredTemplates = [...this.reportTemplates];
    this.updateCategoryCounts();
    this.loading = false;

    /* Actual API implementation:
    this.apiService.getReportTemplates()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (templates) => {
          this.reportTemplates = templates;
          this.filteredTemplates = [...templates];
          this.updateCategoryCounts();
          this.loading = false;
        },
        error: (error) => {
          this.error = 'Failed to load report templates';
          this.toastService.showError('Failed to load report templates');
          this.loading = false;
        }
      });
    */
  }

  private loadScheduledReports(): void {
    // Mock data - replace with actual API call
    this.scheduledReports = [
      {
        id: '1',
        reportId: 'data-quality-summary',
        name: 'Daily Data Quality Report',
        schedule: { type: 'daily', time: '08:00', timezone: 'UTC' },
        recipients: ['admin@cbp.dhs.gov'],
        format: 'pdf',
        isActive: true,
        lastRun: new Date(Date.now() - 24 * 60 * 60 * 1000),
        nextRun: new Date(Date.now() + 24 * 60 * 60 * 1000),
        parameters: {},
        createdBy: 'system',
        createdAt: new Date()
      }
    ];
  }

  private loadReportExecutions(): void {
    // Mock data - replace with actual API call
    this.reportExecutions = [
      {
        id: '1',
        reportId: 'data-quality-summary',
        status: 'completed',
        progress: 100,
        startTime: new Date(Date.now() - 5 * 60 * 1000),
        endTime: new Date(Date.now() - 2 * 60 * 1000),
        parameters: { dateRange: '7d' },
        userId: 'current-user',
        downloadUrl: '/api/reports/executions/1/download'
      }
    ];
  }

  private loadAvailableFields(): void {
    // Mock data - replace with actual API call
    this.availableFields = [
      { id: 'country_code', name: 'countryCode', label: 'Country Code', type: 'string', required: false },
      { id: 'country_name', name: 'countryName', label: 'Country Name', type: 'string', required: false },
      { id: 'is_active', name: 'isActive', label: 'Active Status', type: 'boolean', required: false },
      { id: 'created_date', name: 'createdDate', label: 'Created Date', type: 'date', required: false },
      { id: 'record_count', name: 'recordCount', label: 'Record Count', type: 'number', required: false }
    ];
  }

  private startExecutionPolling(): void {
    interval(5000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        if (this.currentExecution && this.currentExecution.status === 'running') {
          this.checkExecutionStatus();
        }
      });
  }

  private checkExecutionStatus(): void {
    if (!this.currentExecution) return;

    // Mock progress update - replace with actual API call
    if (this.currentExecution.progress < 100) {
      this.currentExecution.progress += Math.random() * 20;
      if (this.currentExecution.progress >= 100) {
        this.currentExecution.progress = 100;
        this.currentExecution.status = 'completed';
        this.currentExecution.endTime = new Date();
        this.generateMockReportData();
      }
    }
  }

  private generateMockReportData(): void {
    // Mock report data - replace with actual data from API
    this.reportData = {
      headers: ['Country Code', 'Country Name', 'Active', 'Records', 'Quality Score'],
      rows: [
        ['US', 'United States', 'Yes', 1250, 98.5],
        ['CA', 'Canada', 'Yes', 890, 97.2],
        ['MX', 'Mexico', 'Yes', 650, 94.8],
        ['GB', 'United Kingdom', 'Yes', 420, 96.3],
        ['DE', 'Germany', 'Yes', 380, 95.7]
      ],
      totalRecords: 195,
      executionTime: 3.2,
      generatedAt: new Date(),
      metadata: {
        reportId: this.selectedTemplate?.id || 'custom',
        name: this.selectedTemplate?.name || 'Custom Report',
        description: this.selectedTemplate?.description || '',
        parameters: {},
        dataSource: 'reference-data'
      }
    };

    this.createVisualization();
  }

  private createVisualization(): void {
    if (!this.chartCanvas || !this.reportData) return;

    const ctx = this.chartCanvas.nativeElement.getContext('2d');
    if (!ctx) return;

    // Destroy existing chart
    if (this.chart) {
      this.chart.destroy();
    }

    // Create new chart based on visualization type
    const visualizationType = this.selectedTemplate?.visualizationType || 'bar-chart';
    
    switch (visualizationType) {
      case 'bar-chart':
        this.createBarChart(ctx);
        break;
      case 'line-chart':
        this.createLineChart(ctx);
        break;
      case 'pie-chart':
        this.createPieChart(ctx);
        break;
      default:
        this.createBarChart(ctx);
    }
  }

  private createBarChart(ctx: CanvasRenderingContext2D): void {
    if (!this.reportData) return;

    const data = {
      labels: this.reportData.rows.map(row => row[0]),
      datasets: [{
        label: 'Quality Score',
        data: this.reportData.rows.map(row => row[4]),
        backgroundColor: 'rgba(0, 51, 102, 0.8)',
        borderColor: 'rgba(0, 51, 102, 1)',
        borderWidth: 1
      }]
    };

    this.chart = new Chart(ctx, {
      type: 'bar',
      data,
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: true,
            position: 'top'
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            max: 100
          }
        }
      }
    });
  }

  private createLineChart(ctx: CanvasRenderingContext2D): void {
    if (!this.reportData) return;

    const data = {
      labels: this.reportData.rows.map(row => row[0]),
      datasets: [{
        label: 'Records',
        data: this.reportData.rows.map(row => row[3]),
        borderColor: 'rgba(0, 90, 156, 1)',
        backgroundColor: 'rgba(0, 90, 156, 0.1)',
        tension: 0.4,
        fill: true
      }]
    };

    this.chart = new Chart(ctx, {
      type: 'line',
      data,
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: true,
            position: 'top'
          }
        }
      }
    });
  }

  private createPieChart(ctx: CanvasRenderingContext2D): void {
    if (!this.reportData) return;

    const data = {
      labels: this.reportData.rows.map(row => row[0]),
      datasets: [{
        data: this.reportData.rows.map(row => row[3]),
        backgroundColor: [
          'rgba(0, 51, 102, 0.8)',
          'rgba(0, 90, 156, 0.8)',
          'rgba(46, 133, 64, 0.8)',
          'rgba(230, 126, 34, 0.8)',
          'rgba(231, 76, 60, 0.8)'
        ]
      }]
    };

    this.chart = new Chart(ctx, {
      type: 'pie',
      data,
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: true,
            position: 'right'
          }
        }
      }
    });
  }

  // Public Methods
  switchView(view: 'templates' | 'builder' | 'scheduled' | 'dashboard' | 'executions'): void {
    this.activeView = view;
    this.clearCurrentExecution();
  }

  selectTemplate(template: ReportTemplate): void {
    this.selectedTemplate = template;
    this.clearCurrentExecution();
  }

  executeReport(): void {
    if (!this.selectedTemplate) return;

    this.currentExecution = {
      id: Date.now().toString(),
      reportId: this.selectedTemplate.id,
      status: 'running',
      progress: 0,
      startTime: new Date(),
      parameters: {},
      userId: 'current-user'
    };

    this.toastService.showSuccess('Report execution started');
  }

  executeCustomReport(): void {
    if (!this.customReportForm.valid) {
      this.markFormGroupTouched(this.customReportForm);
      return;
    }

    const formValue = this.customReportForm.value;
    
    this.currentExecution = {
      id: Date.now().toString(),
      reportId: 'custom-' + Date.now(),
      status: 'running',
      progress: 0,
      startTime: new Date(),
      parameters: {
        ...formValue,
        fields: this.selectedFields,
        filters: this.reportFilters
      },
      userId: 'current-user'
    };

    this.toastService.showSuccess('Custom report execution started');
  }

  cancelExecution(): void {
    if (this.currentExecution) {
      this.currentExecution.status = 'cancelled';
      this.clearCurrentExecution();
      this.toastService.showInfo('Report execution cancelled');
    }
  }

  exportReport(format: ExportFormat): void {
    if (!this.reportData) {
      this.toastService.showError('No report data to export');
      return;
    }

    // Mock export - replace with actual implementation
    const blob = this.generateExportBlob(format);
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `report_${Date.now()}.${format}`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);

    this.toastService.showSuccess(`Report exported as ${format.toUpperCase()}`);
  }

  private generateExportBlob(format: ExportFormat): Blob {
    if (!this.reportData) return new Blob();

    switch (format) {
      case 'csv':
        return this.generateCSV();
      case 'json':
        return this.generateJSON();
      default:
        return this.generateCSV();
    }
  }

  private generateCSV(): Blob {
    if (!this.reportData) return new Blob();

    let csv = this.reportData.headers.join(',') + '\n';
    csv += this.reportData.rows.map(row => row.join(',')).join('\n');
    return new Blob([csv], { type: 'text/csv' });
  }

  private generateJSON(): Blob {
    if (!this.reportData) return new Blob();

    const data = {
      metadata: this.reportData.metadata,
      headers: this.reportData.headers,
      data: this.reportData.rows,
      summary: {
        totalRecords: this.reportData.totalRecords,
        executionTime: this.reportData.executionTime,
        generatedAt: this.reportData.generatedAt
      }
    };

    return new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
  }

  scheduleReport(): void {
    if (!this.scheduleForm.valid) {
      this.markFormGroupTouched(this.scheduleForm);
      return;
    }

    const formValue = this.scheduleForm.value;
    const scheduledReport: ScheduledReport = {
      id: Date.now().toString(),
      reportId: this.selectedTemplate?.id || 'custom',
      name: formValue.name,
      schedule: {
        type: formValue.scheduleType,
        time: formValue.time,
        timezone: formValue.timezone,
        dayOfWeek: formValue.dayOfWeek,
        dayOfMonth: formValue.dayOfMonth,
        endDate: formValue.endDate
      },
      recipients: formValue.recipients.split(',').map((email: string) => email.trim()),
      format: formValue.format,
      isActive: true,
      parameters: {},
      createdBy: 'current-user',
      createdAt: new Date()
    };

    this.scheduledReports.unshift(scheduledReport);
    this.showScheduleModal = false;
    this.scheduleForm.reset();
    this.toastService.showSuccess('Report scheduled successfully');
  }

  deleteScheduledReport(reportId: string): void {
    this.scheduledReports = this.scheduledReports.filter(r => r.id !== reportId);
    this.toastService.showSuccess('Scheduled report deleted');
  }

  toggleScheduledReport(reportId: string): void {
    const report = this.scheduledReports.find(r => r.id === reportId);
    if (report) {
      report.isActive = !report.isActive;
      const status = report.isActive ? 'enabled' : 'disabled';
      this.toastService.showSuccess(`Scheduled report ${status}`);
    }
  }

  addField(fieldId: string): void {
    if (!this.selectedFields.includes(fieldId)) {
      this.selectedFields.push(fieldId);
    }
  }

  removeField(fieldId: string): void {
    this.selectedFields = this.selectedFields.filter(id => id !== fieldId);
  }

  addFilter(): void {
    this.reportFilters.push({
      field: '',
      operator: 'equals',
      value: '',
      logicalOperator: 'AND'
    });
  }

  removeFilter(index: number): void {
    this.reportFilters.splice(index, 1);
  }

  onSearch(term: string): void {
    this.searchSubject.next(term);
  }

  onCategoryChange(category: ReportCategory | 'all'): void {
    this.selectedCategory = category;
    this.filterTemplates();
  }

  onTypeChange(type: ReportType | 'all'): void {
    this.selectedType = type;
    this.filterTemplates();
  }

  getFieldLabel(fieldId: string): string {
    const field = this.availableFields.find(f => f.id === fieldId);
    return field ? field.label : fieldId;
  }

  getVisualizationLabel(value: string | null): string {
    if (!value) return 'None';
    const viz = this.visualizationTypes.find(v => v.value === value);
    return viz ? viz.label : value;
  }

  filterTemplates(): void {
    this.filteredTemplates = this.reportTemplates.filter(template => {
      const matchesSearch = !this.searchTerm || 
        template.name.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        template.description.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        template.tags.some(tag => tag.toLowerCase().includes(this.searchTerm.toLowerCase()));

      const matchesCategory = this.selectedCategory === 'all' || template.category === this.selectedCategory;
      const matchesType = this.selectedType === 'all' || template.type === this.selectedType;

      return matchesSearch && matchesCategory && matchesType;
    });

    this.totalElements = this.filteredTemplates.length;
    this.totalPages = Math.ceil(this.totalElements / this.pageSize);
    this.currentPage = 0;
  }

  private updateCategoryCounts(): void {
    this.reportCategories.forEach(category => {
      if (category.value === 'all') {
        category.count = this.reportTemplates.length;
      } else {
        category.count = this.reportTemplates.filter(t => t.category === category.value).length;
      }
    });
  }

  clearCurrentExecution(): void {
    this.currentExecution = null;
    this.reportData = null;
    
    if (this.chart) {
      this.chart.destroy();
      this.chart = null;
    }
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  private getDefaultReportTemplates(): ReportTemplate[] {
    return [
      // Data Quality Reports
      {
        id: 'data-quality-summary',
        name: 'Data Quality Summary',
        description: 'Comprehensive overview of data quality metrics across all datasets',
        category: 'data-quality',
        type: 'summary',
        fields: [
          { id: 'completeness', name: 'completeness', label: 'Completeness %', type: 'number', required: true },
          { id: 'accuracy', name: 'accuracy', label: 'Accuracy %', type: 'number', required: true },
          { id: 'consistency', name: 'consistency', label: 'Consistency %', type: 'number', required: true }
        ],
        visualizationType: 'gauge',
        icon: 'shield-check',
        estimatedTime: '2-3 minutes',
        isDefault: true,
        permissions: ['ROLE_ADMIN', 'ROLE_DATA_STEWARD'],
        tags: ['quality', 'summary', 'dashboard']
      },
      {
        id: 'completeness-report',
        name: 'Data Completeness Report',
        description: 'Detailed analysis of missing or incomplete data across fields',
        category: 'data-quality',
        type: 'tabular',
        fields: [
          { id: 'table_name', name: 'tableName', label: 'Table Name', type: 'string', required: true },
          { id: 'field_name', name: 'fieldName', label: 'Field Name', type: 'string', required: true },
          { id: 'missing_count', name: 'missingCount', label: 'Missing Records', type: 'number', required: true }
        ],
        visualizationType: 'table',
        icon: 'exclamation-triangle',
        estimatedTime: '1-2 minutes',
        isDefault: true,
        permissions: ['ROLE_ADMIN', 'ROLE_DATA_STEWARD'],
        tags: ['quality', 'completeness', 'missing-data']
      },
      // Audit Reports
      {
        id: 'user-activity-audit',
        name: 'User Activity Audit',
        description: 'Track user actions, logins, and system access patterns',
        category: 'audit',
        type: 'tabular',
        fields: [
          { id: 'username', name: 'username', label: 'User', type: 'string', required: true },
          { id: 'action', name: 'action', label: 'Action', type: 'string', required: true },
          { id: 'timestamp', name: 'timestamp', label: 'Timestamp', type: 'date', required: true }
        ],
        visualizationType: 'table',
        icon: 'document-text',
        estimatedTime: '1-2 minutes',
        isDefault: true,
        permissions: ['ROLE_ADMIN', 'ROLE_AUDITOR'],
        tags: ['audit', 'security', 'access-control']
      },
      {
        id: 'change-requests-audit',
        name: 'Change Requests Audit',
        description: 'Complete audit trail of all change requests and approvals',
        category: 'audit',
        type: 'drill-down',
        fields: [
          { id: 'request_id', name: 'requestId', label: 'Request ID', type: 'string', required: true },
          { id: 'status', name: 'status', label: 'Status', type: 'enum', required: true, options: ['Pending', 'Approved', 'Rejected'] },
          { id: 'approver', name: 'approver', label: 'Approver', type: 'string', required: false }
        ],
        visualizationType: 'table',
        icon: 'clipboard-check',
        estimatedTime: '2-3 minutes',
        isDefault: true,
        permissions: ['ROLE_ADMIN', 'ROLE_AUDITOR', 'ROLE_DATA_STEWARD'],
        tags: ['audit', 'change-management', 'approval-workflow']
      },
      // Statistical Reports
      {
        id: 'entity-statistics',
        name: 'Entity Statistics',
        description: 'Count and trend analysis of countries, ports, airports, and carriers',
        category: 'statistical',
        type: 'trend',
        fields: [
          { id: 'entity_type', name: 'entityType', label: 'Entity Type', type: 'enum', required: true, options: ['Countries', 'Ports', 'Airports', 'Carriers'] },
          { id: 'total_count', name: 'totalCount', label: 'Total Count', type: 'number', required: true },
          { id: 'active_count', name: 'activeCount', label: 'Active Count', type: 'number', required: true }
        ],
        visualizationType: 'bar-chart',
        icon: 'chart-bar',
        estimatedTime: '1 minute',
        isDefault: true,
        permissions: ['ROLE_USER', 'ROLE_ADMIN', 'ROLE_DATA_STEWARD'],
        tags: ['statistics', 'counts', 'trends']
      },
      {
        id: 'growth-trends',
        name: 'Data Growth Trends',
        description: 'Historical growth patterns and projections for reference data',
        category: 'statistical',
        type: 'trend',
        fields: [
          { id: 'date_range', name: 'dateRange', label: 'Date Range', type: 'string', required: true },
          { id: 'growth_rate', name: 'growthRate', label: 'Growth Rate %', type: 'number', required: true },
          { id: 'projection', name: 'projection', label: 'Projected Count', type: 'number', required: false }
        ],
        visualizationType: 'line-chart',
        icon: 'chart-line',
        estimatedTime: '2-3 minutes',
        isDefault: true,
        permissions: ['ROLE_ADMIN', 'ROLE_DATA_STEWARD', 'ROLE_ANALYST'],
        tags: ['trends', 'growth', 'forecasting']
      },
      // Compliance Reports
      {
        id: 'validation-errors',
        name: 'Validation Errors Report',
        description: 'Summary of validation failures and data integrity issues',
        category: 'compliance',
        type: 'tabular',
        fields: [
          { id: 'error_type', name: 'errorType', label: 'Error Type', type: 'string', required: true },
          { id: 'record_id', name: 'recordId', label: 'Record ID', type: 'string', required: true },
          { id: 'error_message', name: 'errorMessage', label: 'Error Message', type: 'string', required: true }
        ],
        visualizationType: 'table',
        icon: 'exclamation-triangle',
        estimatedTime: '2-4 minutes',
        isDefault: true,
        permissions: ['ROLE_ADMIN', 'ROLE_DATA_STEWARD'],
        tags: ['compliance', 'validation', 'errors']
      },
      {
        id: 'policy-violations',
        name: 'Policy Violations Report',
        description: 'Track violations of data governance policies and business rules',
        category: 'compliance',
        type: 'summary',
        fields: [
          { id: 'policy_name', name: 'policyName', label: 'Policy Name', type: 'string', required: true },
          { id: 'violation_count', name: 'violationCount', label: 'Violation Count', type: 'number', required: true },
          { id: 'severity', name: 'severity', label: 'Severity', type: 'enum', required: true, options: ['High', 'Medium', 'Low'] }
        ],
        visualizationType: 'pie-chart',
        icon: 'shield-exclamation',
        estimatedTime: '1-2 minutes',
        isDefault: true,
        permissions: ['ROLE_ADMIN', 'ROLE_COMPLIANCE_OFFICER'],
        tags: ['compliance', 'policy', 'violations']
      },
      // Performance Reports
      {
        id: 'api-usage-stats',
        name: 'API Usage Statistics',
        description: 'Monitor API endpoint usage, response times, and error rates',
        category: 'performance',
        type: 'trend',
        fields: [
          { id: 'endpoint', name: 'endpoint', label: 'Endpoint', type: 'string', required: true },
          { id: 'request_count', name: 'requestCount', label: 'Request Count', type: 'number', required: true },
          { id: 'avg_response_time', name: 'avgResponseTime', label: 'Avg Response Time (ms)', type: 'number', required: true }
        ],
        visualizationType: 'line-chart',
        icon: 'clock',
        estimatedTime: '2-3 minutes',
        isDefault: true,
        permissions: ['ROLE_ADMIN', 'ROLE_SYSTEM_ANALYST'],
        tags: ['performance', 'api', 'monitoring']
      },
      {
        id: 'system-performance',
        name: 'System Performance Overview',
        description: 'Overall system health metrics including database and cache performance',
        category: 'performance',
        type: 'summary',
        fields: [
          { id: 'component', name: 'component', label: 'Component', type: 'string', required: true },
          { id: 'status', name: 'status', label: 'Status', type: 'enum', required: true, options: ['Healthy', 'Warning', 'Critical'] },
          { id: 'response_time', name: 'responseTime', label: 'Response Time (ms)', type: 'number', required: true }
        ],
        visualizationType: 'card',
        icon: 'server',
        estimatedTime: '1 minute',
        isDefault: true,
        permissions: ['ROLE_ADMIN', 'ROLE_SYSTEM_ANALYST'],
        tags: ['performance', 'health', 'monitoring']
      }
    ];
  }

  // Pagination
  changePage(direction: 'prev' | 'next'): void {
    if (direction === 'prev' && this.currentPage > 0) {
      this.currentPage--;
    } else if (direction === 'next' && this.currentPage < this.totalPages - 1) {
      this.currentPage++;
    }
  }

  get paginatedTemplates(): ReportTemplate[] {
    const start = this.currentPage * this.pageSize;
    const end = start + this.pageSize;
    return this.filteredTemplates.slice(start, end);
  }

  // Utility Methods
  formatDate(date: Date | string): string {
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getStatusBadgeClass(status: ReportStatus): string {
    switch (status) {
      case 'completed': return 'usa-tag usa-tag--success';
      case 'running': return 'usa-tag usa-tag--info';
      case 'failed': return 'usa-tag usa-tag--error';
      case 'cancelled': return 'usa-tag usa-tag--warning';
      default: return 'usa-tag';
    }
  }

  getCategoryIcon(category: ReportCategory): string {
    const categoryData = this.reportCategories.find(c => c.value === category);
    return categoryData?.icon || 'document';
  }

  getVisualizationIcon(type: VisualizationType): string {
    const vizData = this.visualizationTypes.find(v => v.value === type);
    return vizData?.icon || 'chart-bar';
  }
}