import { Component, OnInit, ViewChild, ElementRef, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, CountryDto, PortDto, AirportDto, ScheduledExport } from '../../services/api.service';
import { ToastService } from '../../services/toast.service';
import { debounceTime, distinctUntilChanged, Subject, Observable, of, Subscription, interval } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ChangeRequestService } from '../../services/change-request.service';
import { OperationHistoryService } from '../../services/operation-history.service';
import { ScheduleModalComponent } from '../schedule-modal/schedule-modal.component';

export interface ImportExportHistory {
  id: string;
  operationType: 'IMPORT' | 'EXPORT';
  entityType: 'COUNTRIES' | 'PORTS' | 'AIRPORTS' | 'ALL';
  fileName: string;
  fileSize?: number;
  format: 'CSV' | 'JSON' | 'XML' | 'EXCEL' | 'XLSX';
  recordCount?: number;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'PARTIAL';
  startTime: string;
  completedTime?: string;
  successCount?: number;
  errorCount?: number;
  warnings?: number;
  errorDetails?: string;
  downloadUrl?: string;
  createdBy: string;
  progress?: number;
  changeRequestId?: string; // Add this line
  batchId?: string;
}

export interface ImportValidation {
  isValid: boolean;
  errors: ValidationError[];
  warnings: ValidationError[];
  totalRecords: number;
  validRecords: number;
  duplicateRecords: number;
  skippedRecords: number;
}

export interface ValidationError {
  row: number;
  column: string;
  value: any;
  error: string;
  severity: 'ERROR' | 'WARNING';
}

export interface ColumnMapping {
  sourceColumn: string;
  targetField: string;
  required: boolean;
  dataType: 'string' | 'number' | 'boolean' | 'date';
  format?: string;
  example?: string;
}

export interface ImportTemplate {
  entityType: 'COUNTRIES' | 'PORTS' | 'AIRPORTS';
  name: string;
  description: string;
  requiredColumns: string[];
  optionalColumns: string[];
  sampleData: any[];
  validationRules: string[];
}

@Component({
  selector: 'app-import-export',
  standalone: true,
  imports: [CommonModule, FormsModule, ScheduleModalComponent],
  templateUrl: './import-export.html',
  styleUrl: './import-export.scss'
})
export class ImportExportComponent implements OnInit, OnDestroy {
  @ViewChild('fileInput') fileInput!: ElementRef;
  @ViewChild('dropZone') dropZone!: ElementRef;
  
  // Main navigation
  activeTab: 'import' | 'export' | 'history' | 'scheduled' = 'import';

  setActiveTab(tab: 'import' | 'export' | 'history' | 'scheduled') {
    this.activeTab = tab;
    this.cdr.detectChanges();
  }
  
  
  // Import functionality
  selectedFile: File | null = null;
  importFormat: 'CSV' | 'JSON' | 'XML' = 'CSV';
  entityType: 'COUNTRIES' | 'PORTS' | 'AIRPORTS' = 'COUNTRIES';
  dragOver = false;
  uploadProgress = 0;
  isUploading = false;
  
  // Import validation and preview
    previewButtonText = 'Preview';
  showPreview = false;
  previewData: any[] = [];
  validationResults: ImportValidation | null = null;
  columnMappings: ColumnMapping[] = [];
  showMappingModal = false;
  skipFirstRow = true;
  
  // Preview pagination
  currentPage = 1;
  pageSize = 5;
  
  // Export functionality
  exportFormat: 'CSV' | 'JSON' | 'XML' | 'EXCEL' = 'CSV';
  exportEntityType: 'COUNTRIES' | 'PORTS' | 'AIRPORTS' | 'ALL' = 'COUNTRIES';
  exportFilters: any = {};
  includeInactive = false;
  dateRangeFrom = '';
  dateRangeTo = '';
  exportProgress = 0;
  isExporting = false;
  
  // Templates
  importTemplates: ImportTemplate[] = [];
  selectedTemplate: ImportTemplate | null = null;
  
  // History
  operationHistory: ImportExportHistory[] = [];
  private historySubscription!: Subscription;
  historyLoading = false;
  historyPage = 0;
  historySize = 20;
  historyTotal = 0;
  historyFilter = '';
  historyFilterSubject = new Subject<string>();
  
