import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of, forkJoin } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface FeatureFlag {
  id: string;
  name: string;
  description: string;
  enabled: boolean;
  category: 'reference-data' | 'features' | 'experimental';
  dependencies?: string[];
}

export interface FeatureFlagsConfig {
  referenceData: {
    [key: string]: boolean;
    countries: boolean;
    ports: boolean;
    airports: boolean;
    carriers: boolean;
    units: boolean;
    languages: boolean;
  };
  features: {
    [key: string]: boolean;
    changeRequests: boolean;
    analytics: boolean;
    export: boolean;
    import: boolean;
    bulkOperations: boolean;
    advancedSearch: boolean;
    apiAccess: boolean;
    webhooks: boolean;
    activityLog: boolean;
  };
  experimental: {
    [key: string]: boolean;
    aiAssistant: boolean;
    graphView: boolean;
    realtimeSync: boolean;
    collaboration: boolean;
  };
  dashboard: {
    [key: string]: boolean;
    // Only non-data related dashboard features
    // Data visibility is controlled by referenceData flags
    showRecentActivity: boolean;
    showSystemHealth: boolean;
  };
  admin: {
    [key: string]: boolean;
    admin: boolean;
  };
}

@Injectable({
  providedIn: 'root'
})
export class FeatureFlagsService {
  private readonly STORAGE_KEY = 'cbp-feature-flags';
  private apiUrl = `${environment.apiUrl}/api/v1/system-config`;

  private defaultFlags: FeatureFlagsConfig = {
    referenceData: {
      countries: true,
      ports: true,
      airports: true,
      carriers: true,
      units: false,
      languages: false
    },
    features: {
      changeRequests: true,
      analytics: true,
      export: true,
      import: true,
      bulkOperations: true,
      advancedSearch: true,
      apiAccess: true,
      webhooks: false,
      activityLog: true
    },
    experimental: {
      aiAssistant: false,
      graphView: false,
      realtimeSync: false,
      collaboration: false
    },
    dashboard: {
      // Only non-data related dashboard features
      showRecentActivity: true,
      showSystemHealth: true
    },
    admin: {
      admin: true
    }
  };

  private flags = new BehaviorSubject<FeatureFlagsConfig>(this.defaultFlags);
  public flags$ = this.flags.asObservable();

  constructor(private http: HttpClient) {
    this.loadFlags();
  }

  loadFlags() {
    // Load from backend API
    this.http.get<FeatureFlagsConfig>(`${this.apiUrl}/feature-flags`).pipe(
      tap(response => {
        const flags: FeatureFlagsConfig = {
          referenceData: { ...this.defaultFlags.referenceData, ...response.referenceData },
          features: { ...this.defaultFlags.features, ...response.features },
          experimental: { ...this.defaultFlags.experimental, ...response.experimental },
          dashboard: { ...this.defaultFlags.dashboard, ...response.dashboard },
          admin: { ...this.defaultFlags.admin, ...response.admin }
        };

        this.flags.next(flags);
        // Cache in localStorage as fallback
        this.saveFlags(flags);
      }),
      catchError(error => {
        console.error('Failed to load feature flags from server', error);
        // Fall back to localStorage
        const stored = localStorage.getItem(this.STORAGE_KEY);
        if (stored) {
          try {
            const parsed = JSON.parse(stored);
            this.flags.next(parsed);
          } catch (e) {
            console.error('Failed to parse stored feature flags', e);
            // Fall back to defaults if stored flags are invalid
            this.flags.next(this.defaultFlags);
          }
        } else {
          // Fall back to defaults if nothing is stored
          this.flags.next(this.defaultFlags);
        }
        // Re-throw the error to be caught by global error handlers
        throw error;
      })
    ).subscribe();
  }

  getFlags(): FeatureFlagsConfig {
    return this.flags.getValue();
  }

  isEnabled(category: keyof FeatureFlagsConfig, flag: string): boolean {
    const flags = this.flags.getValue();
    return flags[category]?.[flag] ?? false;
  }

  getAllFlags(): FeatureFlag[] {
    const flags = this.flags.getValue();
    const allFlags: FeatureFlag[] = [];

    // Convert to flat list
    Object.entries(flags.referenceData).forEach(([key, value]) => {
      allFlags.push({
        id: key,
        name: this.formatFlagName(key),
        description: `Reference data for ${key}`,
        enabled: value as boolean,
        category: 'reference-data'
      });
    });

    Object.entries(flags.features).forEach(([key, value]) => {
      allFlags.push({
        id: key,
        name: this.formatFlagName(key),
        description: `Feature: ${key}`,
        enabled: value as boolean,
        category: 'features'
      });
    });

    Object.entries(flags.experimental).forEach(([key, value]) => {
      allFlags.push({
        id: key,
        name: this.formatFlagName(key),
        description: `Experimental: ${key}`,
        enabled: value as boolean,
        category: 'experimental'
      });
    });

    return allFlags;
  }

