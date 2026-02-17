import { Component, OnInit, Output, EventEmitter, Input, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { filter, takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { Observable } from 'rxjs';
import { FeatureFlagsService, FeatureFlagsConfig } from '../../services/feature-flags.service';
import { ChangeRequestService } from '../../services/change-request.service';
import { OAuthService } from 'angular-oauth2-oidc';
import { ReferenceDataService } from '../../services/reference-data.service';

export interface NavigationItem {
  id: string;
  label: string;
  icon: string;
  path?: string;
  badge?: number;
  type: 'navigation' | 'category';
  permission?: string | string[];
  children?: NavigationItem[];
  expanded?: boolean;
  featureFlag?: string;
}

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.html',
  styleUrls: ['./sidebar.scss'],
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive]
})
export class SidebarComponent implements OnInit, OnDestroy {
  @Input() isCollapsed = false;
  @Output() navigationClick = new EventEmitter<NavigationItem>();
  @Output() toggleSidebar = new EventEmitter<void>();

  private destroy$ = new Subject<void>();
  currentRoute = '';
  expandedSections: Set<string> = new Set(['reference-data']);
  featureFlags$: Observable<FeatureFlagsConfig>;

  // Hierarchical sidebar navigation items
  navigationItems: NavigationItem[] = [
    {
      id: 'dashboard',
      label: 'Dashboard',
      icon: 'insights',
      path: '/dashboard',
      type: 'navigation'
    },
    {
      id: 'reference-data',
      label: 'Reference Data',
      icon: 'folder_open',
      type: 'category',
      expanded: true,
      children: [
        {
          id: 'country-codes',
          label: 'Country Codes',
          icon: 'public',
          path: '/countries',
          type: 'navigation',
          featureFlag: 'countries'
        },
        {
          id: 'port-codes',
          label: 'Port Codes',
          icon: 'location_on',
          path: '/ports',
          type: 'navigation',
          featureFlag: 'ports'
        },
        {
          id: 'airport-codes',
          label: 'Airport Codes',
          icon: 'flight',
          path: '/airports',
          type: 'navigation',
          featureFlag: 'airports'
        }
      ]
    },
    {
      id: 'operations',
      label: 'Operations',
      icon: 'build',
      type: 'category',
      expanded: false,
      permission: ['admin', 'data-steward'],
      children: [
        {
          id: 'change-requests',
          label: 'Change Requests',
          icon: 'edit',
          path: '/change-requests',
          type: 'navigation',
          featureFlag: 'changeRequests'
        },
        {
          id: 'activity-log',
          label: 'Activity Log',
          icon: 'history',
          path: '/activity-log',
          type: 'navigation',
          featureFlag: 'activityLog'
        },
        {
          id: 'import-export',
          label: 'Import/Export',
          icon: 'file_upload',
          path: '/import-export',
          type: 'navigation',
          featureFlag: 'import'
        }
      ]
    },
    {
      id: 'reports-analytics',
      label: 'Reports & Analytics',
      icon: 'assessment',
      type: 'category',
      expanded: false,
      permission: ['admin', 'data-steward'],
      children: [
        {
          id: 'reports',
          label: 'Reports',
          icon: 'report',
          path: '/reports',
          type: 'navigation',
          featureFlag: 'analytics'
        },
        {
          id: 'analytics',
          label: 'Analytics',
          icon: 'trending_up',
          path: '/analytics',
          type: 'navigation',
          featureFlag: 'analytics'
        }
      ]
    },
    {
      id: 'administration',
      label: 'Administration',
      icon: 'security',
      type: 'category',
      permission: 'admin',
      expanded: false,
      children: [
        {
          id: 'settings',
          label: 'Settings',
          icon: 'settings',
          path: '/settings',
          type: 'navigation',
          featureFlag: 'admin'
        },
        {
          id: 'user-management',
          label: 'User Management',
          icon: 'groups',
          path: '/users',
          type: 'navigation',
          featureFlag: 'admin'
        },
        {
          id: 'system-config',
          label: 'System Configuration',
          icon: 'settings',
          path: '/system-config',
          type: 'navigation',
          featureFlag: 'admin'
        }
      ]
    }
  ];

  constructor(
    private router: Router,
    private featureFlagsService: FeatureFlagsService,
    private changeRequestService: ChangeRequestService,
    private oauthService: OAuthService
  ) {
    this.featureFlags$ = this.featureFlagsService.flags$;
  }

