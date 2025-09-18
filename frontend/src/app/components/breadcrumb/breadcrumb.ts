import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, NavigationEnd, ActivatedRoute, RouterLink } from '@angular/router';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';

export interface BreadcrumbItem {
  label: string;
  url?: string;
  icon?: string;
  isActive?: boolean;
}

@Component({
  selector: 'app-breadcrumb',
  templateUrl: './breadcrumb.html',
  styleUrls: ['./breadcrumb.scss'],
  standalone: true,
  imports: [CommonModule, RouterLink]
})
export class BreadcrumbComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  breadcrumbs: BreadcrumbItem[] = [];

  // Route to breadcrumb mapping
  private routeMap: { [key: string]: BreadcrumbItem } = {
    'dashboard': { label: 'Dashboard', icon: 'dashboard' },
    'countries': { label: 'Countries', icon: 'public' },
    'ports': { label: 'Ports', icon: 'anchor' },
    'airports': { label: 'Airports', icon: 'flight' },
    'change-requests': { label: 'Change Requests', icon: 'assignment' },
    'reports': { label: 'Reports', icon: 'description' },
    'administration': { label: 'Administration', icon: 'settings' },
    'settings': { label: 'Settings', icon: 'settings' },
    'users': { label: 'User Management', icon: 'people' },
    'activity-log': { label: 'Activity Log', icon: 'history' }
  };

  constructor(
    private router: Router,
    private activatedRoute: ActivatedRoute
  ) {}

  ngOnInit() {
    // Listen to route changes
    this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        this.buildBreadcrumbs();
      });

    // Build initial breadcrumbs
    this.buildBreadcrumbs();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private buildBreadcrumbs() {
    const urlSegments = this.router.url.split('/').filter(segment => segment);
    this.breadcrumbs = [];

    // Always start with home
    this.breadcrumbs.push({
      label: 'Home',
      url: '/dashboard',
      icon: 'home'
    });

    // Build breadcrumbs from URL segments
    let currentUrl = '';
    urlSegments.forEach((segment, index) => {
      currentUrl += `/${segment}`;
      
      const breadcrumbItem = this.routeMap[segment];
      if (breadcrumbItem) {
        this.breadcrumbs.push({
          ...breadcrumbItem,
          url: index === urlSegments.length - 1 ? undefined : currentUrl, // No URL for last item
          isActive: index === urlSegments.length - 1
        });
      } else {
        // For dynamic segments (like IDs), use a generic label
        this.breadcrumbs.push({
          label: this.formatSegmentLabel(segment),
          url: index === urlSegments.length - 1 ? undefined : currentUrl,
          isActive: index === urlSegments.length - 1
        });
      }
    });
  }

  private formatSegmentLabel(segment: string): string {
    // Convert kebab-case to Title Case
    return segment
      .split('-')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }

  // Add logic to determine if breadcrumb should be shown
  shouldShowBreadcrumb(): boolean {
    // Always show breadcrumb if current route is not dashboard
    const currentUrl = this.router.url;
    return currentUrl !== '/dashboard' && currentUrl !== '/';
  }

}