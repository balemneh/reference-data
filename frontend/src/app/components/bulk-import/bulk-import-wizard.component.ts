import { Component, OnInit, ViewChild, ElementRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, takeUntil, finalize } from 'rxjs';

import { ApiService } from '../../services/api.service';
import { ToastService } from '../../services/toast.service';

export type WizardStep = 'upload' | 'validate' | 'review' | 'submit';
export type FileFormat = 'CSV' | 'JSON' | 'XLSX';
export type DataType = 'COUNTRIES' | 'PORTS' | 'AIRPORTS';
export type ValidationSeverity = 'ERROR' | 'WARNING' | 'INFO';

export interface BulkImportFile {
  file: File;
  format: FileFormat;
  size: number;
  lastModified: number;
}

export interface ValidationError {
  row: number;
  column: string;
  field: string;
  value: any;
  message: string;
  severity: ValidationSeverity;
  suggestion?: string;
}

export interface ValidationResult {
  batchId: string;
  totalRecords: number;
  validRecords: number;
  invalidRecords: number;
  warningCount: number;
  errors: ValidationError[];
  warnings: ValidationError[];
  previewData: any[];
  isValid: boolean;
}

export interface ProcessingProgress {
  batchId: string;
  status: 'UPLOADING' | 'VALIDATING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  progress: number;
  processedCount: number;
  failedCount: number;
  estimatedTimeRemaining?: number;
  currentStep?: string;
}

export interface BusinessJustification {
  reason: string;
  impactDescription: string;
  approverName: string;
  urgency: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  scheduledDate?: string;
  rollbackPlan?: string;
}

@Component({
  selector: 'app-bulk-import-wizard',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './bulk-import-wizard.component.html',
  styleUrl: './bulk-import-wizard.component.scss'
})
export class BulkImportWizardComponent implements OnInit, OnDestroy {
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;
  @ViewChild('dropZone') dropZone!: ElementRef<HTMLDivElement>;

  // Wizard state
  currentStep: WizardStep = 'upload';
  isProcessing = false;

  // Step completion tracking
  completedSteps = new Set<WizardStep>();

  // File upload
  selectedFile: BulkImportFile | null = null;
  dataType: DataType = 'COUNTRIES';
  isDragOver = false;
  isUploading = false;
  uploadProgress = 0;

  // Validation
  validationResult: ValidationResult | null = null;
  isValidating = false;

  // Data preview
  previewPage = 0;
  previewSize = 10;
  previewData: any[] = [];
  previewColumns: string[] = [];
  filteredErrors: ValidationError[] = [];
  errorFilter: ValidationSeverity | 'ALL' = 'ALL';

  // Processing
  processingProgress: ProcessingProgress | null = null;
  batchId: string | null = null;

  // Business justification form
  justificationForm: FormGroup;

  // Error handling
  error: string | null = null;

  // Component cleanup
  private destroy$ = new Subject<void>();

  // Supported file formats
  readonly SUPPORTED_FORMATS: Record<FileFormat, string[]> = {
    CSV: ['.csv', 'text/csv'],
    JSON: ['.json', 'application/json'],
    XLSX: ['.xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet']
  };

  // Data type configurations
  readonly DATA_TYPE_CONFIG = {
    COUNTRIES: {
      label: 'Countries',
      description: 'ISO country codes and reference data',
      requiredFields: ['countryCode', 'countryName'],
      optionalFields: ['iso2Code', 'iso3Code', 'numericCode', 'isActive']
    },
    PORTS: {
      label: 'Ports',
      description: 'Seaports and maritime facilities',
      requiredFields: ['portCode', 'portName', 'countryCode'],
      optionalFields: ['city', 'portType', 'latitude', 'longitude', 'isActive']
    },
    AIRPORTS: {
      label: 'Airports',
      description: 'Commercial and cargo airports',
      requiredFields: ['iataCode', 'airportName', 'countryCode'],
      optionalFields: ['icaoCode', 'city', 'airportType', 'hasCustoms', 'isActive']
    }
  };

  constructor(
    private apiService: ApiService,
    private toastService: ToastService,
    private router: Router,
    private formBuilder: FormBuilder
  ) {
    this.justificationForm = this.createJustificationForm();
  }