  ngOnInit() {

    // Track current route for active states
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        this.currentRoute = event.url;
      });

    // Debug: Log navigation items to console
    console.log('Navigation Items:', this.navigationItems);
    console.log('Expanded Sections:', this.expandedSections);

    // Subscribe to pending change requests count
    this.changeRequestService.pendingRequestsCount$
      .pipe(takeUntil(this.destroy$))
      .subscribe(count => {
        const operationsCategory = this.navigationItems.find(item => item.id === 'operations');
        if (operationsCategory && operationsCategory.children) {
          const changeRequestItem = operationsCategory.children.find(item => item.id === 'change-requests');
          if (changeRequestItem) {
            changeRequestItem.badge = count;
          }
        }
      });
    // Load initial data
    this.changeRequestService.pendingRequestsCount$.pipe(takeUntil(this.destroy$)).subscribe(count => {
      const operationsCategory = this.navigationItems.find(item => item.id === 'operations');
      if (operationsCategory && operationsCategory.children){
        const changeRequestItem = operationsCategory.children.find(item => item.id === 'change-requests');
        if (changeRequestItem) {
          changeRequestItem.badge = count;
        }
      }
    });
  }

  onItemClick(item: NavigationItem) {
    if (item.type === 'category') {
      this.toggleCategory(item.id);
    } else if (item.path) {
      this.navigationClick.emit(item);
      this.router.navigate([item.path]);
    }
  }

  toggleCategory(categoryId: string) {
    if (this.expandedSections.has(categoryId)) {
      this.expandedSections.delete(categoryId);
    } else {
      this.expandedSections.add(categoryId);
    }
    
    // Update the expanded state in the navigation items
    this.updateCategoryExpanded(categoryId);
  }

  private updateCategoryExpanded(categoryId: string) {
    const category = this.navigationItems.find(item => item.id === categoryId);
    if (category) {
      category.expanded = this.expandedSections.has(categoryId);
    }
  }

  isCategoryExpanded(categoryId: string): boolean {
    return this.expandedSections.has(categoryId);
  }

  onToggleSidebar() {
    this.toggleSidebar.emit();
  }

  isItemActive(item: NavigationItem): boolean {
    if (!item.path) return false;
    
    // Exact match for dashboard
    if (item.path === '/dashboard') {
      return this.currentRoute === '/dashboard' || this.currentRoute === '/';
    }
    
    // Starts with match for other routes
    return this.currentRoute.startsWith(item.path);
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  isItemVisible(item: NavigationItem, flags: FeatureFlagsConfig | null): boolean {
    if (!item.featureFlag) {
      return true; // Always show if no feature flag is required
    }
    if (!flags) {
      return false; // Hide if flags are not loaded yet
    }

    const flagName = item.featureFlag;

    // Check in all relevant categories
    if (flags.referenceData.hasOwnProperty(flagName)) {
      return flags.referenceData[flagName];
    }
    if (flags.features.hasOwnProperty(flagName)) {
      return flags.features[flagName];
    }
    if (flags.experimental.hasOwnProperty(flagName)) {
      return flags.experimental[flagName];
    }
    if (flags.dashboard.hasOwnProperty(flagName)) {
      return flags.dashboard[flagName];
    }
    if (flags.admin.hasOwnProperty(flagName)) {
        return flags.admin[flagName];
    }
    
    // If a feature flag is specified but not found in any category, hide the item.
    return false;
  }

  getVisibleChildren(category: NavigationItem, flags: FeatureFlagsConfig | null): NavigationItem[] {
    if (!category.children) return [];
    return category.children.filter(child => this.isItemVisible(child, flags));
  }

  isCategoryVisible(category: NavigationItem, flags: FeatureFlagsConfig | null): boolean {
    if (category.permission && !this.hasRequiredRole(category.permission)) {
      return false;
    }
    if (category.children && this.getVisibleChildren(category, flags).length === 0) {
      return false;
    }
    return true;
  }

  isCategoryActive(category: NavigationItem): boolean {
    if (!category.children) return false;
    return category.children.some(child => this.isItemActive(child));
  }

  hasRequiredRole(role: string | string[]): boolean {
    const claims = this.oauthService.getIdentityClaims();
    if (!claims) {
      return false;
    }

    const realmAccess = (claims as any).realm_access;
    if (realmAccess && realmAccess.roles) {
      if (Array.isArray(role)) {
        return role.some(r => realmAccess.roles.includes(r));
      }
      return realmAccess.roles.includes(role as string);
    }

    return false;
  }
}
