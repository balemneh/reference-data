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
