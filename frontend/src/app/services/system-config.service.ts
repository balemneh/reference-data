import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable, throwError, of } from 'rxjs';
import { map, catchError, tap, retry, timeout } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface SystemConfiguration {
  general: {
    systemName: string;
    timezone: string;
    defaultLocale: string;
    dateFormat: string;
    timeFormat: string;
    fiscalYearStart: string;
    maintenanceWindow: string;
    systemDescription: string;
  };
  security: {
    sessionTimeout: number;
    maxLoginAttempts: number;
    passwordMinLength: number;
    passwordComplexity: boolean;
    twoFactorRequired: boolean;
    ssoEnabled: boolean;
    apiKeyExpiration: number;
    ipWhitelist: string[];
    auditLogRetention: number;
    encryptionAlgorithm: string;
  };
  dataManagement: {
    retentionPeriods: {
      auditLogs: number;
      changeRequests: number;
      exportFiles: number;
      tempFiles: number;
    };
    archivalRules: {
      autoArchive: boolean;
      archiveThreshold: number;
      compressionEnabled: boolean;
      storageLocation: string;
    };
    backupSchedule: {
      frequency: string;
      retentionCount: number;
      storageLocation: string;
      encryptBackups: boolean;
    };
    dataValidation: {
      strictMode: boolean;
      validateOnImport: boolean;
      requireApprovalForChanges: boolean;
    };
  };
  integration: {
    webhooks: {
      enabled: boolean;
      retryAttempts: number;
      timeout: number;
      secretRotationDays: number;
    };
    apiSettings: {
      rateLimit: number;
      requestTimeout: number;
      maxPayloadSize: number;
      corsOrigins: string[];
    };
    externalSystems: {
      cbpSystems: boolean;
      customIntegrations: string[];
      scheduledSyncs: boolean;
      syncInterval: number;
    };
  };
  notifications: {
    email: {
      enabled: boolean;
      smtpServer: string;
      smtpPort: number;
      smtpSecurity: string;
      fromAddress: string;
      templates: {
        changeRequestApproval: boolean;
        systemAlerts: boolean;
        dataUpdates: boolean;
        securityAlerts: boolean;
      };
    };
    alerts: {
      systemHealth: boolean;
      performanceThresholds: {
        cpuUsage: number;
        memoryUsage: number;
        diskUsage: number;
        responseTime: number;
      };
      errorThresholds: {
        errorRate: number;
        consecutiveFailures: number;
      };
    };
  };
  performance: {
    caching: {
      enabled: boolean;
      defaultTtl: number;
      maxCacheSize: number;
      evictionPolicy: string;
    };
    rateLimiting: {
      enabled: boolean;
      requestsPerMinute: number;
      burstLimit: number;
      blockDuration: number;
    };
    batchProcessing: {
      defaultBatchSize: number;
      maxBatchSize: number;
      processingTimeout: number;
      parallelProcessing: boolean;
    };
    monitoring: {
      metricsEnabled: boolean;
      detailedLogging: boolean;
      performanceTracking: boolean;
      alertingEnabled: boolean;
    };
  };
}

export interface ConfigurationTemplate {
  id: string;
  name: string;
  description: string;
  environment: 'development' | 'test' | 'staging' | 'production';
  configuration: Partial<SystemConfiguration>;
  createdAt: Date;
  createdBy: string;
  tags: string[];
}

export interface ConfigurationChange {
  id: string;
  section: string;
  field: string;
  oldValue: any;
  newValue: any;
  changedBy: string;
  changedAt: Date;
  approvedBy?: string;
  approvedAt?: Date;
  status: 'pending' | 'approved' | 'rejected';
  reason?: string;
  riskLevel: 'low' | 'medium' | 'high' | 'critical';
}

export interface ConfigurationBackup {
  id: string;
  name: string;
  description: string;
  configuration: SystemConfiguration;
  createdAt: Date;
  createdBy: string;
  environment: string;
  version: string;
  checksum: string;
}

