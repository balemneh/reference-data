import { Component, OnInit, OnDestroy, HostListener, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { SearchService, SearchResult, SearchSuggestion, GlobalSearchRequest } from '../../services/search.service';
import { Subject, fromEvent } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged, switchMap, startWith } from 'rxjs/operators';

@Component({
  selector: 'app-header',
  templateUrl: './header.html',
  styleUrls: ['./header.scss'],
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule]
})
export class HeaderComponent implements OnInit, OnDestroy {
  @ViewChild('searchInput', { static: false }) searchInputRef!: ElementRef<HTMLInputElement>;
  
  private destroy$ = new Subject<void>();
  private searchSubject = new Subject<string>();
  
  menuOpen = false;
  userMenuOpen = false;
  helpMenuOpen = false;
  notificationMenuOpen = false;
  searchMenuOpen = false;
  settingsMenuOpen = false;
  liveMessage = '';
  searchQuery = '';
  searchResults: SearchResult[] = [];
  searchSuggestions: SearchSuggestion[] = [];
  recentSearches: string[] = [];
  isSearching = false;
  searchResultIndex = -1;
  showRecentSearches = false;
  searchCategories: { [key: string]: number } = {};
  
  currentUser = {
    name: 'John Doe',
    email: 'john.doe@cbp.dhs.gov',
    role: 'Data Curator',
    avatar: '/assets/img/default-avatar.png',
    initials: 'JD'
  };
  
  notifications = [
    {
      id: 1,
      title: 'New country code added',
      message: 'Country code "ZZ" has been added to ISO3166-1',
      time: '2 hours ago',
      read: false,
      type: 'info'
    },
    {
      id: 2,
      title: 'Change request approved',
      message: 'Your request to update United States data has been approved',
      time: '1 day ago',
      read: false,
      type: 'success'
    },
    {
      id: 3,
      title: 'System maintenance scheduled',
      message: 'Planned maintenance window scheduled for Sunday 2AM-4AM EST',
      time: '2 days ago',
      read: true,
      type: 'warning'
    }
  ];
  
  helpItems = [
    { title: 'User Guide', url: '/help/user-guide', icon: 'help_outline' },
    { title: 'API Docs', url: '/help/api-docs', icon: 'code' }
  ];
  
  // Quick access suggestions for empty search
  quickAccessItems = [
    { type: 'page', title: 'Countries', path: '/countries', icon: 'public' },
    { type: 'page', title: 'Ports', path: '/ports', icon: 'anchor' },
    { type: 'page', title: 'Airports', path: '/airports', icon: 'flight' },
    { type: 'page', title: 'Change Requests', path: '/change-requests', icon: 'assignment' },
    { type: 'page', title: 'Reports', path: '/reports', icon: 'description' },
    { type: 'page', title: 'Administration', path: '/administration', icon: 'settings' }
  ];
  
  settingsItems = [
    { title: 'System Settings', path: '/settings', icon: 'settings' },
    { title: 'User Management', path: '/users', icon: 'people' },
    { title: 'Activity Log', path: '/activity-log', icon: 'history' },
    { title: 'Data Import/Export', path: '/data-management', icon: 'import_export' }
  ];

  constructor(
    public router: Router,
    private searchService: SearchService,
    private elementRef: ElementRef
  ) {}

  ngOnInit() {
    // Initialize search functionality
    this.initializeSearch();
    
    // Load search suggestions
    this.loadSearchSuggestions();
    
    // Setup keyboard shortcuts
    this.setupKeyboardShortcuts();
    
    // Load search history
    this.loadSearchHistory();
  }
  
  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
  
