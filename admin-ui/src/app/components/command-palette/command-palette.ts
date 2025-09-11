import { Component, OnInit, OnDestroy, HostListener, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { trigger, state, style, transition, animate } from '@angular/animations';
import { Subject, BehaviorSubject } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { CommandPaletteService } from '../../services/command-palette.service';

export interface Command {
  id: string;
  title: string;
  description?: string;
  keywords: string[];
  icon?: string;
  action: () => void;
  category: 'navigation' | 'action' | 'data' | 'setting';
  shortcut?: string;
}

@Component({
  selector: 'app-command-palette',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="cbp-command-palette-overlay" 
         *ngIf="isOpen" 
         (click)="close()"
         [@fadeIn]>
      
      <div class="cbp-command-palette" 
           (click)="$event.stopPropagation()"
           [@slideIn]
           role="dialog"
           aria-modal="true"
           aria-labelledby="command-palette-title">
        
        <div class="cbp-command-palette__header">
          <div class="cbp-command-palette__search">
            <svg class="cbp-command-palette__search-icon" aria-hidden="true">
              <use xlink:href="/assets/uswds/img/sprite.svg#search"></use>
            </svg>
            <input
              #searchInput
              class="cbp-command-palette__input"
              type="text"
              [(ngModel)]="searchQuery"
              (ngModelChange)="onSearchChange($event)"
              placeholder="Type a command or search..."
              aria-label="Command search"
              autocomplete="off"
              spellcheck="false"
            />
            <div class="cbp-command-palette__shortcut-hint">
              <kbd>Esc</kbd>
            </div>
          </div>
        </div>
        
        <div class="cbp-command-palette__body">
          <div class="cbp-command-palette__results" *ngIf="filteredCommands.length > 0">
            <div *ngFor="let category of getCategories()" class="cbp-command-palette__category">
              <div class="cbp-command-palette__category-title" *ngIf="getCategoryCommands(category).length > 0">
                {{ getCategoryLabel(category) }}
              </div>
              
              <div class="cbp-command-palette__commands">
                <button
                  *ngFor="let command of getCategoryCommands(category); let i = index"
                  class="cbp-command-palette__command"
                  [class.cbp-command-palette__command--selected]="isSelected(command)"
                  (click)="executeCommand(command)"
                  (mouseenter)="setSelected(command)"
                  type="button"
                >
                  <div class="cbp-command-palette__command-icon" *ngIf="command.icon">
                    <svg class="cbp-command-icon" aria-hidden="true">
                      <use [attr.xlink:href]="'/assets/uswds/img/sprite.svg#' + command.icon"></use>
                    </svg>
                  </div>
                  
                  <div class="cbp-command-palette__command-content">
                    <div class="cbp-command-palette__command-title">
                      {{ command.title }}
                    </div>
                    <div class="cbp-command-palette__command-description" *ngIf="command.description">
                      {{ command.description }}
                    </div>
                  </div>
                  
                  <div class="cbp-command-palette__command-shortcut" *ngIf="command.shortcut">
                    <kbd>{{ command.shortcut }}</kbd>
                  </div>
                </button>
              </div>
            </div>
          </div>
          
          <div class="cbp-command-palette__empty" *ngIf="filteredCommands.length === 0">
            <div class="cbp-command-palette__empty-icon">
              <svg class="cbp-empty-icon" aria-hidden="true">
                <use xlink:href="/assets/uswds/img/sprite.svg#search_off"></use>
              </svg>
            </div>
            <div class="cbp-command-palette__empty-title">No commands found</div>
            <div class="cbp-command-palette__empty-description">
              Try a different search term or browse available commands
            </div>
          </div>
        </div>
        
        <div class="cbp-command-palette__footer">
          <div class="cbp-command-palette__shortcuts">
            <div class="cbp-shortcut-hint">
              <kbd>↑</kbd><kbd>↓</kbd> Navigate
            </div>
            <div class="cbp-shortcut-hint">
              <kbd>Enter</kbd> Execute
            </div>
            <div class="cbp-shortcut-hint">
              <kbd>Esc</kbd> Close
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styleUrls: ['./command-palette.scss'],
  animations: [
    trigger('fadeIn', [
      state('in', style({ opacity: 1 })),
      transition('void => *', [
        style({ opacity: 0 }),
        animate('200ms ease-out')
      ]),
      transition('* => void', [
        animate('150ms ease-in', style({ opacity: 0 }))
      ])
    ]),
    trigger('slideIn', [
      state('in', style({ opacity: 1, transform: 'translateY(0) scale(1)' })),
      transition('void => *', [
        style({ opacity: 0, transform: 'translateY(-20px) scale(0.95)' }),
        animate('200ms cubic-bezier(0.25, 0.8, 0.25, 1)')
      ]),
      transition('* => void', [
        animate('150ms cubic-bezier(0.25, 0.8, 0.25, 1)', 
          style({ opacity: 0, transform: 'translateY(-20px) scale(0.95)' })
        )
      ])
    ])
  ]
})
export class CommandPaletteComponent implements OnInit, OnDestroy {
  @ViewChild('searchInput', { static: false }) searchInput!: ElementRef<HTMLInputElement>;
  
  private destroy$ = new Subject<void>();
  private searchSubject = new BehaviorSubject<string>('');
  
  isOpen = false;
  searchQuery = '';
  filteredCommands: Command[] = [];
  selectedCommandIndex = 0;
  selectedCommand: Command | null = null;
  
  private allCommands: Command[] = [
    // Navigation commands
    {
      id: 'nav-dashboard',
      title: 'Go to Dashboard',
      description: 'View system overview and metrics',
      keywords: ['dashboard', 'home', 'overview', 'metrics'],
      icon: 'dashboard',
      action: () => this.router.navigate(['/dashboard']),
      category: 'navigation'
    },
    {
      id: 'nav-countries',
      title: 'Go to Countries',
      description: 'Manage country reference data',
      keywords: ['countries', 'nations', 'iso'],
      icon: 'public',
      action: () => this.router.navigate(['/countries']),
      category: 'navigation'
    },
    {
      id: 'nav-change-requests',
      title: 'Go to Change Requests',
      description: 'Review pending data changes',
      keywords: ['changes', 'requests', 'approval', 'review'],
      icon: 'assignment',
      action: () => this.router.navigate(['/change-requests']),
      category: 'navigation'
    },
    
    // Action commands
    {
      id: 'action-add-country',
      title: 'Add New Country',
      description: 'Create a new country record',
      keywords: ['add', 'new', 'country', 'create'],
      icon: 'add_circle',
      action: () => this.router.navigate(['/countries', 'new']),
      category: 'action',
      shortcut: 'Ctrl+N'
    },
    {
      id: 'action-refresh-data',
      title: 'Refresh Dashboard Data',
      description: 'Reload all dashboard metrics',
      keywords: ['refresh', 'reload', 'update'],
      icon: 'refresh',
      action: () => window.location.reload(),
      category: 'action',
      shortcut: 'Ctrl+R'
    },
    {
      id: 'action-export-data',
      title: 'Export Data',
      description: 'Download reference data exports',
      keywords: ['export', 'download', 'data'],
      icon: 'download',
      action: () => this.router.navigate(['/export']),
      category: 'action'
    },
    
    // Data commands
    {
      id: 'data-search',
      title: 'Search Data',
      description: 'Search across all reference data',
      keywords: ['search', 'find', 'lookup'],
      icon: 'search',
      action: () => this.router.navigate(['/search']),
      category: 'data',
      shortcut: 'Ctrl+F'
    },
    {
      id: 'data-quality',
      title: 'Data Quality Report',
      description: 'View data quality metrics',
      keywords: ['quality', 'validation', 'report'],
      icon: 'assessment',
      action: () => this.router.navigate(['/quality']),
      category: 'data'
    },
    
    // Settings commands removed theme toggle
    {
      id: 'setting-profile',
      title: 'Profile Settings',
      description: 'Manage your profile and preferences',
      keywords: ['profile', 'settings', 'account'],
      icon: 'person',
      action: () => this.router.navigate(['/profile']),
      category: 'setting'
    }
  ];
  
  constructor(
    private router: Router,
    private commandPaletteService: CommandPaletteService
  ) {}
  
  ngOnInit(): void {
    // Subscribe to service state
    this.commandPaletteService.isOpen$
      .pipe(takeUntil(this.destroy$))
      .subscribe(isOpen => {
        if (isOpen && !this.isOpen) {
          this.open();
        } else if (!isOpen && this.isOpen) {
          this.close();
        }
      });
    this.filteredCommands = [...this.allCommands];
    
    // Set up search debouncing
    this.searchSubject.pipe(
      takeUntil(this.destroy$),
      debounceTime(200),
      distinctUntilChanged()
    ).subscribe(query => {
      this.filterCommands(query);
    });
  }
  
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
  
  @HostListener('document:keydown', ['$event'])
  handleKeyDown(event: KeyboardEvent): void {
    // Open command palette with Ctrl+K or Cmd+K
    if ((event.ctrlKey || event.metaKey) && event.key === 'k') {
      event.preventDefault();
      this.commandPaletteService.toggle();
      return;
    }
    
    // Handle shortcuts when palette is closed
    if (!this.isOpen) {
      this.handleGlobalShortcuts(event);
      return;
    }
    
    // Handle navigation within palette
    switch (event.key) {
      case 'Escape':
        this.close();
        break;
      case 'ArrowDown':
        event.preventDefault();
        this.selectNext();
        break;
      case 'ArrowUp':
        event.preventDefault();
        this.selectPrevious();
        break;
      case 'Enter':
        event.preventDefault();
        this.executeSelected();
        break;
    }
  }
  
  private handleGlobalShortcuts(event: KeyboardEvent): void {
    const shortcutKey = this.getShortcutKey(event);
    const command = this.allCommands.find(cmd => cmd.shortcut === shortcutKey);
    
    if (command) {
      event.preventDefault();
      command.action();
    }
  }
  
  private getShortcutKey(event: KeyboardEvent): string {
    const parts = [];
    
    if (event.ctrlKey) parts.push('Ctrl');
    if (event.metaKey) parts.push('Cmd');
    if (event.shiftKey) parts.push('Shift');
    if (event.altKey) parts.push('Alt');
    
    if (event.key !== 'Control' && event.key !== 'Meta' && 
        event.key !== 'Shift' && event.key !== 'Alt') {
      parts.push(event.key.toUpperCase());
    }
    
    return parts.join('+');
  }
  
  open(): void {
    this.isOpen = true;
    this.searchQuery = '';
    this.filteredCommands = [...this.allCommands];
    this.selectedCommandIndex = 0;
    this.selectedCommand = this.filteredCommands[0] || null;
    
    // Focus search input after animation
    setTimeout(() => {
      if (this.searchInput) {
        this.searchInput.nativeElement.focus();
      }
    }, 100);
  }
  
  close(): void {
    this.isOpen = false;
    this.searchQuery = '';
    this.selectedCommand = null;
    this.commandPaletteService.close();
  }
  
  onSearchChange(query: string): void {
    this.searchSubject.next(query);
  }
  
  private filterCommands(query: string): void {
    if (!query.trim()) {
      this.filteredCommands = [...this.allCommands];
    } else {
      const lowerQuery = query.toLowerCase();
      this.filteredCommands = this.allCommands.filter(command => {
        return command.title.toLowerCase().includes(lowerQuery) ||
               command.description?.toLowerCase().includes(lowerQuery) ||
               command.keywords.some(keyword => keyword.toLowerCase().includes(lowerQuery));
      });
    }
    
    // Reset selection
    this.selectedCommandIndex = 0;
    this.selectedCommand = this.filteredCommands[0] || null;
  }
  
  private selectNext(): void {
    if (this.filteredCommands.length > 0) {
      this.selectedCommandIndex = (this.selectedCommandIndex + 1) % this.filteredCommands.length;
      this.selectedCommand = this.filteredCommands[this.selectedCommandIndex];
    }
  }
  
  private selectPrevious(): void {
    if (this.filteredCommands.length > 0) {
      this.selectedCommandIndex = this.selectedCommandIndex === 0 
        ? this.filteredCommands.length - 1 
        : this.selectedCommandIndex - 1;
      this.selectedCommand = this.filteredCommands[this.selectedCommandIndex];
    }
  }
  
  private executeSelected(): void {
    if (this.selectedCommand) {
      this.executeCommand(this.selectedCommand);
    }
  }
  
  executeCommand(command: Command): void {
    this.close();
    command.action();
  }
  
  setSelected(command: Command): void {
    this.selectedCommand = command;
    this.selectedCommandIndex = this.filteredCommands.indexOf(command);
  }
  
  isSelected(command: Command): boolean {
    return this.selectedCommand?.id === command.id;
  }
  
  getCategories(): string[] {
    const categories = new Set(this.filteredCommands.map(cmd => cmd.category));
    return Array.from(categories).sort();
  }
  
  getCategoryCommands(category: string): Command[] {
    return this.filteredCommands.filter(cmd => cmd.category === category);
  }
  
  getCategoryLabel(category: string): string {
    switch (category) {
      case 'navigation':
        return 'Navigation';
      case 'action':
        return 'Actions';
      case 'data':
        return 'Data';
      case 'setting':
        return 'Settings';
      default:
        return category.charAt(0).toUpperCase() + category.slice(1);
    }
  }
  
  // Theme toggle method removed since dark mode is no longer supported
}