  // Scheduled exports
  scheduledExports: ScheduledExport[] = [];
  showScheduleModal = false;
  selectedSchedule: ScheduledExport | null = null;

  
  // General
  loading = false;
  error: string | null = null;
  scheduleForm: any = {};

  
  constructor(
    private apiService: ApiService,
    private toastService: ToastService,
    private changeRequestService: ChangeRequestService,
    private operationHistoryService: OperationHistoryService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadImportTemplates();
    this.historySubscription = this.operationHistoryService.history$.subscribe(history => {
      this.operationHistory = history;
    });
    this.loadScheduledExports();
    this.initializeHistoryFilterSubscription();
  }

  ngOnDestroy() {
    if (this.historySubscription) {
      this.historySubscription.unsubscribe();
    }
  }

  getEnabledScheduledExportsCount(): number {
    return this.scheduledExports.filter(s => s.enabled).length;
  }
  
  private initializeHistoryFilterSubscription() {
    this.historyFilterSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(filter => {
      this.historyFilter = filter;
      this.historyPage = 0;
      this.operationHistory = this.operationHistoryService.getHistory();
    });
  }

  private addToHistory(operationType: 'IMPORT' | 'EXPORT', status: ImportExportHistory['status'], changeRequestId?: string, batchId?: string, recordCount?: number, fileSize?: number): string {
    const newOperation: ImportExportHistory = {
      id: `op_${Date.now()}`,
      operationType: operationType,
      entityType: this.entityType,
      fileName: this.selectedFile?.name || `${this.exportEntityType.toLowerCase()}_export.${this.exportFormat.toLowerCase()}`,
      fileSize: fileSize || this.selectedFile?.size || 0,
      format: (operationType === 'IMPORT' ? this.importFormat : this.exportFormat) as 'CSV' | 'JSON' | 'XML' | 'EXCEL' | 'XLSX',
      recordCount: recordCount || 0,
      status: status,
      startTime: '',
      createdBy: 'current-user',
      progress: 0,
      changeRequestId: changeRequestId,
      batchId: batchId
    };
    
    this.operationHistoryService.addOperation(newOperation);
    return newOperation.id;
  }



