import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, takeUntil, debounceTime, distinctUntilChanged } from 'rxjs';
import { ToastService } from '../../services/toast.service';
import { ApiService } from '../../services/api.service';
import { SystemConfigService } from '../../services/system-config.service';

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
  tags?: string[];
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
}

@Component({
  selector: 'app-system-config',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './system-config.html',
  styleUrl: './system-config.scss'
})
export class SystemConfigComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  configuration: SystemConfiguration = {
    general: {
      systemName: 'CBP Reference Data Service',
      timezone: 'America/New_York',
      defaultLocale: 'en-US',
      dateFormat: 'MM/dd/yyyy',
      timeFormat: '12h',
      fiscalYearStart: 'October',
      maintenanceWindow: 'Sunday 02:00-04:00',
      systemDescription: 'Centralized reference data management system for CBP operations'
    },
    security: {
      sessionTimeout: 480,
      maxLoginAttempts: 3,
      passwordMinLength: 12,
      passwordComplexity: true,
      twoFactorRequired: true,
      ssoEnabled: true,
      apiKeyExpiration: 365,
      ipWhitelist: [],
      auditLogRetention: 2555,
      encryptionAlgorithm: 'AES-256-GCM'
    },
    dataManagement: {
      retentionPeriods: {
        auditLogs: 2555,
        changeRequests: 1095,
        exportFiles: 90,
        tempFiles: 7
      },
      archivalRules: {
        autoArchive: true,
        archiveThreshold: 365,
        compressionEnabled: true,
        storageLocation: 's3://cbp-refdata-archive'
      },
      backupSchedule: {
        frequency: 'daily',
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
        requestTimeout: 30,
        maxPayloadSize: 10485760,
        corsOrigins: ['https://*.cbp.dhs.gov']
      },
      externalSystems: {
        cbpSystems: true,
        customIntegrations: [],
        scheduledSyncs: true,
        syncInterval: 60
      }
    },
    notifications: {
      email: {
        enabled: true,
        smtpServer: 'smtp.cbp.dhs.gov',
        smtpPort: 587,
        smtpSecurity: 'STARTTLS',
        fromAddress: 'refdata-system@cbp.dhs.gov',
        templates: {
          changeRequestApproval: true,
          systemAlerts: true,
          dataUpdates: false,
          securityAlerts: true
        }
      },
      alerts: {
        systemHealth: true,
        performanceThresholds: {
          cpuUsage: 80,
          memoryUsage: 85,
          diskUsage: 90,
          responseTime: 5000
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
        defaultBatchSize: 1000,
        maxBatchSize: 10000,
        processingTimeout: 300,
        parallelProcessing: true
      },
      monitoring: {
        metricsEnabled: true,
        detailedLogging: false,
        performanceTracking: true,
        alertingEnabled: true
      }
    }
  };

  originalConfiguration: SystemConfiguration = structuredClone(this.configuration);
  configurationForm: FormGroup;
  loading = false;
  saving = false;
  currentSection = 'general';
  searchQuery = '';
  showAdvanced = false;
  
  // Configuration management
  templates: ConfigurationTemplate[] = [];
  pendingChanges: ConfigurationChange[] = [];
  auditTrail: ConfigurationChange[] = [];
  
  // Validation and warnings
  validationErrors: { [key: string]: string[] } = {};
  criticalWarnings: string[] = [];
  
  // Backup and restore
  backupHistory: any[] = [];
  
  sections = [
    {
      id: 'general',
      title: 'General Settings',
      icon: 'settings',
      description: 'System name, timezone, locale, and basic configuration'
    },
    {
      id: 'security',
      title: 'Security Settings',
      icon: 'security',
      description: 'Authentication, authorization, and security policies'
    },
    {
      id: 'dataManagement',
      title: 'Data Management',
      icon: 'database',
      description: 'Retention policies, archival rules, and backup settings'
    },
    {
      id: 'integration',
      title: 'Integration Settings',
      icon: 'link',
      description: 'API settings, webhooks, and external system connections'
    },
    {
      id: 'notifications',
      title: 'Notification Settings',
      icon: 'notifications',
      description: 'Email servers, alert thresholds, and notification templates'
    },
    {
      id: 'performance',
      title: 'Performance Settings',
      icon: 'speed',
      description: 'Caching, rate limiting, and performance optimization'
    }
  ];

