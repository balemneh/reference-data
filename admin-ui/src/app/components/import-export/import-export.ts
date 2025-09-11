import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, CountryDto, PortDto, AirportDto } from '../../services/api.service';
import { ToastService } from '../../services/toast.service';
import { debounceTime, distinctUntilChanged, Subject, Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

export interface ImportExportHistory {
  id: string;
  operationType: 'IMPORT' | 'EXPORT';
  entityType: 'COUNTRIES' | 'PORTS' | 'AIRPORTS' | 'ALL';
  fileName: string;
  fileSize: number;
  format: 'CSV' | 'JSON' | 'XML' | 'EXCEL';
  recordCount: number;
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

export interface ScheduledExport {
  id: string;
  name: string;
  entityType: 'COUNTRIES' | 'PORTS' | 'AIRPORTS' | 'ALL';
  format: 'CSV' | 'JSON' | 'XML' | 'EXCEL';
  schedule: string; // cron expression
  enabled: boolean;
  filters?: any;
  lastRun?: string;
  nextRun?: string;
  createdBy: string;
  recipients?: string[];
}

@Component({
  selector: 'app-import-export',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './import-export.html',
  styleUrl: './import-export.scss'
})
export class ImportExportComponent implements OnInit {
  @ViewChild('fileInput') fileInput!: ElementRef;
  @ViewChild('dropZone') dropZone!: ElementRef;
  
  // Main navigation
  activeTab: 'import' | 'export' | 'history' | 'scheduled' = 'import';
  
  // Import functionality
  selectedFile: File | null = null;
  importFormat: 'CSV' | 'JSON' | 'XML' = 'CSV';
  entityType: 'COUNTRIES' | 'PORTS' | 'AIRPORTS' = 'COUNTRIES';
  dragOver = false;
  uploadProgress = 0;
  isUploading = false;
  
  // Import validation and preview
  showPreview = false;
  previewData: any[] = [];
  validationResults: ImportValidation | null = null;
  columnMappings: ColumnMapping[] = [];
  showMappingModal = false;
  skipFirstRow = true;
  
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
  scheduleForm: any = {
    name: '',
    entityType: 'COUNTRIES',
    format: 'CSV',
    schedule: '0 0 * * 1', // Weekly on Monday
    enabled: true,
    recipients: []
  };
  
  // General
  loading = false;
  error: string | null = null;
  
  constructor(
    private apiService: ApiService, 
    private toastService: ToastService
  ) {}

  ngOnInit() {
    this.loadImportTemplates();
    this.loadOperationHistory();
    this.loadScheduledExports();
    this.initializeHistoryFilterSubscription();
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
      this.loadOperationHistory();
    });
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
      this.error = 'Please select a valid file format (CSV, JSON, XML, or Excel)';
      this.toastService.showError('Invalid file type', this.error);
      return;
    }
    
    // Validate file size (max 50MB)
    const maxSize = 50 * 1024 * 1024;
    if (file.size > maxSize) {
      this.error = 'File size must be less than 50MB';
      this.toastService.showError('File too large', this.error);
      return;
    }
    
    this.selectedFile = file;
    this.error = null;
    
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
    this.error = null;
    
    if (this.fileInput) {
      this.fileInput.nativeElement.value = '';
    }
  }

  // ==================== IMPORT FUNCTIONALITY ====================
  
  previewImport() {
    if (!this.selectedFile) {
      this.error = 'Please select a file first';
      return;
    }
    
    this.loading = true;
    this.error = null;
    
    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const content = e.target?.result as string;
        this.parseFileContent(content);
        this.generateColumnMappings();
        this.showPreview = true;
      } catch (error) {
        this.error = 'Failed to parse file content';
        this.toastService.showError('Parse error', this.error);
      } finally {
        this.loading = false;
      }
    };
    
    reader.onerror = () => {
      this.error = 'Failed to read file';
      this.loading = false;
    };
    
    reader.readAsText(this.selectedFile);
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
      this.error = 'No data to validate';
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
    if (!this.validationResults?.isValid) {
      this.error = 'Please fix validation errors before importing';
      return;
    }
    
    this.isUploading = true;
    this.uploadProgress = 0;
    this.error = null;
    
    // Simulate import progress
    const progressInterval = setInterval(() => {
      this.uploadProgress += Math.random() * 20;
      if (this.uploadProgress >= 100) {
        this.uploadProgress = 100;
        clearInterval(progressInterval);
        
        setTimeout(() => {
          this.isUploading = false;
          this.toastService.showSuccess('Import completed', 
            `${this.validationResults!.validRecords} records imported successfully`);
          this.addToHistory('IMPORT', 'COMPLETED');
          this.clearSelectedFile();
          this.showPreview = false;
        }, 500);
      }
    }, 200);
  }

  // ==================== EXPORT FUNCTIONALITY ====================
  
  exportData() {
    this.isExporting = true;
    this.exportProgress = 0;
    this.error = null;
    
    const exportParams = {
      entityType: this.exportEntityType,
      format: this.exportFormat,
      includeInactive: this.includeInactive,
      dateFrom: this.dateRangeFrom,
      dateTo: this.dateRangeTo,
      filters: this.exportFilters
    };
    
    // Simulate export progress
    const progressInterval = setInterval(() => {
      this.exportProgress += Math.random() * 15;
      if (this.exportProgress >= 100) {
        this.exportProgress = 100;
        clearInterval(progressInterval);
        
        setTimeout(() => {
          this.isExporting = false;
          this.downloadExportFile();
          this.addToHistory('EXPORT', 'COMPLETED');
        }, 500);
      }
    }, 300);
  }
  
  private downloadExportFile() {
    const timestamp = new Date().getTime();
    const filename = `${this.exportEntityType.toLowerCase()}_export_${timestamp}.${this.exportFormat.toLowerCase()}`;
    
    // Generate sample export data
    let data = '';
    const recordCount = Math.floor(Math.random() * 1000) + 100;
    
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
      this.error = 'Please select a template first';
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
    
    // Mock history data - in production, this would call the API
    setTimeout(() => {
      this.operationHistory = this.generateMockHistory();
      this.historyTotal = 50; // Mock total
      this.historyLoading = false;
    }, 500);
  }
  
  private generateMockHistory(): ImportExportHistory[] {
    const operations: ImportExportHistory[] = [];
    const statuses: ImportExportHistory['status'][] = ['COMPLETED', 'FAILED', 'PARTIAL', 'PROCESSING'];
    const entityTypes: ImportExportHistory['entityType'][] = ['COUNTRIES', 'PORTS', 'AIRPORTS'];
    const formats: ImportExportHistory['format'][] = ['CSV', 'JSON', 'XML', 'EXCEL'];
    
    for (let i = 0; i < 20; i++) {
      const isImport = Math.random() > 0.5;
      const status = statuses[Math.floor(Math.random() * statuses.length)];
      const entityType = entityTypes[Math.floor(Math.random() * entityTypes.length)];
      const format = formats[Math.floor(Math.random() * formats.length)];
      const recordCount = Math.floor(Math.random() * 1000) + 10;
      const errorCount = status === 'FAILED' ? recordCount : Math.floor(Math.random() * 10);
      const successCount = recordCount - errorCount;
      
      operations.push({
        id: `op_${i + 1}`,
        operationType: isImport ? 'IMPORT' : 'EXPORT',
        entityType: entityType,
        fileName: `${entityType.toLowerCase()}_data_${Date.now() - i * 86400000}.${format.toLowerCase()}`,
        fileSize: Math.floor(Math.random() * 10000000) + 1000,
        format: format,
        recordCount: recordCount,
        status: status,
        startTime: new Date(Date.now() - i * 86400000).toISOString(),
        completedTime: status === 'PROCESSING' ? undefined : new Date(Date.now() - i * 86400000 + 300000).toISOString(),
        successCount: successCount,
        errorCount: errorCount,
        warnings: Math.floor(Math.random() * 5),
        createdBy: 'system',
        progress: status === 'PROCESSING' ? Math.floor(Math.random() * 90) + 10 : 100
      });
    }
    
    return operations;
  }
  
  private addToHistory(operationType: 'IMPORT' | 'EXPORT', status: ImportExportHistory['status']) {
    const newOperation: ImportExportHistory = {
      id: `op_${Date.now()}`,
      operationType: operationType,
      entityType: this.entityType,
      fileName: this.selectedFile?.name || `${this.exportEntityType.toLowerCase()}_export.${this.exportFormat.toLowerCase()}`,
      fileSize: this.selectedFile?.size || 0,
      format: operationType === 'IMPORT' ? this.importFormat : this.exportFormat,
      recordCount: this.validationResults?.validRecords || Math.floor(Math.random() * 1000),
      status: status,
      startTime: new Date().toISOString(),
      completedTime: new Date().toISOString(),
      successCount: this.validationResults?.validRecords || Math.floor(Math.random() * 1000),
      errorCount: 0,
      warnings: 0,
      createdBy: 'current-user',
      progress: 100
    };
    
    this.operationHistory.unshift(newOperation);
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
    this.toastService.showSuccess('Download started', `Downloading ${operation.fileName}`);
    // In production, this would download the file from the server
  }

  // ==================== SCHEDULED EXPORTS ====================
  
  private loadScheduledExports() {
    // Mock scheduled exports
    this.scheduledExports = [
      {
        id: 'sch_1',
        name: 'Weekly Countries Export',
        entityType: 'COUNTRIES',
        format: 'CSV',
        schedule: '0 0 * * 1',
        enabled: true,
        lastRun: new Date(Date.now() - 86400000).toISOString(),
        nextRun: new Date(Date.now() + 6 * 86400000).toISOString(),
        createdBy: 'admin',
        recipients: ['admin@example.com']
      },
      {
        id: 'sch_2',
        name: 'Daily Ports Backup',
        entityType: 'PORTS',
        format: 'JSON',
        schedule: '0 2 * * *',
        enabled: false,
        lastRun: new Date(Date.now() - 2 * 86400000).toISOString(),
        nextRun: new Date(Date.now() + 86400000).toISOString(),
        createdBy: 'system',
        recipients: []
      }
    ];
  }
  
  openScheduleModal(schedule?: ScheduledExport) {
    if (schedule) {
      this.selectedSchedule = schedule;
      this.scheduleForm = { ...schedule };
    } else {
      this.selectedSchedule = null;
      this.scheduleForm = {
        name: '',
        entityType: 'COUNTRIES',
        format: 'CSV',
        schedule: '0 0 * * 1',
        enabled: true,
        recipients: []
      };
    }
    this.showScheduleModal = true;
  }
  
  saveScheduledExport() {
    if (!this.scheduleForm.name.trim()) {
      this.error = 'Schedule name is required';
      return;
    }
    
    if (this.selectedSchedule) {
      // Update existing schedule
      const index = this.scheduledExports.findIndex(s => s.id === this.selectedSchedule!.id);
      if (index >= 0) {
        this.scheduledExports[index] = { ...this.scheduleForm, id: this.selectedSchedule.id };
      }
      this.toastService.showSuccess('Schedule updated', 'Scheduled export updated successfully');
    } else {
      // Create new schedule
      const newSchedule: ScheduledExport = {
        ...this.scheduleForm,
        id: `sch_${Date.now()}`,
        createdBy: 'current-user'
      };
      this.scheduledExports.unshift(newSchedule);
      this.toastService.showSuccess('Schedule created', 'New scheduled export created successfully');
    }
    
    this.closeScheduleModal();
  }
  
  deleteScheduledExport(schedule: ScheduledExport) {
    if (confirm(`Are you sure you want to delete the schedule "${schedule.name}"?`)) {
      this.scheduledExports = this.scheduledExports.filter(s => s.id !== schedule.id);
      this.toastService.showSuccess('Schedule deleted', 'Scheduled export deleted successfully');
    }
  }
  
  toggleScheduleEnabled(schedule: ScheduledExport) {
    schedule.enabled = !schedule.enabled;
    this.toastService.showSuccess('Schedule updated', 
      `Schedule ${schedule.enabled ? 'enabled' : 'disabled'}`);
  }
  
  closeScheduleModal() {
    this.showScheduleModal = false;
    this.selectedSchedule = null;
    this.error = null;
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
      '0 0 * * *': 'Daily at midnight',
      '0 0 * * 1': 'Weekly on Monday',
      '0 0 1 * *': 'Monthly on 1st',
      '0 2 * * *': 'Daily at 2 AM',
      '0 0 * * 0': 'Weekly on Sunday'
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