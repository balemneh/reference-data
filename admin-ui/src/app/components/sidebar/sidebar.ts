import { Component, OnInit, Output, EventEmitter, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';

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
}

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.html',
  styleUrls: ['./sidebar.scss'],
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive]
})
export class SidebarComponent implements OnInit {
  @Input() isCollapsed = false;
  @Output() navigationClick = new EventEmitter<NavigationItem>();
  @Output() toggleSidebar = new EventEmitter<void>();

  currentRoute = '';
  expandedSections: Set<string> = new Set(['reference-data']);

  // Hierarchical sidebar navigation items
  navigationItems: NavigationItem[] = [
    {
      id: 'dashboard',
      label: 'Dashboard',
      icon: 'dashboard',
      path: '/dashboard',
      type: 'navigation'
    },
    {
      id: 'reference-data',
      label: 'Reference Data',
      icon: 'dataset',
      type: 'category',
      expanded: true,
      children: [
        {
          id: 'country-codes',
          label: 'Country Codes',
          icon: 'public',
          path: '/countries',
          type: 'navigation'
        },
        {
          id: 'port-codes',
          label: 'Port Codes',
          icon: 'anchor',
          path: '/ports',
          type: 'navigation'
        },
        {
          id: 'airport-codes',
          label: 'Airport Codes',
          icon: 'flight',
          path: '/airports',
          type: 'navigation'
        }
      ]
    },
    {
      id: 'operations',
      label: 'Operations',
      icon: 'construction',
      type: 'category',
      expanded: false,
      children: [
        {
          id: 'change-requests',
          label: 'Change Requests',
          icon: 'assignment',
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
          icon: 'import_export',
          path: '/import-export',
          type: 'navigation'
        }
      ]
    },
    {
      id: 'reports-analytics',
      label: 'Reports & Analytics',
      icon: 'analytics',
      type: 'category',
      expanded: false,
      children: [
        {
          id: 'reports',
          label: 'Reports',
          icon: 'description',
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
      icon: 'admin_panel_settings',
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
          icon: 'group',
          path: '/users',
          type: 'navigation'
        },
        {
          id: 'system-config',
          label: 'System Configuration',
          icon: 'tune',
          path: '/system-config',
          type: 'navigation'
        }
      ]
    }
  ];

  constructor(private router: Router) {}

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