export interface ValidationResult {
  isValid: boolean;
  errors: ValidationError[];
  warnings: ValidationWarning[];
  criticalIssues: string[];
}

export interface ValidationError {
  field: string;
  message: string;
  severity: 'error' | 'warning';
}

export interface ValidationWarning {
  field: string;
  message: string;
  impact: string;
  recommendation: string;
}

export interface ConfigurationDiff {
  section: string;
  field: string;
  oldValue: any;
  newValue: any;
  changeType: 'added' | 'modified' | 'removed';
  riskLevel: 'low' | 'medium' | 'high' | 'critical';
}

@Injectable({
  providedIn: 'root'
})
export class SystemConfigService {
  private readonly apiUrl = `${environment.apiUrl}/api/v1/system-config`;
  
  // State management
  private configurationSubject = new BehaviorSubject<SystemConfiguration | null>(null);
  private loadingSubject = new BehaviorSubject<boolean>(false);
  private errorSubject = new BehaviorSubject<string | null>(null);

  // Public observables
  public configuration$ = this.configurationSubject.asObservable();
  public loading$ = this.loadingSubject.asObservable();
  public error$ = this.errorSubject.asObservable();

  constructor(private http: HttpClient) {
    this.loadConfiguration();
  }

  // Configuration Management
  loadConfiguration(): Observable<SystemConfiguration> {
    this.loadingSubject.next(true);
    this.errorSubject.next(null);

    return this.http.get<SystemConfiguration>(`${this.apiUrl}/current`)
      .pipe(
        timeout(10000),
        retry(2),
        tap(config => {
          this.configurationSubject.next(config);
          this.loadingSubject.next(false);
        }),
        catchError(error => {
          // Load mock data on error for demonstration
          const mockConfig = this.getMockConfiguration();
          this.configurationSubject.next(mockConfig);
          this.loadingSubject.next(false);
          console.log('API not available, loading mock configuration for demonstration');
          return of(mockConfig);
        })
      );
  }

  private getMockConfiguration(): SystemConfiguration {
    return {
      general: {
        systemName: 'CBP Reference Data Service',
        timezone: 'America/New_York',
        defaultLocale: 'en-US',
        dateFormat: 'MM/DD/YYYY',
        timeFormat: '12h',
        fiscalYearStart: 'October',
        maintenanceWindow: 'Sunday 2:00 AM - 6:00 AM EST',
        systemDescription: 'Centralized reference data management system for CBP operations'
      },
      security: {
        sessionTimeout: 30,
        maxLoginAttempts: 3,
        passwordMinLength: 12,
        passwordComplexity: true,
        twoFactorRequired: true,
        ssoEnabled: true,
        apiKeyExpiration: 90,
        ipWhitelist: ['10.0.0.0/8', '172.16.0.0/12', '192.168.0.0/16'],
        auditLogRetention: 365,
        encryptionAlgorithm: 'AES-256-GCM'
      },
      dataManagement: {
        retentionPeriods: {
          auditLogs: 365,
          changeRequests: 730,
          exportFiles: 30,
          tempFiles: 7
        },
        archivalRules: {
          autoArchive: true,
          archiveThreshold: 180,
          compressionEnabled: true,
          storageLocation: 's3://cbp-refdata-archive'
        },
        backupSchedule: {
          frequency: 'Daily',
          retentionCount: 30,
          storageLocation: 's3://cbp-refdata-backups',
          encryptBackups: true
        },
        dataValidation: {
          strictMode: true,
          validateOnImport: true,
          requireApprovalForChanges: true
        }
      },
      integration: {
        webhooks: {
          enabled: true,
          retryAttempts: 3,
          timeout: 30,
          secretRotationDays: 90
        },
        apiSettings: {
          rateLimit: 1000,
          requestTimeout: 60,
          maxPayloadSize: 10485760,
          corsOrigins: ['https://cbp.gov', 'https://*.cbp.dhs.gov']
        },
        externalSystems: {
          cbpSystems: true,
          customIntegrations: ['TECS', 'ACE', 'APIS', 'ATS'],
          scheduledSyncs: true,
          syncInterval: 3600
        }
      },
      notifications: {
        email: {
          enabled: true,
          smtpServer: 'smtp.cbp.dhs.gov',
          smtpPort: 587,
          smtpSecurity: 'STARTTLS',
          fromAddress: 'refdata-noreply@cbp.dhs.gov',
          templates: {
            changeRequestApproval: true,
            systemAlerts: true,
            dataUpdates: true,
            securityAlerts: true
          }
        },
        alerts: {
          systemHealth: true,
          performanceThresholds: {
            cpuUsage: 80,
            memoryUsage: 85,
            diskUsage: 90,
            responseTime: 2000
          },
          errorThresholds: {
            errorRate: 5,
            consecutiveFailures: 10
          }
        }
      },
      performance: {
        caching: {
          enabled: true,
          defaultTtl: 3600,
          maxCacheSize: 1073741824,
          evictionPolicy: 'LRU'
        },
        rateLimiting: {
          enabled: true,
          requestsPerMinute: 60,
          burstLimit: 100,
          blockDuration: 300
        },
        batchProcessing: {
          defaultBatchSize: 100,
          maxBatchSize: 1000,
          processingTimeout: 300,
          parallelProcessing: true
        },
        monitoring: {
          metricsEnabled: true,
          detailedLogging: true,
          performanceTracking: true,
          alertingEnabled: true
        }
      }
    };
  }

