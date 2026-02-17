
import { Injectable } from '@angular/core';
import { ImportExportHistory } from '../models/import-export-history.model';

@Injectable({
  providedIn: 'root'
})
export class ImportExportService {
  private operationHistory: ImportExportHistory[] = [];

  constructor() { }

  getHistory(): ImportExportHistory[] {
    if (this.operationHistory.length === 0) {
      this.loadHistory();
    }
    return this.operationHistory;
  }

  addToHistory(operation: ImportExportHistory) {
    this.operationHistory.unshift(operation);
  }

  private loadHistory() {
    // In a real application, this would fetch data from an API
    // For now, we'll use the same mock data generation
    this.operationHistory = this.generateMockHistory();
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
}
