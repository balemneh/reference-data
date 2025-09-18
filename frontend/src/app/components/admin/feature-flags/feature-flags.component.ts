import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FeatureFlagsService, FeatureFlag } from '../../../services/feature-flags.service';
import { ToastService } from '../../../services/toast.service';

@Component({
  selector: 'app-feature-flags',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './feature-flags.component.html',
  styleUrls: ['./feature-flags.component.scss']
})
export class FeatureFlagsComponent implements OnInit {
  referenceDataFlags: any = {};
  featuresFlags: any = {};
  experimentalFlags: any = {};
  dashboardFlags: any = {};

  activeTab: 'reference-data' | 'features' | 'dashboard' | 'experimental' = 'reference-data';
  searchTerm = '';
  showImportExport = false;
  configJson = '';

  constructor(
    private featureFlagsService: FeatureFlagsService,
    private toastService: ToastService
  ) {}

  ngOnInit() {
    this.loadFlags();

    // Subscribe to flag changes
    this.featureFlagsService.flags$.subscribe(flags => {
      this.referenceDataFlags = { ...flags.referenceData };
      this.featuresFlags = { ...flags.features };
      this.experimentalFlags = { ...flags.experimental };
      this.dashboardFlags = { ...flags.dashboard };
    });
  }

  loadFlags() {
    const flags = this.featureFlagsService.getFlags();
    this.referenceDataFlags = { ...flags.referenceData };
    this.featuresFlags = { ...flags.features };
    this.experimentalFlags = { ...flags.experimental };
    this.dashboardFlags = { ...flags.dashboard };
  }

  updateFlag(category: string, flag: string, event: Event) {
    const checkbox = event.target as HTMLInputElement;
    const enabled = checkbox.checked;

    this.featureFlagsService.updateFlag(category as any, flag, enabled);

    this.toastService.showSuccess(
      'Feature Flag Updated',
      `${this.formatFlagName(flag)} has been ${enabled ? 'enabled' : 'disabled'}`
    );

    // Handle dependencies
    this.handleDependencies(category, flag, enabled);
  }

  handleDependencies(category: string, flag: string, enabled: boolean) {
    // If disabling a reference data type, also disable its dashboard card
    if (category === 'referenceData' && !enabled) {
      const dashboardKey = `show${flag.charAt(0).toUpperCase()}${flag.slice(1)}`;
      if ((this.dashboardFlags as any)[dashboardKey] !== undefined) {
        this.featureFlagsService.updateFlag('dashboard', dashboardKey, false);
      }
    }

    // If enabling a dashboard card, ensure the reference data is also enabled
    if (category === 'dashboard' && enabled) {
      const refDataKey = flag.replace('show', '').toLowerCase();
      if ((this.referenceDataFlags as any)[refDataKey] !== undefined && !(this.referenceDataFlags as any)[refDataKey]) {
        this.featureFlagsService.updateFlag('referenceData', refDataKey, true);
        this.toastService.showInfo(
          'Dependency Enabled',
          `${this.formatFlagName(refDataKey)} reference data has been enabled as a dependency`
        );
      }
    }
  }

  formatFlagName(flag: string): string {
    return flag
      .replace(/([A-Z])/g, ' $1')
      .replace(/^show/, '')
      .replace(/^./, str => str.toUpperCase())
      .trim();
  }

  resetToDefaults() {
    if (confirm('Are you sure you want to reset all feature flags to their default values?')) {
      this.featureFlagsService.resetToDefaults();
      this.loadFlags();
      this.toastService.showSuccess('Reset Complete', 'All feature flags have been reset to defaults');
    }
  }

  exportConfig() {
    this.configJson = this.featureFlagsService.exportConfig();
    this.showImportExport = true;
    this.toastService.showSuccess('Config Exported', 'Feature flags configuration has been exported');
  }

  importConfig() {
    if (!this.configJson.trim()) {
      this.toastService.showError('Import Failed', 'Please paste a valid configuration JSON');
      return;
    }

    if (this.featureFlagsService.importConfig(this.configJson)) {
      this.loadFlags();
      this.showImportExport = false;
      this.configJson = '';
      this.toastService.showSuccess('Config Imported', 'Feature flags configuration has been imported successfully');
    } else {
      this.toastService.showError('Import Failed', 'Invalid configuration JSON format');
    }
  }

  copyConfig() {
    navigator.clipboard.writeText(this.configJson).then(() => {
      this.toastService.showSuccess('Copied', 'Configuration copied to clipboard');
    });
  }

  getFilteredFlags(flags: any): string[] {
    if (!this.searchTerm) {
      return Object.keys(flags);
    }

    return Object.keys(flags).filter(key =>
      this.formatFlagName(key).toLowerCase().includes(this.searchTerm.toLowerCase())
    );
  }

  setActiveTab(tab: 'reference-data' | 'features' | 'dashboard' | 'experimental') {
    this.activeTab = tab;
  }

  toggleAll(category: string, enabled: boolean) {
    let flags: any;
    switch (category) {
      case 'referenceData':
        flags = this.referenceDataFlags;
        break;
      case 'features':
        flags = this.featuresFlags;
        break;
      case 'experimental':
        flags = this.experimentalFlags;
        break;
      case 'dashboard':
        flags = this.dashboardFlags;
        break;
      default:
        return;
    }

    Object.keys(flags).forEach(flag => {
      this.featureFlagsService.updateFlag(category as any, flag, enabled);
    });

    this.loadFlags();
    this.toastService.showSuccess(
      'Bulk Update',
      `All ${category} flags have been ${enabled ? 'enabled' : 'disabled'}`
    );
  }

  getEnabledCount(flags: any): number {
    return Object.values(flags).filter(v => v === true).length;
  }

  getTotalCount(flags: any): number {
    return Object.keys(flags).length;
  }
}