  ngOnInit(): void {
    this.initializeComponent();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private createJustificationForm(): FormGroup {
    return this.formBuilder.group({
      reason: ['', [Validators.required, Validators.minLength(10)]],
      impactDescription: ['', [Validators.required, Validators.minLength(20)]],
      approverName: ['', Validators.required],
      urgency: ['MEDIUM', Validators.required],
      scheduledDate: [''],
      rollbackPlan: ['']
    });
  }

  private initializeComponent(): void {
    this.resetWizard();
  }

  // ==================== WIZARD NAVIGATION ====================

  get wizardSteps(): Array<{step: WizardStep, label: string, description: string}> {
    return [
      { step: 'upload', label: 'Upload File', description: 'Select and upload data file' },
      { step: 'validate', label: 'Validate Data', description: 'Check data quality and format' },
      { step: 'review', label: 'Review Results', description: 'Preview data and validation results' },
      { step: 'submit', label: 'Submit Import', description: 'Provide justification and execute import' }
    ];
  }

  get currentStepIndex(): number {
    return this.wizardSteps.findIndex(s => s.step === this.currentStep);
  }

  get canProceedToNext(): boolean {
    switch (this.currentStep) {
      case 'upload':
        return !!this.selectedFile && !this.isUploading;
      case 'validate':
        return !!this.validationResult && !this.isValidating;
      case 'review':
        return !!this.validationResult?.isValid;
      case 'submit':
        return this.justificationForm.valid && !this.isProcessing;
      default:
        return false;
    }
  }

  get canGoBack(): boolean {
    return this.currentStepIndex > 0 && !this.isProcessing && !this.isValidating && !this.isUploading;
  }

  nextStep(): void {
    if (!this.canProceedToNext) return;

    const nextIndex = this.currentStepIndex + 1;
    if (nextIndex < this.wizardSteps.length) {
      this.completedSteps.add(this.currentStep);
      this.currentStep = this.wizardSteps[nextIndex].step;

      // Auto-trigger step-specific actions
      switch (this.currentStep) {
        case 'validate':
          this.initiateValidation();
          break;
        case 'review':
          this.prepareReviewStep();
          break;
        case 'submit':
          this.prepareSubmissionStep();
          break;
      }
    }
  }

  previousStep(): void {
    if (!this.canGoBack) return;

    const prevIndex = this.currentStepIndex - 1;
    if (prevIndex >= 0) {
      this.currentStep = this.wizardSteps[prevIndex].step;
    }
  }

  goToStep(step: WizardStep): void {
    if (this.completedSteps.has(step) || step === 'upload') {
      this.currentStep = step;
    }
  }

  isStepCompleted(step: WizardStep): boolean {
    return this.completedSteps.has(step);
  }

  isStepActive(step: WizardStep): boolean {
    return this.currentStep === step;
  }

  isStepAccessible(step: WizardStep): boolean {
    return this.isStepCompleted(step) || this.isStepActive(step) || step === 'upload';
  }

  // ==================== FILE UPLOAD ====================

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleFileSelection(input.files[0]);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;

    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.handleFileSelection(files[0]);
    }
  }

  private handleFileSelection(file: File): void {
    this.error = null;

    // Validate file size (max 50MB)
    const maxSize = 50 * 1024 * 1024;
    if (file.size > maxSize) {
      this.showError('File size exceeds 50MB limit. Please select a smaller file.');
      return;
    }

    // Validate file format
    const format = this.detectFileFormat(file);
    if (!format) {
      this.showError('Unsupported file format. Please select a CSV, JSON, or XLSX file.');
      return;
    }

    this.selectedFile = {
      file,
      format,
      size: file.size,
      lastModified: file.lastModified
    };

    this.resetValidationResults();
    this.toastService.showSuccess('File Selected', `${file.name} is ready for validation`);
  }

  private detectFileFormat(file: File): FileFormat | null {
    const fileName = file.name.toLowerCase();
    const mimeType = file.type;

    for (const [format, identifiers] of Object.entries(this.SUPPORTED_FORMATS)) {
      if (identifiers.some(id =>
        fileName.endsWith(id) || mimeType === id
      )) {
        return format as FileFormat;
      }
    }

    return null;
  }

  removeSelectedFile(): void {
    this.selectedFile = null;
    this.resetValidationResults();
    this.error = null;

    if (this.fileInput) {
      this.fileInput.nativeElement.value = '';
    }
  }

  // ==================== VALIDATION ====================

  private initiateValidation(): void {
    if (!this.selectedFile || this.isValidating) return;

    this.isValidating = true;
    this.error = null;

    // First, upload the file and get batch ID
    this.uploadFile()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.isValidating = false)
      )
      .subscribe({
        next: (response) => {
          this.batchId = response.batchId;
          this.validateBatchData(response.batchId);
        },
        error: (error) => {
          this.showError('Failed to upload file for validation');
          console.error('Upload error:', error);
        }
      });
  }

  private uploadFile() {
    if (!this.selectedFile) throw new Error('No file selected');

    this.isUploading = true;
    const formData = new FormData();
    formData.append('file', this.selectedFile.file);
    formData.append('userId', 'current-user'); // In real app, get from auth service
    formData.append('dataType', this.dataType);
    formData.append('sourceSystem', 'BULK_IMPORT_WIZARD');
    formData.append('description', `Bulk import of ${this.dataType} data via wizard`);

    return this.apiService.initiateBulkImport(formData);
  }

  private validateBatchData(batchId: string): void {
    this.apiService.validateBulkImport(batchId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => {
          this.validationResult = this.processValidationResult(result);
          this.preparePreviewData();
        },
        error: (error) => {
          this.showError('Validation failed. Please check your file format and try again.');
          console.error('Validation error:', error);
        }
      });
  }

  private processValidationResult(apiResult: any): ValidationResult {
    // Transform API response to our internal format
    return {
      batchId: apiResult.batchId,
      totalRecords: apiResult.validCount + apiResult.invalidCount,
      validRecords: apiResult.validCount,
      invalidRecords: apiResult.invalidCount,
      warningCount: apiResult.warningCount || 0,
      errors: apiResult.errors || [],
      warnings: apiResult.warnings || [],
      previewData: apiResult.previewData || [],
      isValid: apiResult.validCount > 0 && apiResult.success
    };
  }

  retryValidation(): void {
    if (this.batchId) {
      this.validateBatchData(this.batchId);
    }
  }

  // ==================== PREVIEW AND REVIEW ====================

  private prepareReviewStep(): void {
    if (this.validationResult) {
      this.preparePreviewData();
      this.updateErrorFilter();
    }
  }

  private preparePreviewData(): void {
    if (!this.validationResult) return;

    this.previewData = this.validationResult.previewData || [];

    // Extract column names from preview data
    if (this.previewData.length > 0) {
      this.previewColumns = Object.keys(this.previewData[0]).filter(key => !key.startsWith('_'));
    }
  }

  get paginatedPreviewData(): any[] {
    const start = this.previewPage * this.previewSize;
    const end = start + this.previewSize;
    return this.previewData.slice(start, end);
  }

  get previewTotalPages(): number {
    return Math.ceil(this.previewData.length / this.previewSize);
  }

  nextPreviewPage(): void {
    if (this.previewPage < this.previewTotalPages - 1) {
      this.previewPage++;
    }
  }

  previousPreviewPage(): void {
    if (this.previewPage > 0) {
      this.previewPage--;
    }
  }

  updateErrorFilter(): void {
    if (!this.validationResult) return;

    if (this.errorFilter === 'ALL') {
      this.filteredErrors = [...this.validationResult.errors, ...this.validationResult.warnings];
    } else {
      this.filteredErrors = [
        ...this.validationResult.errors.filter(e => e.severity === this.errorFilter),
        ...this.validationResult.warnings.filter(w => w.severity === this.errorFilter)
      ];
    }
  }

  getRowValidationStatus(rowIndex: number): 'valid' | 'warning' | 'error' {
    if (!this.validationResult) return 'valid';

    const hasError = this.validationResult.errors.some(e => e.row === rowIndex + 1);
    if (hasError) return 'error';

    const hasWarning = this.validationResult.warnings.some(w => w.row === rowIndex + 1);
    if (hasWarning) return 'warning';

    return 'valid';
  }

  getCellValidationMessages(rowIndex: number, column: string): ValidationError[] {
    if (!this.validationResult) return [];

    return [
      ...this.validationResult.errors.filter(e => e.row === rowIndex + 1 && e.column === column),
      ...this.validationResult.warnings.filter(w => w.row === rowIndex + 1 && w.column === column)
    ];
  }

  downloadValidationReport(): void {
    if (!this.validationResult) return;

    const reportData = this.generateValidationReportCSV();
    const blob = new Blob([reportData], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);

    link.setAttribute('href', url);
    link.setAttribute('download', `validation-report-${this.dataType}-${new Date().toISOString().split('T')[0]}.csv`);
    link.style.visibility = 'hidden';

    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    this.toastService.showSuccess('Report Downloaded', 'Validation report has been downloaded');
  }

  private generateValidationReportCSV(): string {
    if (!this.validationResult) return '';

    const headers = 'Row,Column,Field,Value,Severity,Message,Suggestion\n';
    const errors = [...this.validationResult.errors, ...this.validationResult.warnings];

    const rows = errors.map(error => {
      return [
        error.row,
        error.column,
        error.field,
        `"${String(error.value).replace(/"/g, '""')}"`,
        error.severity,
        `"${error.message.replace(/"/g, '""')}"`,
        `"${(error.suggestion || '').replace(/"/g, '""')}"`
      ].join(',');
    }).join('\n');

    return headers + rows;
  }

  // ==================== SUBMISSION ====================

  private prepareSubmissionStep(): void {
    // Pre-populate form if needed
    if (!this.justificationForm.get('reason')?.value) {
      this.justificationForm.patchValue({
        reason: `Bulk import of ${this.validationResult?.validRecords || 0} ${this.dataType.toLowerCase()} records`
      });
    }
  }

  submitImport(): void {
    if (!this.justificationForm.valid || !this.batchId || this.isProcessing) return;

    this.isProcessing = true;
    this.error = null;

    const justification = this.justificationForm.value as BusinessJustification;

    this.apiService.processBulkImport(this.batchId, justification)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.isProcessing = false)
      )
      .subscribe({
        next: (result) => {
          this.processingProgress = {
            batchId: this.batchId!,
            status: 'COMPLETED',
            progress: 100,
            processedCount: result.processedCount,
            failedCount: result.failedCount
          };

          this.toastService.showSuccess('Import Completed',
            `Successfully processed ${result.processedCount} records`);

          // Navigate back to import-export page after a delay
          setTimeout(() => {
            this.router.navigate(['/import-export']);
          }, 3000);
        },
        error: (error) => {
          this.showError('Import processing failed. Please try again or contact support.');
          console.error('Processing error:', error);
        }
      });
  }

  // ==================== UTILITY METHODS ====================

  private resetWizard(): void {
    this.currentStep = 'upload';
    this.completedSteps.clear();
    this.selectedFile = null;
    this.validationResult = null;
    this.processingProgress = null;
    this.batchId = null;
    this.error = null;
    this.isProcessing = false;
    this.isValidating = false;
    this.isUploading = false;
    this.previewPage = 0;
    this.errorFilter = 'ALL';
    this.justificationForm.reset();
  }

  private resetValidationResults(): void {
    this.validationResult = null;
    this.processingProgress = null;
    this.batchId = null;
    this.previewData = [];
    this.previewColumns = [];
    this.filteredErrors = [];
    this.completedSteps.delete('validate');
    this.completedSteps.delete('review');
    this.completedSteps.delete('submit');
  }

  private showError(message: string): void {
    this.error = message;
    this.toastService.showError('Error', message);
  }

  formatFileSize(bytes: number): string {
    const units = ['B', 'KB', 'MB', 'GB'];
    let size = bytes;
    let unitIndex = 0;

    while (size >= 1024 && unitIndex < units.length - 1) {
      size /= 1024;
      unitIndex++;
    }

    return `${size.toFixed(unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`;
  }

  formatDate(timestamp: number): string {
    return new Date(timestamp).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getDataTypeIcon(dataType: DataType): string {
    const icons = {
      COUNTRIES: 'public',
      PORTS: 'anchor',
      AIRPORTS: 'flight'
    };
    return icons[dataType] || 'upload_file';
  }

  getSeverityIcon(severity: ValidationSeverity): string {
    const icons = {
      ERROR: 'error',
      WARNING: 'warning',
      INFO: 'info'
    };
    return icons[severity] || 'help';
  }

  getSeverityClass(severity: ValidationSeverity): string {
    const classes = {
      ERROR: 'text-error',
      WARNING: 'text-warning',
      INFO: 'text-info'
    };
    return classes[severity] || '';
  }

  cancelWizard(): void {
    if (this.isProcessing || this.isValidating) {
      const canCancel = confirm('Import is in progress. Are you sure you want to cancel?');
      if (!canCancel) return;
    }

    this.router.navigate(['/import-export']);
  }

  startNewImport(): void {
    this.resetWizard();
  }
}