  // ==================== FILE HANDLING ====================
  
  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleFileSelection(input.files[0]);
    }
  }
  
  onDragOver(event: DragEvent) {
    event.preventDefault();
    this.dragOver = true;
  }
  
  onDragLeave(event: DragEvent) {
    event.preventDefault();
    this.dragOver = false;
  }
  
  onDrop(event: DragEvent) {
    event.preventDefault();
    this.dragOver = false;
    
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.handleFileSelection(files[0]);
    }
  }
  
  private handleFileSelection(file: File) {
    // Validate file type
    const allowedTypes = ['text/csv', 'application/json', 'text/xml', 'application/xml', 
                         'application/vnd.ms-excel', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'];
    
    if (!allowedTypes.some(type => file.type === type || file.name.toLowerCase().endsWith(this.getFileExtension(type)))) {
      this.toastService.showError('Invalid file type', 'Please select a valid file format (CSV, JSON, XML, or Excel)');
      return;
    }
    
    // Validate file size (max 50MB)
    const maxSize = 50 * 1024 * 1024;
    if (file.size > maxSize) {
      this.toastService.showError('File too large', 'File size must be less than 50MB');
      return;
    }
    
    this.selectedFile = file;
    
    // Auto-detect format from file extension
    const extension = file.name.split('.').pop()?.toLowerCase();
    if (extension === 'csv') this.importFormat = 'CSV';
    else if (extension === 'json') this.importFormat = 'JSON';
    else if (extension === 'xml') this.importFormat = 'XML';
    
    this.toastService.showSuccess('File selected', `${file.name} ready for import`);
  }
  
  private getFileExtension(mimeType: string): string {
    const extensions: { [key: string]: string } = {
      'text/csv': '.csv',
      'application/json': '.json',
      'text/xml': '.xml',
      'application/xml': '.xml',
      'application/vnd.ms-excel': '.xls',
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': '.xlsx'
    };
    return extensions[mimeType] || '';
  }
  
  clearSelectedFile() {
    this.selectedFile = null;
    this.showPreview = false;
    this.previewData = [];
    this.validationResults = null;
    this.columnMappings = [];
    
    if (this.fileInput) {
      this.fileInput.nativeElement.value = '';
    }
  }

  // ==================== IMPORT FUNCTIONALITY ====================
  
  previewImport() {
    if (this.showPreview) {
      this.showPreview = false;
      this.previewButtonText = 'Preview';
      return;
    }
    if (!this.selectedFile) {
      this.toastService.showError('Selection Error', 'Please select a file first');
      return;
    }
    
    this.loading = true;
    
    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const content = e.target?.result as string;
        this.parseFileContent(content);
        this.generateColumnMappings();
        this.showPreview = true;
        this.previewButtonText = 'Hide Preview';
      } catch (error) {
        this.toastService.showError('Parse error', 'Failed to parse file content');
      } finally {
        this.loading = false;
      }
    };
    
    reader.onerror = () => {
      this.toastService.showError('File Read Error', 'Failed to read file');
      this.loading = false;
    };
    
    reader.readAsText(this.selectedFile);
  }
  
  get paginatedPreviewData(): any[] {
    const startIndex = (this.currentPage - 1) * this.pageSize;
    return this.previewData.slice(startIndex, startIndex + this.pageSize);
  }

  previousPage() {
    if (this.hasPreviousPage()) {
      this.currentPage--;
    }
  }

  nextPage() {
    if (this.hasNextPage()) {
      this.currentPage++;
    }
  }

  hasPreviousPage(): boolean {
    return this.currentPage > 1;
  }

  hasNextPage(): boolean {
    return (this.currentPage * this.pageSize) < this.previewData.length;
  }
  
  private parseFileContent(content: string) {
    switch (this.importFormat) {
      case 'CSV':
        this.parseCSV(content);
        break;
      case 'JSON':
        this.parseJSON(content);
        break;
      case 'XML':
        this.parseXML(content);
        break;
    }
  }
  
  private parseCSV(content: string) {
    const lines = content.split('\n').filter(line => line.trim());
    if (lines.length === 0) throw new Error('File is empty');
    
    const headers = lines[0].split(',').map(h => h.trim().replace(/"/g, ''));
    const dataStartIndex = this.skipFirstRow ? 1 : 0;
    
    this.previewData = lines.slice(dataStartIndex, dataStartIndex + 10).map((line, index) => {
      const values = line.split(',').map(v => v.trim().replace(/"/g, ''));
      const row: any = { _rowIndex: dataStartIndex + index };
      
      headers.forEach((header, i) => {
        row[header] = values[i] || '';
      });
      
      return row;
    });
  }
  
  private parseJSON(content: string) {
    const data = JSON.parse(content);
    const array = Array.isArray(data) ? data : [data];
    this.previewData = array.slice(0, 10).map((item, index) => ({
      ...item,
      _rowIndex: index
    }));
  }
  
  private parseXML(content: string) {
    const parser = new DOMParser();
    const xmlDoc = parser.parseFromString(content, 'text/xml');
    const records = Array.from(xmlDoc.getElementsByTagName('record'));
    
    this.previewData = records.slice(0, 10).map((record, index) => {
      const item: any = { _rowIndex: index };
      Array.from(record.children).forEach(child => {
        item[child.tagName] = child.textContent || '';
      });
      return item;
    });
  }
  
  private generateColumnMappings() {
    if (this.previewData.length === 0) return;
    
    const sourceColumns = Object.keys(this.previewData[0]).filter(key => key !== '_rowIndex');
    const targetFields = this.getTargetFields();
    
    this.columnMappings = sourceColumns.map(column => {
      const mapping: ColumnMapping = {
        sourceColumn: column,
        targetField: this.suggestTargetField(column, targetFields),
        required: this.isRequiredField(this.suggestTargetField(column, targetFields)),
        dataType: this.detectDataType(column),
        example: this.previewData[0][column]
      };
      
      return mapping;
    });
  }
  
  getTargetFields(): string[] {
    switch (this.entityType) {
      case 'COUNTRIES':
        return ['countryCode', 'countryName', 'iso2Code', 'iso3Code', 'numericCode', 'codeSystem', 'isActive'];
      case 'PORTS':
        return ['portCode', 'portName', 'countryCode', 'city', 'portType', 'latitude', 'longitude', 'isActive'];
      case 'AIRPORTS':
        return ['iataCode', 'icaoCode', 'airportName', 'city', 'countryCode', 'airportType', 'isActive'];
      default:
        return [];
    }
  }
  
  private suggestTargetField(sourceColumn: string, targetFields: string[]): string {
    const normalized = sourceColumn.toLowerCase().replace(/[_\s-]/g, '');
    
    // Direct matches
    const directMatch = targetFields.find(field => 
      field.toLowerCase().replace(/[_\s-]/g, '') === normalized
    );
    if (directMatch) return directMatch;
    
    // Partial matches
    const partialMatch = targetFields.find(field => 
      normalized.includes(field.toLowerCase()) || field.toLowerCase().includes(normalized)
    );
    if (partialMatch) return partialMatch;
    
    return targetFields[0] || '';
  }
  
  private isRequiredField(fieldName: string): boolean {
    const requiredFields = {
      'COUNTRIES': ['countryCode', 'countryName'],
      'PORTS': ['portCode', 'portName', 'countryCode'],
      'AIRPORTS': ['iataCode', 'airportName', 'countryCode']
    };
    
    return requiredFields[this.entityType]?.includes(fieldName) || false;
  }
  
  private detectDataType(column: string): 'string' | 'number' | 'boolean' | 'date' {
    if (!this.previewData.length) return 'string';
    
    const sampleValue = this.previewData[0][column];
    
    if (!isNaN(Number(sampleValue)) && sampleValue !== '') return 'number';
    if (sampleValue === 'true' || sampleValue === 'false') return 'boolean';
    if (this.isDateString(sampleValue)) return 'date';
    
    return 'string';
  }
  
  private isDateString(value: string): boolean {
    const date = new Date(value);
    return !isNaN(date.getTime()) && value.length > 8;
  }
  
  validateImport() {
    if (!this.previewData.length || !this.columnMappings.length) {
      this.toastService.showError('Validation Error', 'No data to validate');
      return;
    }
    
    this.loading = true;
    
    // Simulate validation (in production, this would call the API)
    setTimeout(() => {
      const errors: ValidationError[] = [];
      const warnings: ValidationError[] = [];
      let validRecords = 0;
      
      this.previewData.forEach((row, index) => {
        const requiredMappings = this.columnMappings.filter(m => m.required);
        
        requiredMappings.forEach(mapping => {
          const value = row[mapping.sourceColumn];
          if (!value || value.trim() === '') {
            errors.push({
              row: index + 1,
              column: mapping.sourceColumn,
              value: value,
              error: `${mapping.targetField} is required`,
              severity: 'ERROR'
            });
          }
        });
        
        if (errors.filter(e => e.row === index + 1).length === 0) {
          validRecords++;
        }
      });
      
      this.validationResults = {
        isValid: errors.length === 0,
        errors: errors,
        warnings: warnings,
        totalRecords: this.previewData.length,
        validRecords: validRecords,
        duplicateRecords: 0,
        skippedRecords: this.previewData.length - validRecords
      };
      
      this.loading = false;
      
      if (this.validationResults.isValid) {
        this.toastService.showSuccess('Validation passed', `${validRecords} records ready for import`);
      } else {
        this.toastService.showWarning('Validation issues found', `${errors.length} errors need to be fixed`);
      }
    }, 1500);
  }
  
  executeImport() {
    if (!this.selectedFile) {
      this.toastService.showError('Selection Error', 'Please select a file first');
      return;
    }

    this.isUploading = true;
    this.uploadProgress = 0;

    const formData = new FormData();
    formData.append('file', this.selectedFile);
    formData.append('userId', 'current-user'); 
    formData.append('dataType', this.entityType);
    formData.append('sourceSystem', 'IMPORT_EXPORT_PAGE');
    formData.append('description', `Import of ${this.entityType} data from import/export page`);

    this.apiService.initiateBulkImport(formData).subscribe({
      next: (response) => {
        this.isUploading = false;
        
        let message = `Import process initiated successfully.`;
        this.toastService.showSuccess('Import Initiated', message);
        
        const operationId = this.addToHistory('IMPORT', 'PROCESSING', response.changeRequestId, response.batchId);
        this.pollForStatus(response.batchId, operationId);

        this.clearSelectedFile();
        this.showPreview = false;
        this.changeRequestService.notifyChangeRequestCreated();
      },
      error: (error) => {
        this.isUploading = false;
        this.toastService.showError('Import Error', 'Failed to initiate import process');
        console.error(error);
        this.addToHistory('IMPORT', 'FAILED');
      }
    });
  }

  private pollForStatus(batchId: string, operationId: string) {
    const poll = interval(2000).subscribe(() => {
      this.apiService.getBulkImportStatus(batchId).subscribe({
        next: (status) => {
          if (status.status === 'COMPLETED' || status.status === 'FAILED' || status.status === 'PARTIAL') {
            poll.unsubscribe();
            console.log('Updating operation history with final status:', status);
            this.operationHistoryService.updateOperation(operationId, {
              status: status.status as 'COMPLETED' | 'FAILED' | 'PARTIAL',
              completedTime: new Date().toISOString(),
              recordCount: status.totalRecords,
              successCount: status.recordsProcessed,
              errorCount: status.failedCount,
              warnings: status.warningCount,
              progress: 100
            });
          } else {
            this.operationHistoryService.updateOperation(operationId, {
              progress: status.progressPercentage
            });
          }
        },
        error: (error) => {
          console.error(`Error polling for status of batch ${batchId}`, error);
          poll.unsubscribe();
          this.operationHistoryService.updateOperation(operationId, {
            status: 'FAILED',
            errorDetails: 'Failed to get import status.'
          });
        }
      });
    });
  }

  // ==================== EXPORT FUNCTIONALITY ====================
  
  exportData() {
    this.isExporting = true;
    this.exportProgress = 0;
    const startTime = new Date().toISOString();

    this.apiService.exportData(this.exportEntityType, this.exportFormat).subscribe({
      next: (blob) => {
        this.isExporting = false;
        const completedTime = new Date().toISOString();
        const timestamp = new Date().getTime();
        const filename = `${this.exportEntityType.toLowerCase()}_export_${timestamp}.${this.exportFormat.toLowerCase()}`;
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = filename;
        link.click();
        window.URL.revokeObjectURL(url);
        this.toastService.showSuccess('Export completed', `Successfully downloaded export: ${filename}`);
        this.getRecordCountFromBlob(blob).then(recordCount => {
          const operationId = this.addToHistory('EXPORT', 'COMPLETED', undefined, undefined, recordCount, blob.size);
          this.operationHistoryService.updateOperation(operationId, {
            startTime: startTime,
            completedTime: completedTime
          });
        });
      },
      error: (error) => {
        this.isExporting = false;
        this.toastService.showError('Export Error', 'Failed to export data');
        console.error(error);
        this.addToHistory('EXPORT', 'FAILED');
      }
    });
  }
  
  private getRecordCountFromBlob(blob: Blob): Promise<number> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => {
        const content = reader.result as string;
        let recordCount = 0;
        if (this.exportFormat === 'CSV') {
          // Assuming there is always a header row
          recordCount = content.split('\n').filter(line => line.trim() !== '').length - 1;
        } else if (this.exportFormat === 'JSON') {
          const data = JSON.parse(content);
          recordCount = data.length;
        } else if (this.exportFormat === 'XML') {
          const parser = new DOMParser();
          const xmlDoc = parser.parseFromString(content, 'text/xml');
          recordCount = xmlDoc.getElementsByTagName('record').length;
        }
        resolve(recordCount);
      };
      reader.onerror = reject;
      reader.readAsText(blob);
    });
  }
  
  private downloadExportFile() {
    const timestamp = new Date().getTime();
    const filename = `${this.exportEntityType.toLowerCase()}_export_${timestamp}.${this.exportFormat.toLowerCase()}`;
    
    // Generate sample export data
    let data = '';
    const recordCount = 100;
    
    switch (this.exportFormat) {
      case 'CSV':
        data = this.generateCSVExport(recordCount);
        break;
      case 'JSON':
        data = this.generateJSONExport(recordCount);
        break;
      case 'XML':
        data = this.generateXMLExport(recordCount);
        break;
      case 'EXCEL':
        data = this.generateCSVExport(recordCount); // Simplified for demo
        break;
    }
    
    const blob = new Blob([data], { type: this.getExportMimeType() });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    window.URL.revokeObjectURL(url);
    
    this.toastService.showSuccess('Export completed', `${recordCount} records exported to ${filename}`);
    this.getRecordCountFromBlob(blob).then(recordCount => {
      console.log('recordCount from generated data', recordCount);
    });
  }
  
  private generateCSVExport(recordCount: number): string {
    if (this.exportEntityType === 'COUNTRIES') {
      const headers = 'Country Code,Country Name,ISO2,ISO3,Numeric,Code System,Active';
      const rows = Array.from({ length: recordCount }, (_, i) => {
        const countryCode = `C${String(i + 1).padStart(3, '0')}`;
        return `${countryCode},Country ${i + 1},${countryCode.substring(0, 2)},${countryCode},${String(i + 1).padStart(3, '0')},ISO3166-1,true`;
      });
      return [headers, ...rows].join('\n');
    }
    return '';
  }
  
  private generateJSONExport(recordCount: number): string {
    if (this.exportEntityType === 'COUNTRIES') {
      const data = Array.from({ length: recordCount }, (_, i) => ({
        countryCode: `C${String(i + 1).padStart(3, '0')}`,
        countryName: `Country ${i + 1}`,
        iso2Code: `C${String(i + 1).padStart(3, '0')}`.substring(0, 2),
        iso3Code: `C${String(i + 1).padStart(3, '0')}`,
        numericCode: String(i + 1).padStart(3, '0'),
        codeSystem: 'ISO3166-1',
        isActive: true
      }));
      return JSON.stringify(data, null, 2);
    }
    return '[]';
  }
  
  private generateXMLExport(recordCount: number): string {
    let xml = '<?xml version="1.0" encoding="UTF-8"?>\n<countries>\n';
    for (let i = 0; i < recordCount; i++) {
      const countryCode = `C${String(i + 1).padStart(3, '0')}`;
      xml += `  <country>\n`;
      xml += `    <countryCode>${countryCode}</countryCode>\n`;
      xml += `    <countryName>Country ${i + 1}</countryName>\n`;
      xml += `    <iso2Code>${countryCode.substring(0, 2)}</iso2Code>\n`;
      xml += `    <iso3Code>${countryCode}</iso3Code>\n`;
      xml += `    <numericCode>${String(i + 1).padStart(3, '0')}</numericCode>\n`;
      xml += `    <codeSystem>ISO3166-1</codeSystem>\n`;
      xml += `    <isActive>true</isActive>\n`;
      xml += `  </country>\n`;
    }
    xml += '</countries>';
    return xml;
  }
  
  private getExportMimeType(): string {
    const mimeTypes = {
      'CSV': 'text/csv',
      'JSON': 'application/json',
      'XML': 'application/xml',
      'EXCEL': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    };
    return mimeTypes[this.exportFormat];
  }
  
  downloadTemplate() {
    if (!this.selectedTemplate) {
      this.toastService.showError('Selection Error', 'Please select a template first');
      return;
    }
    
    const csv = this.convertTemplateToCSV(this.selectedTemplate);
    const filename = `${this.selectedTemplate.name.toLowerCase().replace(/\s+/g, '_')}_template.csv`;
    
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    window.URL.revokeObjectURL(url);
    
    this.toastService.showSuccess('Template downloaded', filename);
  }
  
  private convertTemplateToCSV(template: ImportTemplate): string {
    const headers = [...template.requiredColumns, ...template.optionalColumns];
    const sampleRows = template.sampleData.map(row => 
      headers.map(header => row[header] || '').join(',')
    );
    
    return [
      headers.join(','),
      ...sampleRows,
      // Add a few empty rows for users to fill in
      ...Array(3).fill(headers.map(() => '').join(','))
    ].join('\n');
  }

  // ==================== TEMPLATES ====================
  
  private loadImportTemplates() {
    // Mock templates - in production, these would come from the API
    this.importTemplates = [
      {
        entityType: 'COUNTRIES',
        name: 'ISO Countries Standard',
        description: 'Standard template for importing countries using ISO 3166-1 codes',
        requiredColumns: ['countryCode', 'countryName', 'iso2Code', 'iso3Code'],
        optionalColumns: ['numericCode', 'codeSystem', 'isActive'],
        sampleData: [
          {
            countryCode: 'US',
            countryName: 'United States',
            iso2Code: 'US',
            iso3Code: 'USA',
            numericCode: '840',
            codeSystem: 'ISO3166-1',
            isActive: 'true'
          },
          {
            countryCode: 'CA',
            countryName: 'Canada',
            iso2Code: 'CA',
            iso3Code: 'CAN',
            numericCode: '124',
            codeSystem: 'ISO3166-1',
            isActive: 'true'
          }
        ],
        validationRules: [
          'Country code must be unique',
          'ISO2 code must be exactly 2 characters',
          'ISO3 code must be exactly 3 characters',
          'Numeric code must be exactly 3 digits'
        ]
      },
      {
        entityType: 'PORTS',
        name: 'Seaports and Airports',
        description: 'Template for importing port facilities including seaports and airports',
        requiredColumns: ['portCode', 'portName', 'countryCode', 'city'],
        optionalColumns: ['portType', 'latitude', 'longitude', 'timeZone', 'isActive'],
        sampleData: [
          {
            portCode: 'USLAX',
            portName: 'Los Angeles',
            countryCode: 'US',
            city: 'Los Angeles',
            portType: 'SEAPORT',
            latitude: '33.7326',
            longitude: '-118.2437',
            isActive: 'true'
          }
        ],
        validationRules: [
          'Port code must be unique',
          'Country code must exist in countries table',
          'Coordinates must be valid decimal degrees'
        ]
      },
      {
        entityType: 'AIRPORTS',
        name: 'IATA/ICAO Airports',
        description: 'Template for importing airports with IATA and ICAO codes',
        requiredColumns: ['iataCode', 'airportName', 'city', 'countryCode'],
        optionalColumns: ['icaoCode', 'airportType', 'hubSize', 'hasCustoms', 'isActive'],
        sampleData: [
          {
            iataCode: 'LAX',
            icaoCode: 'KLAX',
            airportName: 'Los Angeles International Airport',
            city: 'Los Angeles',
            countryCode: 'US',
            airportType: 'INTERNATIONAL',
            hubSize: 'LARGE_HUB',
            hasCustoms: 'true',
            isActive: 'true'
          }
        ],
        validationRules: [
          'IATA code must be exactly 3 characters',
          'ICAO code must be exactly 4 characters',
          'Country code must exist in countries table'
        ]
      }
    ];
    
    this.selectedTemplate = this.importTemplates[0];
  }

  // ==================== HISTORY ====================
  
  private loadOperationHistory() {
    this.historyLoading = true;
    this.apiService.getOperationHistory({
      page: this.historyPage,
      size: this.historySize,
      filter: this.historyFilter
    }).subscribe({
      next: (response) => {
        this.operationHistory = response.content;
        this.historyTotal = response.totalElements;
        this.historyLoading = false;
      },
      error: (error) => {
        this.toastService.showError('Error', 'Failed to load operation history');
        this.historyLoading = false;
        console.error(error);
      }
    });
  }
  
  onHistoryFilterInput(event: Event) {
    const value = (event.target as HTMLInputElement).value;
    this.historyFilterSubject.next(value);
  }
  
  retryOperation(operation: ImportExportHistory) {
    this.toastService.showInfo('Retrying operation', `Retrying ${operation.operationType.toLowerCase()} of ${operation.fileName}`);
    // In production, this would call the API to retry the operation
  }
  
  downloadOperationFile(operation: ImportExportHistory) {
    if (!operation.batchId) {
      this.toastService.showError('Download failed', 'Batch ID is missing.');
      return;
    }

    this.apiService.downloadImportedFile(operation.batchId).subscribe({
      next: (blob) => {
        console.log('Received blob:', blob);
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = operation.fileName;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
        this.toastService.showSuccess('Download started', `Downloading ${operation.fileName}`);
      },
      error: (error) => {
        console.error('Download error:', error);
        this.toastService.showError('Download failed', 'Could not download the file. See console for details.');
      }
    });
  }

  // ==================== SCHEDULED EXPORTS ====================
  
  private loadScheduledExports() {
    this.apiService.getScheduledExports().subscribe({
        next: (response) => {
            this.scheduledExports = response;
        },
        error: (error) => {
            this.toastService.showError('Error loading scheduled exports', 'Could not load scheduled exports. See console for details.');
            console.error('Error loading scheduled exports:', error);
        }
    });
  }
  
  openScheduleModal(schedule?: ScheduledExport) {
    this.selectedSchedule = schedule || null;
    this.showScheduleModal = true;
  }
  
  closeScheduleModal() {
    this.showScheduleModal = false;
    this.selectedSchedule = null;
  }



  handleScheduleSaved(schedule: ScheduledExport) {
    this.loadScheduledExports();
    this.closeScheduleModal();
  }

  handleScheduleDeleted(schedule: ScheduledExport) {
    this.scheduledExports = this.scheduledExports.filter(s => s.id !== schedule.id);
  }

  deleteScheduledExport(schedule: ScheduledExport) {
    if (confirm(`Are you sure you want to delete the schedule "${schedule.name}"?`)) {
      this.apiService.deleteScheduledExport(schedule.id).subscribe({
        next: () => {
          this.handleScheduleDeleted(schedule);
          this.toastService.showSuccess('Schedule deleted', 'Scheduled export deleted successfully');
        },
        error: (error) => {
          this.toastService.showError('Error deleting schedule', 'Could not delete scheduled export. See console for details.');
          console.error('Error deleting schedule:', error);
        }
      });
    }
  }

  saveScheduledExport() {
    if (this.selectedSchedule) {
      this.apiService.updateScheduledExport(this.selectedSchedule.id, this.scheduleForm).subscribe({
        next: (response) => {
          this.handleScheduleSaved(response);
          this.toastService.showSuccess('Schedule updated', 'Scheduled export updated successfully');
        },
        error: (error) => {
          this.error = 'Could not update scheduled export. See console for details.';
          console.error('Error updating schedule:', error);
        }
      });
    } else {
      this.apiService.createScheduledExport(this.scheduleForm).subscribe({
        next: (response) => {
          this.handleScheduleSaved(response);
          this.toastService.showSuccess('Schedule created', 'Scheduled export created successfully');
        },
        error: (error) => {
          this.error = 'Could not create scheduled export. See console for details.';
          console.error('Error creating schedule:', error);
        }
      });
    }
  }

  toggleScheduleEnabled(schedule: ScheduledExport) {
    const updatedSchedule = { ...schedule, enabled: !schedule.enabled };
    this.apiService.updateScheduledExport(schedule.id, updatedSchedule).subscribe({
      next: (response) => {
        schedule.enabled = response.enabled;
        this.toastService.showSuccess('Schedule updated', `Schedule set to ${response.enabled ? 'enabled' : 'disabled'}`);
      },
      error: (error) => {
        this.toastService.showError('Error updating schedule', 'Could not update schedule status. See console for details.');
        console.error('Error updating schedule status:', error);
      }
    });
  }

  // ==================== UTILITY METHODS ====================
  
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
  
  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
  
  getStatusIcon(status: string): string {
    const icons = {
      'COMPLETED': 'check_circle',
      'FAILED': 'error',
      'PROCESSING': 'hourglass_empty',
      'PARTIAL': 'warning',
      'PENDING': 'schedule'
    };
    return icons[status as keyof typeof icons] || 'help';
  }
  
  getStatusClass(status: string): string {
    const classes = {
      'COMPLETED': 'status-success',
      'FAILED': 'status-error',
      'PROCESSING': 'status-processing',
      'PARTIAL': 'status-warning',
      'PENDING': 'status-pending'
    };
    return classes[status as keyof typeof classes] || '';
  }
  
  getCronDescription(cron: string): string {
    const descriptions: { [key: string]: string } = {
      '0 * * * * *': 'Every minute',
      '0 0 0 * * *': 'Daily at midnight',
      '0 0 0 * * 1': 'Weekly on Monday',
      '0 0 0 1 * *': 'Monthly on 1st',
      '0 0 2 * * *': 'Daily at 2 AM',
      '0 0 0 * * 0': 'Weekly on Sunday'
    };
    return descriptions[cron] || cron;
  }
  
  getDuration(startTime: string, endTime: string): string {
    const start = new Date(startTime);
    const end = new Date(endTime);
    const diffMs = end.getTime() - start.getTime();
    
    const seconds = Math.floor(diffMs / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    
    if (hours > 0) {
      return `${hours}h ${minutes % 60}m`;
    } else if (minutes > 0) {
      return `${minutes}m ${seconds % 60}s`;
    } else {
      return `${seconds}s`;
    }
  }
  
  // Make Object available in template
  Object = Object;
  
  openMappingModal() {
    this.showMappingModal = true;
  }
  
  closeMappingModal() {
    this.showMappingModal = false;
  }
  
  applyColumnMappings() {
    this.closeMappingModal();
    this.validateImport();
  }
}