  saveConfiguration(configuration: SystemConfiguration, comment?: string): Observable<SystemConfiguration> {
    this.loadingSubject.next(true);
    this.errorSubject.next(null);

    const payload = {
      configuration,
      comment,
      timestamp: new Date().toISOString()
    };

    return this.http.put<SystemConfiguration>(`${this.apiUrl}/current`, payload)
      .pipe(
        timeout(15000),
        tap(config => {
          this.configurationSubject.next(config);
          this.loadingSubject.next(false);
        }),
        catchError(error => {
          this.loadingSubject.next(false);
          this.errorSubject.next(this.handleError(error));
          return throwError(() => error);
        })
      );
  }

  validateConfiguration(configuration: SystemConfiguration): Observable<ValidationResult> {
    return this.http.post<ValidationResult>(`${this.apiUrl}/validate`, configuration)
      .pipe(
        timeout(10000),
        catchError(error => {
          console.error('Configuration validation failed:', error);
          return throwError(() => error);
        })
      );
  }

  resetConfiguration(): Observable<SystemConfiguration> {
    this.loadingSubject.next(true);
    
    return this.http.post<SystemConfiguration>(`${this.apiUrl}/reset`, {})
      .pipe(
        timeout(10000),
        tap(config => {
          this.configurationSubject.next(config);
          this.loadingSubject.next(false);
        }),
        catchError(error => {
          this.loadingSubject.next(false);
          this.errorSubject.next(this.handleError(error));
          return throwError(() => error);
        })
      );
  }

  // Template Management
  getTemplates(): Observable<ConfigurationTemplate[]> {
    return this.http.get<ConfigurationTemplate[]>(`${this.apiUrl}/templates`)
      .pipe(
        timeout(5000),
        catchError(error => {
          console.error('Failed to load templates:', error);
          return of([]);
        })
      );
  }

  createTemplate(template: Omit<ConfigurationTemplate, 'id' | 'createdAt'>): Observable<ConfigurationTemplate> {
    return this.http.post<ConfigurationTemplate>(`${this.apiUrl}/templates`, template)
      .pipe(
        timeout(10000),
        catchError(error => {
          console.error('Failed to create template:', error);
          return throwError(() => error);
        })
      );
  }

  applyTemplate(templateId: string): Observable<SystemConfiguration> {
    this.loadingSubject.next(true);
    
    return this.http.post<SystemConfiguration>(`${this.apiUrl}/templates/${templateId}/apply`, {})
      .pipe(
        timeout(10000),
        tap(config => {
          this.configurationSubject.next(config);
          this.loadingSubject.next(false);
        }),
        catchError(error => {
          this.loadingSubject.next(false);
          this.errorSubject.next(this.handleError(error));
          return throwError(() => error);
        })
      );
  }