  timezoneOptions = [
    { value: 'America/New_York', label: 'Eastern Time (ET)' },
    { value: 'America/Chicago', label: 'Central Time (CT)' },
    { value: 'America/Denver', label: 'Mountain Time (MT)' },
    { value: 'America/Los_Angeles', label: 'Pacific Time (PT)' },
    { value: 'America/Anchorage', label: 'Alaska Time (AKT)' },
    { value: 'Pacific/Honolulu', label: 'Hawaii Time (HST)' },
    { value: 'UTC', label: 'Coordinated Universal Time (UTC)' }
  ];

  localeOptions = [
    { value: 'en-US', label: 'English (United States)' },
    { value: 'es-US', label: 'Spanish (United States)' },
    { value: 'fr-FR', label: 'French (France)' }
  ];

  encryptionOptions = [
    { value: 'AES-256-GCM', label: 'AES-256-GCM (Recommended)' },
    { value: 'AES-256-CBC', label: 'AES-256-CBC' },
    { value: 'AES-192-GCM', label: 'AES-192-GCM' }
  ];

  constructor(
    private fb: FormBuilder,
    private toastService: ToastService,
    private apiService: ApiService,
    private systemConfigService: SystemConfigService,
    private router: Router
  ) {
    this.configurationForm = this.createFormGroup();
  }

