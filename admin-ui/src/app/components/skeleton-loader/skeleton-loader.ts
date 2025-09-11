import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type SkeletonType = 'text' | 'card' | 'button' | 'avatar' | 'metric' | 'chart' | 'table' | 'activity';
export type SkeletonSize = 'sm' | 'md' | 'lg' | 'xl';

@Component({
  selector: 'app-skeleton-loader',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div 
      class="cbp-skeleton" 
      [ngClass]="getSkeletonClasses()"
      [attr.aria-label]="'Loading ' + type"
      role="status"
    >
      <!-- Text Skeleton -->
      <ng-container *ngIf="type === 'text'">
        <div class="cbp-skeleton__line" *ngFor="let line of getLines()"></div>
      </ng-container>
      
      <!-- Card Skeleton -->
      <ng-container *ngIf="type === 'card'">
        <div class="cbp-skeleton__card-header">
          <div class="cbp-skeleton__avatar"></div>
          <div class="cbp-skeleton__card-content">
            <div class="cbp-skeleton__line cbp-skeleton__line--title"></div>
            <div class="cbp-skeleton__line cbp-skeleton__line--subtitle"></div>
          </div>
        </div>
        <div class="cbp-skeleton__card-body">
          <div class="cbp-skeleton__line" *ngFor="let line of [1,2,3]"></div>
        </div>
      </ng-container>
      
      <!-- Button Skeleton -->
      <ng-container *ngIf="type === 'button'">
        <div class="cbp-skeleton__button"></div>
      </ng-container>
      
      <!-- Avatar Skeleton -->
      <ng-container *ngIf="type === 'avatar'">
        <div class="cbp-skeleton__avatar" [ngClass]="'cbp-skeleton__avatar--' + size"></div>
      </ng-container>
      
      <!-- Metric Card Skeleton -->
      <ng-container *ngIf="type === 'metric'">
        <div class="cbp-skeleton__metric">
          <div class="cbp-skeleton__metric-header">
            <div class="cbp-skeleton__line cbp-skeleton__line--title"></div>
            <div class="cbp-skeleton__badge"></div>
          </div>
          <div class="cbp-skeleton__metric-value"></div>
          <div class="cbp-skeleton__line cbp-skeleton__line--subtitle"></div>
          <div class="cbp-skeleton__progress"></div>
        </div>
      </ng-container>
      
      <!-- Chart Skeleton -->
      <ng-container *ngIf="type === 'chart'">
        <div class="cbp-skeleton__chart">
          <div class="cbp-skeleton__chart-header">
            <div class="cbp-skeleton__line cbp-skeleton__line--title"></div>
            <div class="cbp-skeleton__line cbp-skeleton__line--subtitle"></div>
          </div>
          <div class="cbp-skeleton__chart-body">
            <div class="cbp-skeleton__chart-placeholder"></div>
          </div>
        </div>
      </ng-container>
      
      <!-- Table Skeleton -->
      <ng-container *ngIf="type === 'table'">
        <div class="cbp-skeleton__table">
          <div class="cbp-skeleton__table-header">
            <div class="cbp-skeleton__table-cell" *ngFor="let col of getTableCols()"></div>
          </div>
          <div class="cbp-skeleton__table-row" *ngFor="let row of getTableRows()">
            <div class="cbp-skeleton__table-cell" *ngFor="let col of getTableCols()"></div>
          </div>
        </div>
      </ng-container>
      
      <!-- Activity Timeline Skeleton -->
      <ng-container *ngIf="type === 'activity'">
        <div class="cbp-skeleton__activity" *ngFor="let item of getActivityItems()">
          <div class="cbp-skeleton__activity-dot"></div>
          <div class="cbp-skeleton__activity-content">
            <div class="cbp-skeleton__activity-header">
              <div class="cbp-skeleton__line cbp-skeleton__line--title"></div>
              <div class="cbp-skeleton__line cbp-skeleton__line--time"></div>
            </div>
            <div class="cbp-skeleton__activity-body">
              <div class="cbp-skeleton__line" *ngFor="let line of [1,2]"></div>
            </div>
          </div>
        </div>
      </ng-container>
    </div>
  `,
  styleUrls: ['./skeleton-loader.scss']
})
export class SkeletonLoaderComponent {
  @Input() type: SkeletonType = 'text';
  @Input() size: SkeletonSize = 'md';
  @Input() lines: number = 3;
  @Input() width: string = '100%';
  @Input() height: string = 'auto';
  @Input() animate: boolean = true;
  @Input() tableCols: number = 4;
  @Input() tableRows: number = 5;
  @Input() activityItems: number = 5;
  
  getSkeletonClasses(): string {
    const classes = [
      `cbp-skeleton--${this.type}`,
      `cbp-skeleton--${this.size}`
    ];
    
    if (this.animate) {
      classes.push('cbp-skeleton--animated');
    }
    
    return classes.join(' ');
  }
  
  getLines(): number[] {
    return Array.from({ length: this.lines }, (_, i) => i);
  }
  
  getTableCols(): number[] {
    return Array.from({ length: this.tableCols }, (_, i) => i);
  }
  
  getTableRows(): number[] {
    return Array.from({ length: this.tableRows }, (_, i) => i);
  }
  
  getActivityItems(): number[] {
    return Array.from({ length: this.activityItems }, (_, i) => i);
  }
}