import { Injectable } from '@angular/core';

/**
 * Simplified ThemeService - only supports CBP light theme
 * Dark mode has been removed per requirements
 */
@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  constructor() {
    // Initialize with CBP light theme colors
    this.initializeTheme();
  }
  
  /**
   * Initialize CBP theme
   */
  private initializeTheme(): void {
    const root = document.documentElement;
    
    // CBP brand colors only
    root.style.setProperty('--theme-primary', '#003366');
    root.style.setProperty('--theme-secondary', '#005a9c');
    root.style.setProperty('--theme-background', '#f8f9fa');
    root.style.setProperty('--theme-surface', '#ffffff');
    root.style.setProperty('--theme-text', '#202124');
    root.style.setProperty('--theme-textSecondary', '#5f6368');
    root.style.setProperty('--theme-border', '#dadce0');
    root.style.setProperty('--theme-accent', '#f39c12');
    
    // Set data-theme attribute for CSS
    root.setAttribute('data-theme', 'light');
    
    // Add theme class
    document.body.classList.add('cbp-theme-light');
  }
}