  ngOnInit() {
    this.loadConfiguration();
    this.loadTemplates();
    this.loadAuditTrail();
    this.setupFormValidation();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private createFormGroup(): FormGroup {
    return this.fb.group({
      general: this.fb.group({
        systemName: ['', [Validators.required, Validators.maxLength(100)]],
        timezone: ['', Validators.required],
        defaultLocale: ['', Validators.required],
        dateFormat: ['', Validators.required],
        timeFormat: ['', Validators.required],
        fiscalYearStart: ['', Validators.required],
        maintenanceWindow: [''],
        systemDescription: ['', Validators.maxLength(500)]
      }),
      security: this.fb.group({
        sessionTimeout: [0, [Validators.required, Validators.min(15), Validators.max(1440)]],
        maxLoginAttempts: [0, [Validators.required, Validators.min(1), Validators.max(10)]],
        passwordMinLength: [0, [Validators.required, Validators.min(8), Validators.max(128)]],
        passwordComplexity: [false],
        twoFactorRequired: [false],
        ssoEnabled: [false],
        apiKeyExpiration: [0, [Validators.required, Validators.min(30), Validators.max(1095)]],
        auditLogRetention: [0, [Validators.required, Validators.min(365)]],
        encryptionAlgorithm: ['', Validators.required]
      }),
      // Additional form groups would be created for other sections...
    });
  }

  private setupFormValidation() {
    this.configurationForm.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        this.validateConfiguration();
        this.checkCriticalChanges();
      });
  }

  loadConfiguration() {
    this.loading = true;
    
    // Subscribe to the service's configuration observable
    this.systemConfigService.configuration$
      .pipe(takeUntil(this.destroy$))
      .subscribe(config => {
        if (config) {
          this.configuration = config;
          this.configurationForm.patchValue(this.configuration);
          this.originalConfiguration = structuredClone(this.configuration);
        }
      });

    // Subscribe to loading state
    this.systemConfigService.loading$
      .pipe(takeUntil(this.destroy$))
      .subscribe(loading => {
        this.loading = loading;
      });

    // Subscribe to error state
    this.systemConfigService.error$
      .pipe(takeUntil(this.destroy$))
      .subscribe(error => {
        if (error) {
          this.toastService.showError(error);
          this.systemConfigService.clearError();
        }
      });

    // Load configuration from service (this will trigger the API call)
    this.systemConfigService.loadConfiguration()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (config) => {
          // Configuration is already handled by the observable subscription above
        },
        error: (error) => {
          // Error is already handled by the error observable subscription above
        }
      });
  }

  saveConfiguration() {
    if (!this.configurationForm.valid) {
      this.toastService.showError('Please fix validation errors before saving');
      return;
    }

    if (this.criticalWarnings.length > 0) {
      const confirmed = confirm(`WARNING: This change may impact system operation:\n\n${this.criticalWarnings.join('\n')}\n\nContinue with save?`);
      if (!confirmed) {
        return;
      }
    }

    this.saving = true;
    const updatedConfiguration = { ...this.configuration, ...this.configurationForm.value };
    
    this.systemConfigService.saveConfiguration(updatedConfiguration, 'Configuration updated via admin UI')
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (savedConfig) => {
          this.saving = false;
          this.criticalWarnings = [];
          this.toastService.showSuccess('System configuration saved successfully');
          this.logConfigurationChange('Configuration updated', 'System Administrator');
        },
        error: (error) => {
          this.saving = false;
          // Error handling is done in the service subscription
        }
      });
  }

  resetConfiguration() {
    if (confirm('Are you sure you want to reset all changes? This will revert to the last saved configuration.')) {
      this.systemConfigService.resetConfiguration()
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (resetConfig) => {
            this.criticalWarnings = [];
            this.validationErrors = {};
            this.toastService.showInfo('Configuration reset to system defaults');
          },
          error: (error) => {
            // Error handling is done in the service subscription
          }
        });
    }
  }

  validateConfiguration() {
    this.validationErrors = {};
    
    // Example validations
    const general = this.configurationForm.get('general');
    const security = this.configurationForm.get('security');
    
    if (general?.get('systemName')?.value?.length < 3) {
      this.addValidationError('general.systemName', 'System name must be at least 3 characters');
    }
    
    if (security?.get('sessionTimeout')?.value < 15) {
      this.addValidationError('security.sessionTimeout', 'Session timeout must be at least 15 minutes for security compliance');
    }
    
    if (security?.get('passwordMinLength')?.value < 12) {
      this.addValidationError('security.passwordMinLength', 'Password minimum length should be at least 12 characters for government systems');
    }
  }

  checkCriticalChanges() {
    this.criticalWarnings = [];
    const currentValues = this.configurationForm.value;
    
    // Check for critical changes that might affect system operation
    if (currentValues.security?.sessionTimeout !== this.originalConfiguration.security.sessionTimeout) {
      this.criticalWarnings.push('Changing session timeout will affect all active user sessions');
    }
    
    if (currentValues.performance?.caching?.enabled !== this.originalConfiguration.performance.caching.enabled) {
      this.criticalWarnings.push('Enabling/disabling cache will require system restart and may affect performance');
    }
    
    if (currentValues.integration?.apiSettings?.rateLimit !== this.originalConfiguration.integration.apiSettings.rateLimit) {
      this.criticalWarnings.push('Changing rate limits may affect existing API integrations');
    }
  }

  private addValidationError(field: string, message: string) {
    if (!this.validationErrors[field]) {
      this.validationErrors[field] = [];
    }
    this.validationErrors[field].push(message);
  }

  setActiveSection(section: string) {
    this.currentSection = section;
  }

  exportConfiguration() {
    const exportData = {
      configuration: this.configuration,
      exportedAt: new Date().toISOString(),
      exportedBy: 'System Administrator',
      version: '1.0.0'
    };
    
    const dataStr = JSON.stringify(exportData, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `cbp-system-config-${new Date().toISOString().split('T')[0]}.json`;
    link.click();
    URL.revokeObjectURL(url);
    
    this.toastService.showSuccess('Configuration exported successfully');
    this.logConfigurationChange('Configuration exported', 'System Administrator');
  }

  importConfiguration(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      const reader = new FileReader();
      
      reader.onload = (e) => {
        try {
          const imported = JSON.parse(e.target?.result as string);
          if (imported.configuration) {
            if (confirm('This will replace the current configuration. Continue?')) {
              this.configuration = imported.configuration;
              this.configurationForm.patchValue(this.configuration);
              this.toastService.showSuccess('Configuration imported successfully');
              this.logConfigurationChange('Configuration imported', 'System Administrator');
            }
          } else {
            throw new Error('Invalid configuration format');
          }
        } catch (error) {
          this.toastService.showError('Invalid configuration file format');
        }
      };
      
      reader.readAsText(file);
    }
  }

  createBackup() {
    const backup = {
      id: Date.now().toString(),
      name: `System Backup ${new Date().toLocaleDateString()}`,
      configuration: structuredClone(this.configuration),
      createdAt: new Date(),
      createdBy: 'System Administrator'
    };
    
    this.backupHistory.unshift(backup);
    this.toastService.showSuccess('Configuration backup created');
    this.logConfigurationChange('Configuration backup created', 'System Administrator');
  }

  restoreBackup(backup: any) {
    if (confirm(`Are you sure you want to restore the backup from ${backup.createdAt.toLocaleDateString()}?`)) {
      this.configuration = structuredClone(backup.configuration);
      this.configurationForm.patchValue(this.configuration);
      this.toastService.showSuccess('Configuration restored from backup');
      this.logConfigurationChange(`Configuration restored from backup: ${backup.name}`, 'System Administrator');
    }
  }

  loadTemplates() {
    this.systemConfigService.getTemplates()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (templates) => {
          this.templates = templates;
        },
        error: (error) => {
          console.error('Failed to load templates:', error);
          // Use fallback templates
          this.templates = [
            {
              id: '1',
              name: 'Development Environment',
              description: 'Configuration template for development environment',
              environment: 'development',
              configuration: {
                security: { 
                  sessionTimeout: 60, 
                  maxLoginAttempts: 3, 
                  passwordMinLength: 8, 
                  passwordComplexity: true, 
                  twoFactorRequired: false, 
                  ssoEnabled: false, 
                  apiKeyExpiration: 365, 
                  ipWhitelist: [], 
                  auditLogRetention: 365, 
                  encryptionAlgorithm: 'AES-256' 
                }
              },
              createdAt: new Date('2024-01-15'),
              createdBy: 'System Administrator',
              tags: ['development', 'testing']
            },
            {
              id: '2',
              name: 'Production Environment',
              description: 'Configuration template for production environment',
              environment: 'production',
              configuration: {
                security: { 
                  sessionTimeout: 480, 
                  maxLoginAttempts: 5, 
                  passwordMinLength: 12, 
                  passwordComplexity: true, 
                  twoFactorRequired: true, 
                  ssoEnabled: true, 
                  apiKeyExpiration: 180, 
                  ipWhitelist: [], 
                  auditLogRetention: 2555, 
                  encryptionAlgorithm: 'AES-256' 
                }
              },
              createdAt: new Date('2024-01-10'),
              createdBy: 'System Administrator',
              tags: ['production', 'security']
            }
          ];
        }
      });
  }

  applyTemplate(template: ConfigurationTemplate) {
    if (confirm(`Apply the "${template.name}" template? This will override current settings.`)) {
      this.systemConfigService.applyTemplate(template.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (updatedConfig) => {
            this.toastService.showSuccess(`Template "${template.name}" applied successfully`);
            this.logConfigurationChange(`Template applied: ${template.name}`, 'System Administrator');
          },
          error: (error) => {
            // Error handling is done in the service subscription
          }
        });
    }
  }

  loadAuditTrail() {
    this.systemConfigService.getChangeHistory(50, 0)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (changes) => {
          this.auditTrail = changes;
        },
        error: (error) => {
          console.error('Failed to load audit trail:', error);
          // Use fallback data
          this.auditTrail = [
            {
              id: '1',
              section: 'Security',
              field: 'sessionTimeout',
              oldValue: 240,
              newValue: 480,
              changedBy: 'System Administrator',
              changedAt: new Date('2024-01-15T10:30:00'),
              status: 'approved',
              approvedBy: 'Security Officer',
              approvedAt: new Date('2024-01-15T11:00:00')
            }
          ];
        }
      });
  }

  private logConfigurationChange(description: string, changedBy: string) {
    const change: ConfigurationChange = {
      id: Date.now().toString(),
      section: 'System',
      field: 'configuration',
      oldValue: 'Previous Configuration',
      newValue: description,
      changedBy,
      changedAt: new Date(),
      status: 'approved'
    };
    
    this.auditTrail.unshift(change);
  }

  searchConfiguration(query: string) {
    this.searchQuery = query.toLowerCase();
  }

  isFieldVisible(fieldPath: string): boolean {
    if (!this.searchQuery) return true;
    return fieldPath.toLowerCase().includes(this.searchQuery);
  }

  hasUnsavedChanges(): boolean {
    return JSON.stringify(this.configurationForm.value) !== JSON.stringify(this.originalConfiguration);
  }

  getValidationErrors(field: string): string[] {
    return this.validationErrors[field] || [];
  }

  // Helper methods for type-safe event handling
  updateNumberValue(path: string[], event: Event): void {
    const target = event.target as HTMLInputElement;
    const value = parseFloat(target.value);
    if (!isNaN(value)) {
      this.setNestedValue(this.configuration, path, value);
    }
  }

  updateStringValue(path: string[], event: Event): void {
    const target = event.target as HTMLInputElement | HTMLSelectElement;
    this.setNestedValue(this.configuration, path, target.value);
  }

  updateBooleanValue(path: string[], event: Event): void {
    const target = event.target as HTMLInputElement;
    this.setNestedValue(this.configuration, path, target.checked);
  }

  private setNestedValue(obj: any, path: string[], value: any): void {
    let current = obj;
    for (let i = 0; i < path.length - 1; i++) {
      current = current[path[i]];
    }
    current[path[path.length - 1]] = value;
  }

  goBack() {
    if (this.hasUnsavedChanges()) {
      if (confirm('You have unsaved changes. Are you sure you want to leave?')) {
        this.router.navigate(['/dashboard']);
      }
    } else {
      this.router.navigate(['/dashboard']);
    }
  }

}