  private initializeSearch() {
    // Setup debounced search with proper operators
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(query => {
        if (!query.trim()) {
          this.searchResults = [];
          this.showRecentSearches = true;
          this.isSearching = false;
          return [];
        }
        
        this.isSearching = true;
        this.showRecentSearches = false;
        
        const searchRequest: GlobalSearchRequest = {
          query: query.trim(),
          limit: 20,
          fuzzySearch: true
        };
        
        return this.searchService.searchGlobal(searchRequest);
      }),
      takeUntil(this.destroy$)
    ).subscribe({
      next: (response) => {
        if (response && response.results) {
          this.searchResults = response.results;
          this.searchCategories = response.categories;
        }
        this.isSearching = false;
        this.resetSearchNavigation();
      },
      error: (error) => {
        console.error('Search failed:', error);
        this.searchResults = [];
        this.isSearching = false;
      }
    });
  }

  toggleMenu() {
    this.menuOpen = !this.menuOpen;
    if (this.menuOpen) {
      this.userMenuOpen = false;
    }
  }

  toggleUserMenu() {
    this.userMenuOpen = !this.userMenuOpen;
    if (this.userMenuOpen) {
      this.menuOpen = false;
      this.helpMenuOpen = false;
      this.notificationMenuOpen = false;
    }
    this.announce(this.userMenuOpen ? 'Account menu opened' : 'Account menu closed');
  }

  toggleHelpMenu() {
    this.helpMenuOpen = !this.helpMenuOpen;
    if (this.helpMenuOpen) {
      this.menuOpen = false;
      this.userMenuOpen = false;
      this.notificationMenuOpen = false;
    }
    this.announce(this.helpMenuOpen ? 'Help menu opened' : 'Help menu closed');
  }

  toggleNotificationMenu() {
    this.notificationMenuOpen = !this.notificationMenuOpen;
    if (this.notificationMenuOpen) {
      this.menuOpen = false;
      this.userMenuOpen = false;
      this.helpMenuOpen = false;
      this.searchMenuOpen = false;
      this.settingsMenuOpen = false;
    }
    this.announce(this.notificationMenuOpen ? 'Notifications menu opened' : 'Notifications menu closed');
  }
  
  toggleSearchMenu() {
    this.searchMenuOpen = !this.searchMenuOpen;
    if (this.searchMenuOpen) {
      this.menuOpen = false;
      this.userMenuOpen = false;
      this.helpMenuOpen = false;
      this.notificationMenuOpen = false;
      this.settingsMenuOpen = false;
      // Focus search input after menu opens
      setTimeout(() => {
        const searchInput = document.getElementById('header-search-input');
        if (searchInput) {
          searchInput.focus();
        }
      }, 100);
    }
    this.announce(this.searchMenuOpen ? 'Search menu opened' : 'Search menu closed');
  }
  
  toggleSettingsMenu() {
    this.settingsMenuOpen = !this.settingsMenuOpen;
    if (this.settingsMenuOpen) {
      this.menuOpen = false;
      this.userMenuOpen = false;
      this.helpMenuOpen = false;
      this.notificationMenuOpen = false;
      this.searchMenuOpen = false;
    }
    this.announce(this.settingsMenuOpen ? 'Settings menu opened' : 'Settings menu closed');
  }

  closeMenus() {
    this.menuOpen = false;
    this.userMenuOpen = false;
    this.helpMenuOpen = false;
    this.notificationMenuOpen = false;
    this.searchMenuOpen = false;
    this.settingsMenuOpen = false;
    this.announce('Menus closed');
  }

  logout() {
    // Clear all stored authentication data
    localStorage.removeItem('cbp-auth-token');
    localStorage.removeItem('cbp-refresh-token');
    localStorage.removeItem('cbp-user-data');
    sessionStorage.clear();
    
    // Clear any app-specific data
    localStorage.removeItem('cbp-user-preferences');
    
    // Close all menus
    this.closeMenus();
    
    // Show confirmation
    console.log('User logged out successfully');
    
    // In a real app, you would redirect to login page
    // For now, we'll reload the page to simulate logout
    alert('You have been signed out successfully.');
    window.location.reload();
  }

  isActive(path: string): boolean {
    return this.router.url === path;
  }

  get unreadNotificationCount(): number {
    return this.notifications.filter(n => !n.read).length;
  }

  markNotificationAsRead(notificationId: number) {
    const notification = this.notifications.find(n => n.id === notificationId);
    if (notification) {
      notification.read = true;
    }
  }

  markAllNotificationsAsRead() {
    this.notifications.forEach(n => n.read = true);
  }

  markAllAsRead() {
    this.markAllNotificationsAsRead();
  }
  
  clearAllNotifications() {
    this.notifications = [];
  }
  
  onNotificationClick(notification: any) {
    this.markNotificationAsRead(notification.id);
    // Navigate to relevant page if needed
    if (notification.url) {
      this.router.navigate([notification.url]);
    }
    this.toggleNotificationMenu();
  }

  openHelpItem(item: any) {
    if (item.url === '#') {
      console.log(`Opening help item: ${item.title}`);
      // Implement help item functionality
    } else {
      window.open(item.url, '_blank');
    }
    this.helpMenuOpen = false;
  }

  onSearch() {
    this.searchSubject.next(this.searchQuery);
  }
  
  onSearchResultClick(result: SearchResult | any) {
    // Handle both new SearchResult objects and legacy suggestion objects
    const path = result.path;
    const title = result.title;
    
    if (path) {
      this.router.navigate([path]);
      
      // Add to recent searches if it's an actual search result
      if ('type' in result && result.type !== 'page') {
        this.addToRecentSearches(title);
      }
    }
    
    this.clearSearch();
    this.closeMenus();
  }
  
  onSearchInputFocus() {
    if (!this.searchMenuOpen) {
      this.toggleSearchMenu();
    }
    this.showRecentSearches = !this.searchQuery.trim();
  }
  
  
  openSettings(item: any) {
    this.router.navigate([item.path]);
    this.settingsMenuOpen = false;
  }
  
  private announce(message: string) {
    this.liveMessage = '';
    // flush to ensure SR picks up change
    setTimeout(() => (this.liveMessage = message), 0);
  }
  
  // Enhanced search methods
  
  private loadSearchSuggestions() {
    this.searchService.getSearchSuggestions()
      .pipe(takeUntil(this.destroy$))
      .subscribe(suggestions => {
        this.searchSuggestions = suggestions;
      });
  }
  
  private loadSearchHistory() {
    this.searchService.searchHistory$
      .pipe(takeUntil(this.destroy$))
      .subscribe(history => {
        this.recentSearches = history.map(h => h.query).slice(0, 5);
      });
  }
  
  private setupKeyboardShortcuts() {
    // Global keyboard shortcut for search (Cmd/Ctrl + K)
    fromEvent<KeyboardEvent>(document, 'keydown')
      .pipe(takeUntil(this.destroy$))
      .subscribe(event => {
        // Cmd/Ctrl + K to focus search
        if ((event.metaKey || event.ctrlKey) && event.key === 'k') {
          event.preventDefault();
          this.focusSearch();
        }
        
        // ESC to close search
        if (event.key === 'Escape' && this.searchMenuOpen) {
          event.preventDefault();
          this.clearSearch();
          this.closeMenus();
        }
      });
  }
  
  @HostListener('keydown', ['$event'])
  onKeyDown(event: KeyboardEvent) {
    if (!this.searchMenuOpen) return;
    
    const results = this.getDisplayedResults();
    
    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        this.searchResultIndex = Math.min(this.searchResultIndex + 1, results.length - 1);
        this.announce(`Result ${this.searchResultIndex + 1} of ${results.length}: ${results[this.searchResultIndex]?.title}`);
        break;
        
      case 'ArrowUp':
        event.preventDefault();
        this.searchResultIndex = Math.max(this.searchResultIndex - 1, -1);
        if (this.searchResultIndex === -1) {
          this.announce('Back to search input');
        } else {
          this.announce(`Result ${this.searchResultIndex + 1} of ${results.length}: ${results[this.searchResultIndex]?.title}`);
        }
        break;
        
      case 'Enter':
        event.preventDefault();
        if (this.searchResultIndex >= 0 && results[this.searchResultIndex]) {
          this.onSearchResultClick(results[this.searchResultIndex]);
        } else if (this.searchQuery.trim()) {
          // Navigate to full search results page
          this.router.navigate(['/search'], { queryParams: { q: this.searchQuery.trim() } });
          this.clearSearch();
          this.closeMenus();
        }
        break;
    }
  }
  
  private getDisplayedResults(): (SearchResult | any)[] {
    if (this.searchQuery.trim()) {
      return this.searchResults;
    } else {
      return [
        ...this.recentSearches.map(query => ({
          id: `recent_${query}`,
          title: query,
          path: `/search?q=${encodeURIComponent(query)}`,
          icon: 'history',
          type: 'recent'
        })),
        ...this.quickAccessItems.slice(0, 5)
      ];
    }
  }
  
  private resetSearchNavigation() {
    this.searchResultIndex = -1;
  }
  
  focusSearch() {
    if (!this.searchMenuOpen) {
      this.toggleSearchMenu();
    }
    
    // Focus search input after menu opens
    setTimeout(() => {
      const searchInput = document.getElementById('header-search-input') as HTMLInputElement;
      if (searchInput) {
        searchInput.focus();
        searchInput.select();
      }
    }, 100);
  }
  
  clearSearch() {
    this.searchQuery = '';
    this.searchResults = [];
    this.resetSearchNavigation();
    this.showRecentSearches = true;
  }
  
  private addToRecentSearches(query: string) {
    if (!query.trim()) return;
    
    // Add to search service history (this will also update our local recentSearches)
    // Note: The service will handle the actual search execution and history management
  }
  
  removeFromRecentSearches(query: string) {
    this.searchService.removeFromHistory(query);
  }
  
  clearSearchHistory() {
    this.searchService.clearSearchHistory();
  }
  
  getResultsByCategory(category: string): SearchResult[] {
    return this.searchResults.filter(result => result.category === category);
  }
  
  getCategoryDisplayName(category: string): string {
    const categoryMap: { [key: string]: string } = {
      'reference_data': 'Reference Data',
      'workflow': 'Workflows',
      'administration': 'Administration',
      'help': 'Help & Documentation',
      'navigation': 'Pages'
    };
    return categoryMap[category] || category;
  }
  
  getCategoryIcon(category: string): string {
    const categoryIcons: { [key: string]: string } = {
      'reference_data': 'database',
      'workflow': 'assignment',
      'administration': 'admin_panel_settings',
      'help': 'help_outline',
      'navigation': 'menu'
    };
    return categoryIcons[category] || 'folder';
  }
  
  getSearchPlaceholder(): string {
    return 'Search countries, ports, change requests...';
  }
  
  onSearchInputKeydown(event: KeyboardEvent) {
    // Handle special keys in search input
    if (event.key === 'Escape') {
      this.clearSearch();
      this.closeMenus();
    }
  }
  
  highlightSearchText(text: string, query: string): string {
    if (!query.trim()) return text;
    
    const regex = new RegExp(`(${this.escapeRegex(query)})`, 'gi');
    return text.replace(regex, '<mark>$1</mark>');
  }
  
  private escapeRegex(string: string): string {
    return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  }
  
  // Helper methods for template
  
  trackBySearchResult(index: number, result: SearchResult): string {
    return result.id;
  }
  
  getGlobalResultIndex(result: SearchResult): number {
    // Find the index of this result in the displayed results array
    const displayedResults = this.getDisplayedResults();
    return displayedResults.findIndex(r => r.id === result.id);
  }
  
  // Expose Object.keys to template
  get Object() {
    return Object;
  }
}