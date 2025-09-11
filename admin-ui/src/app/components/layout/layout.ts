import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterOutlet } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { HeaderComponent } from '../header/header';
import { SidebarComponent } from '../sidebar/sidebar';
import { BreadcrumbComponent } from '../breadcrumb/breadcrumb';
import { DashboardComponent } from '../dashboard/dashboard';
import { ShortcutsModalComponent } from '../shortcuts-modal/shortcuts-modal';

@Component({
  selector: 'app-layout',
  templateUrl: './layout.html',
  styleUrls: ['./layout.scss'],
  standalone: true,
  imports: [
    CommonModule, 
    RouterOutlet, 
    HeaderComponent,
    SidebarComponent, 
    BreadcrumbComponent,
    ShortcutsModalComponent
  ]
})
export class LayoutComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  showShortcuts = false;
  sidebarCollapsed = false;
  isMobile = false;
  showMobileBackdrop = false;

  constructor(private router: Router) {
    this.checkMobileView();
  }

  ngOnInit() {
    document.addEventListener('openShortcuts', this.onOpenShortcuts);
    window.addEventListener('resize', this.onWindowResize.bind(this));
    this.checkMobileView();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    document.removeEventListener('openShortcuts', this.onOpenShortcuts);
    window.removeEventListener('resize', this.onWindowResize.bind(this));
  }

  onNavigationClick(item: any) {
    console.log('Navigation item clicked:', item);
    
    // Close mobile sidebar when navigating on mobile
    if (this.isMobile) {
      this.showMobileBackdrop = false;
    }
  }

  onSidebarToggle() {
    if (this.isMobile) {
      this.showMobileBackdrop = !this.showMobileBackdrop;
    } else {
      this.sidebarCollapsed = !this.sidebarCollapsed;
    }
  }

  onMobileBackdropClick() {
    if (this.isMobile) {
      this.showMobileBackdrop = false;
    }
  }

  private checkMobileView() {
    this.isMobile = window.innerWidth <= 768;
    if (!this.isMobile) {
      this.showMobileBackdrop = false;
    }
  }

  private onWindowResize() {
    this.checkMobileView();
  }

  // Shortcuts modal handlers
  onOpenShortcuts = () => {
    this.showShortcuts = true;
  };
  
  onShortcutsClose() {
    this.showShortcuts = false;
  }
}
