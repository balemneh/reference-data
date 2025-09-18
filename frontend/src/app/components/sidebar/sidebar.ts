import { Component, OnInit, Output, EventEmitter, Input, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { filter, takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { FeatureFlagsService, FeatureFlagsConfig } from '../../services/feature-flags.service';

export interface NavigationItem {
  id: string;
  label: string;
  icon: string;
  path?: string;
  badge?: number;
  type: 'navigation' | 'category';
  permission?: string;
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
  featureFlags: FeatureFlagsConfig | null = null;

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
      children: [
        {
          id: 'change-requests',
          label: 'Change Requests',
          icon: 'edit',
          path: '/change-requests',
          type: 'navigation',
          badge: 0
        },
        {
          id: 'activity-log',
          label: 'Activity Log',
          icon: 'history',
          path: '/activity-log',
          type: 'navigation'
        },
        {
          id: 'import-export',
          label: 'Import/Export',
          icon: 'file_upload',
          path: '/import-export',
          type: 'navigation'
        }
      ]
    },
    {
      id: 'reports-analytics',
      label: 'Reports & Analytics',
      icon: 'assessment',
      type: 'category',
      expanded: false,
      children: [
        {
          id: 'reports',
          label: 'Reports',
          icon: 'report',
          path: '/reports',
          type: 'navigation'
        },
        {
          id: 'analytics',
          label: 'Analytics',
          icon: 'trending_up',
          path: '/analytics',
          type: 'navigation'
        }
      ]
    },
    {
      id: 'administration',
      label: 'Administration',
      icon: 'security',
      type: 'category',
      expanded: false,
      children: [
        {
          id: 'settings',
          label: 'Settings',
          icon: 'settings',
          path: '/settings',
          type: 'navigation'
        },
        {
          id: 'user-management',
          label: 'User Management',
          icon: 'groups',
          path: '/users',
          type: 'navigation'
        },
        {
          id: 'system-config',
          label: 'System Configuration',
          icon: 'settings',
          path: '/system-config',
          type: 'navigation'
        }
      ]
    }
  ];

  constructor(
    private router: Router,
    private featureFlagsService: FeatureFlagsService
  ) {}

  ngOnInit() {
    // Subscribe to feature flags
    this.featureFlagsService.flags$
      .pipe(takeUntil(this.destroy$))
      .subscribe(flags => {
        this.featureFlags = flags;
      });

    // Track current route for active states
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        this.currentRoute = event.url;
      });

    // Debug: Log navigation items to console
    console.log('Navigation Items:', this.navigationItems);
    console.log('Expanded Sections:', this.expandedSections);

    // Load initial data
    this.loadPendingRequestsCount();
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

  isItemVisible(item: NavigationItem): boolean {
    if (!item.featureFlag || !this.featureFlags) {
      return true; // Show if no feature flag required or flags not loaded
    }

    // Check if the feature is enabled in reference data
    if ((this.featureFlags.referenceData as any)[item.featureFlag] !== undefined) {
      return (this.featureFlags.referenceData as any)[item.featureFlag];
    }

    // Check if it's a feature flag
    if ((this.featureFlags.features as any)[item.featureFlag] !== undefined) {
      return (this.featureFlags.features as any)[item.featureFlag];
    }

    return true; // Default to showing if flag not found
  }

  getVisibleChildren(category: NavigationItem): NavigationItem[] {
    if (!category.children) return [];
    return category.children.filter(child => this.isItemVisible(child));
  }

  isCategoryVisible(category: NavigationItem): boolean {
    if (!category.children) return true;
    // Hide category if all children are hidden
    return this.getVisibleChildren(category).length > 0;
  }

  isCategoryActive(category: NavigationItem): boolean {
    if (!category.children) return false;
    return category.children.some(child => this.isItemActive(child));
  }

  loadPendingRequestsCount() {
    // This would typically call an API service
    // Find change-requests in the operations category
    const operationsCategory = this.navigationItems.find(item => item.id === 'operations');
    if (operationsCategory && operationsCategory.children) {
      const changeRequestItem = operationsCategory.children.find(item => item.id === 'change-requests');
      if (changeRequestItem) {
        changeRequestItem.badge = 3; // Mock value - replace with API call
      }
    }
  }
}