  updateFlag(category: keyof FeatureFlagsConfig, flag: string, enabled: boolean): Observable<any> {
    const featureId = flag;
    const primaryUpdate = this.http.put(`${this.apiUrl}/feature-flags/${featureId}`, { enabled });

    const updates: Observable<any>[] = [primaryUpdate];

    // If activityLog is being updated, also update showRecentActivity
    if (flag === 'activityLog') {
      const secondaryUpdate = this.http.put(`${this.apiUrl}/feature-flags/showRecentActivity`, { enabled });
      updates.push(secondaryUpdate);
    }

    return forkJoin(updates).pipe(
      tap(() => {
        const current = this.flags.getValue();
        const updated = { ...current };

        if (updated[category]) {
          updated[category][flag] = enabled;
          if (flag === 'activityLog') {
            updated.dashboard.showRecentActivity = enabled;
          }
        }

        this.flags.next(updated);
        this.saveFlags(updated);
      }),
      catchError(error => {
        console.error('Failed to update feature flag on server', error);
        // Re-throw the error to be caught by the component
        throw error;
      })
    );
  }

  bulkUpdate(category: keyof FeatureFlagsConfig, updates: Record<string, boolean>) {
    // Update on backend
    this.http.post(`${this.apiUrl}/feature-flags/bulk-update/${category}`, updates).pipe(
      tap(() => {
        // Update local state
        const current = this.flags.getValue();
        const updated = { ...current };

        if (updated[category]) {
          (updated[category] as any) = { ...updated[category], ...updates };
          this.flags.next(updated);
          this.saveFlags(updated);
        }
      }),
      catchError(error => {
        console.error('Failed to bulk update feature flags on server', error);
        // Still update locally
        const current = this.flags.getValue();
        const updated = { ...current };

        if (updated[category]) {
          (updated[category] as any) = { ...updated[category], ...updates };
          this.flags.next(updated);
          this.saveFlags(updated);
        }
        return of(null);
      })
    ).subscribe();
  }

  private saveFlags(flags: FeatureFlagsConfig) {
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(flags));
  }

  resetToDefaults() {
    // Reset on backend
    this.http.post(`${this.apiUrl}/reset`, {}).pipe(
      tap(() => {
        // Reset local state
        this.flags.next(this.defaultFlags);
        this.saveFlags(this.defaultFlags);
      }),
      catchError(error => {
        console.error('Failed to reset feature flags on server', error);
        // Still reset locally
        this.flags.next(this.defaultFlags);
        this.saveFlags(this.defaultFlags);
        return of(null);
      })
    ).subscribe();
  }

  exportConfig(): string {
    return JSON.stringify(this.flags.getValue(), null, 2);
  }

  importConfig(jsonString: string): boolean {
    try {
      const config = JSON.parse(jsonString) as FeatureFlagsConfig;

      // Validate structure
      if (!config.referenceData || !config.features ||
          !config.experimental || !config.dashboard || !config.admin) {
        return false;
      }

      // Import on backend
      this.http.post(`${this.apiUrl}/import`, config).pipe(
        tap(() => {
          // Update local state
          this.flags.next(config);
          this.saveFlags(config);
        }),
        catchError(error => {
          console.error('Failed to import config on server', error);
          // Still import locally
          this.flags.next(config);
          this.saveFlags(config);
          return of(null);
        })
      ).subscribe();

      return true;
    } catch (e) {
      console.error('Failed to import config', e);
      return false;
    }
  }

  private formatFlagName(flag: string): string {
    return flag
      .replace(/([A-Z])/g, ' $1')
      .replace(/^./, str => str.toUpperCase())
      .trim();
  }

  // Dashboard helpers
  getDashboardFlags(): FeatureFlagsConfig['dashboard'] {
    return this.flags.getValue().dashboard;
  }

  // Reference data helpers
  getReferenceDataFlags(): FeatureFlagsConfig['referenceData'] {
    return this.flags.getValue().referenceData;
  }

  // Feature helpers
  getFeatureFlags(): FeatureFlagsConfig['features'] {
    return this.flags.getValue().features;
  }

  // Experimental helpers
  getExperimentalFlags(): FeatureFlagsConfig['experimental'] {
    return this.flags.getValue().experimental;
  }
}