  deleteTemplate(templateId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/templates/${templateId}`)
      .pipe(
        timeout(5000),
        catchError(error => {
          console.error('Failed to delete template:', error);
          return throwError(() => error);
        })
      );
  }

  // Backup Management
  getBackups(): Observable<ConfigurationBackup[]> {
    return this.http.get<ConfigurationBackup[]>(`${this.apiUrl}/backups`)
      .pipe(
        timeout(5000),
        map(backups => backups.map(backup => ({
          ...backup,
          createdAt: new Date(backup.createdAt)
        }))),
        catchError(error => {
          console.error('Failed to load backups:', error);
          return of([]);
        })
      );
  }

  createBackup(name: string, description?: string): Observable<ConfigurationBackup> {
    const payload = {
      name,
      description,
      timestamp: new Date().toISOString()
    };

    return this.http.post<ConfigurationBackup>(`${this.apiUrl}/backups`, payload)
      .pipe(
        timeout(10000),
        map(backup => ({
          ...backup,
          createdAt: new Date(backup.createdAt)
        })),
        catchError(error => {
          console.error('Failed to create backup:', error);
          return throwError(() => error);
        })
      );
  }

  restoreBackup(backupId: string): Observable<SystemConfiguration> {
    this.loadingSubject.next(true);
    
    return this.http.post<SystemConfiguration>(`${this.apiUrl}/backups/${backupId}/restore`, {})
      .pipe(
        timeout(15000),
        tap(config => {
          this.configurationSubject.next(config);
          this.loadingSubject.next(false);
        }),
        catchError(error => {
          this.loadingSubject.next(false);
          this.errorSubject.next(this.handleError(error));
          return throwError(() => error);
        })
      );
  }

  deleteBackup(backupId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/backups/${backupId}`)
      .pipe(
        timeout(5000),
        catchError(error => {
          console.error('Failed to delete backup:', error);
          return throwError(() => error);
        })
      );
  }

  // Change Management
  getChangeHistory(limit = 50, offset = 0): Observable<ConfigurationChange[]> {
    const params = new HttpParams()
      .set('limit', limit.toString())
      .set('offset', offset.toString());

    return this.http.get<ConfigurationChange[]>(`${this.apiUrl}/changes`, { params })
      .pipe(
        timeout(5000),
        map(changes => changes.map(change => ({
          ...change,
          changedAt: new Date(change.changedAt),
          approvedAt: change.approvedAt ? new Date(change.approvedAt) : undefined
        }))),
        catchError(error => {
          console.error('Failed to load change history:', error);
          return of([]);
        })
      );
  }

  submitChangeRequest(changes: ConfigurationDiff[], comment?: string): Observable<ConfigurationChange> {
    const payload = {
      changes,
      comment,
      timestamp: new Date().toISOString()
    };

    return this.http.post<ConfigurationChange>(`${this.apiUrl}/changes`, payload)
      .pipe(
        timeout(10000),
        map(change => ({
          ...change,
          changedAt: new Date(change.changedAt),
          approvedAt: change.approvedAt ? new Date(change.approvedAt) : undefined
        })),
        catchError(error => {
          console.error('Failed to submit change request:', error);
          return throwError(() => error);
        })
      );
  }

  approveChange(changeId: string, comment?: string): Observable<ConfigurationChange> {
    const payload = { comment };
    
    return this.http.post<ConfigurationChange>(`${this.apiUrl}/changes/${changeId}/approve`, payload)
      .pipe(
        timeout(10000),
        map(change => ({
          ...change,
          changedAt: new Date(change.changedAt),
          approvedAt: change.approvedAt ? new Date(change.approvedAt) : undefined
        })),
        catchError(error => {
          console.error('Failed to approve change:', error);
          return throwError(() => error);
        })
      );
  }

  rejectChange(changeId: string, reason: string): Observable<ConfigurationChange> {
    const payload = { reason };
    
    return this.http.post<ConfigurationChange>(`${this.apiUrl}/changes/${changeId}/reject`, payload)
      .pipe(
        timeout(10000),
        map(change => ({
          ...change,
          changedAt: new Date(change.changedAt),
          approvedAt: change.approvedAt ? new Date(change.approvedAt) : undefined
        })),
        catchError(error => {
          console.error('Failed to reject change:', error);
          return throwError(() => error);
        })
      );
  }

  // Import/Export
  exportConfiguration(format: 'json' | 'yaml' = 'json'): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/export`, {
      params: { format },
      responseType: 'blob'
    }).pipe(
      timeout(10000),
      catchError(error => {
        console.error('Failed to export configuration:', error);
        return throwError(() => error);
      })
    );
  }

  importConfiguration(file: File, validateOnly = false): Observable<SystemConfiguration | ValidationResult> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('validateOnly', validateOnly.toString());

    return this.http.post<SystemConfiguration | ValidationResult>(`${this.apiUrl}/import`, formData)
      .pipe(
        timeout(30000),
        catchError(error => {
          console.error('Failed to import configuration:', error);
          return throwError(() => error);
        })
      );
  }

  // Utility Methods
  compareConfigurations(config1: SystemConfiguration, config2: SystemConfiguration): ConfigurationDiff[] {
    const diffs: ConfigurationDiff[] = [];
    
    // Deep comparison logic would go here
    // This is a simplified example
    const sections = Object.keys(config1) as (keyof SystemConfiguration)[];
    
    sections.forEach(section => {
      const section1 = config1[section];
      const section2 = config2[section];
      
      if (typeof section1 === 'object' && typeof section2 === 'object') {
        const fields = new Set([...Object.keys(section1), ...Object.keys(section2)]);
        
        fields.forEach(field => {
          const value1 = (section1 as any)[field];
          const value2 = (section2 as any)[field];
          
          if (JSON.stringify(value1) !== JSON.stringify(value2)) {
            diffs.push({
              section: section as string,
              field,
              oldValue: value1,
              newValue: value2,
              changeType: value1 === undefined ? 'added' : 
                         value2 === undefined ? 'removed' : 'modified',
              riskLevel: this.assessRiskLevel(section as string, field)
            });
          }
        });
      }
    });
    
    return diffs;
  }

  private assessRiskLevel(section: string, field: string): 'low' | 'medium' | 'high' | 'critical' {
    // Risk assessment logic
    const criticalFields = ['sessionTimeout', 'encryptionAlgorithm', 'auditLogRetention'];
    const highRiskFields = ['twoFactorRequired', 'ssoEnabled', 'rateLimit'];
    const mediumRiskFields = ['passwordMinLength', 'maxLoginAttempts'];
    
    if (criticalFields.includes(field)) return 'critical';
    if (highRiskFields.includes(field)) return 'high';
    if (mediumRiskFields.includes(field)) return 'medium';
    
    return 'low';
  }

  getCurrentConfiguration(): SystemConfiguration | null {
    return this.configurationSubject.value;
  }

  clearError(): void {
    this.errorSubject.next(null);
  }

  private handleError(error: any): string {
    if (error.status === 0) {
      return 'Unable to connect to the server. Please check your network connection.';
    } else if (error.status === 401) {
      return 'You are not authorized to perform this action. Please log in and try again.';
    } else if (error.status === 403) {
      return 'You do not have permission to modify system configuration.';
    } else if (error.status === 404) {
      return 'Configuration endpoint not found. Please contact system administrator.';
    } else if (error.status === 422) {
      return error.error?.message || 'Invalid configuration data provided.';
    } else if (error.status >= 500) {
      return 'Server error occurred. Please try again later or contact support.';
    } else {
      return error.error?.message || 'An unexpected error occurred.';
    }
  }
}