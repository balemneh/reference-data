import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { trigger, state, style, transition, animate } from '@angular/animations';
import { ToastService, Toast } from '../../services/toast.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="cbp-toast-container" role="region" aria-label="Notifications">
      <div 
        *ngFor="let toast of toasts; trackBy: trackByToastId"
        class="cbp-toast"
        [attr.data-type]="toast.type"
        [attr.data-id]="toast.id"
        [@slideIn]
        role="alert"
        [attr.aria-live]="toast.type === 'error' ? 'assertive' : 'polite'"
      >
        <div class="cbp-toast__icon">
          <svg class="cbp-toast-icon" aria-hidden="true">
            <use [attr.xlink:href]="'/assets/uswds/img/sprite.svg#' + getToastIcon(toast.type)"></use>
          </svg>
        </div>
        
        <div class="cbp-toast__content">
          <div class="cbp-toast__header">
            <h4 class="cbp-toast__title">{{ toast.title }}</h4>
            <div class="cbp-toast__timestamp">{{ getRelativeTime(toast.timestamp) }}</div>
          </div>
          
          <div class="cbp-toast__message" *ngIf="toast.message">
            {{ toast.message }}
          </div>
          
          <div class="cbp-toast__actions" *ngIf="toast.action">
            <button 
              class="cbp-toast__action-button"
              (click)="handleAction(toast)"
              type="button"
            >
              {{ toast.action.label }}
            </button>
          </div>
        </div>
        
        <div class="cbp-toast__controls" *ngIf="toast.dismissible">
          <button 
            class="cbp-toast__dismiss"
            (click)="dismiss(toast.id)"
            [attr.aria-label]="'Dismiss ' + toast.title"
            type="button"
          >
            <svg class="cbp-dismiss-icon" aria-hidden="true">
              <use xlink:href="/assets/uswds/img/sprite.svg#close"></use>
            </svg>
          </button>
        </div>
        
        <!-- Progress bar for non-persistent toasts -->
        <div 
          class="cbp-toast__progress" 
          *ngIf="!toast.persistent && toast.duration && toast.duration > 0"
          [style.animation-duration]="toast.duration + 'ms'"
        ></div>
      </div>
    </div>
  `,
  styleUrls: ['./toast-container.scss'],
  animations: [
    trigger('slideIn', [
      state('in', style({ opacity: 1, transform: 'translateX(0)' })),
      transition('void => *', [
        style({ opacity: 0, transform: 'translateX(100%)' }),
        animate('300ms cubic-bezier(0.25, 0.8, 0.25, 1)')
      ]),
      transition('* => void', [
        animate('200ms cubic-bezier(0.25, 0.8, 0.25, 1)', 
          style({ opacity: 0, transform: 'translateX(100%)' })
        )
      ])
    ])
  ]
})
export class ToastContainerComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  toasts: Toast[] = [];
  
  constructor(private toastService: ToastService) {}
  
  ngOnInit(): void {
    this.toastService.toasts$
      .pipe(takeUntil(this.destroy$))
      .subscribe(toasts => {
        this.toasts = toasts;
      });
  }
  
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
  
  trackByToastId(index: number, toast: Toast): string {
    return toast.id;
  }
  
  dismiss(id: string): void {
    this.toastService.dismiss(id);
  }
  
  handleAction(toast: Toast): void {
    if (toast.action) {
      toast.action.handler();
      this.dismiss(toast.id);
    }
  }
  
  getToastIcon(type: Toast['type']): string {
    switch (type) {
      case 'success':
        return 'check_circle';
      case 'error':
        return 'error';
      case 'warning':
        return 'warning';
      case 'info':
        return 'info';
      default:
        return 'info';
    }
  }
  
  getRelativeTime(timestamp: Date): string {
    const now = new Date();
    const diffMs = now.getTime() - timestamp.getTime();
    const diffMinutes = Math.floor(diffMs / (1000 * 60));
    
    if (diffMinutes < 1) {
      return 'Just now';
    } else if (diffMinutes === 1) {
      return '1 minute ago';
    } else if (diffMinutes < 60) {
      return `${diffMinutes} minutes ago`;
    } else {
      const diffHours = Math.floor(diffMinutes / 60);
      if (diffHours === 1) {
        return '1 hour ago';
      } else if (diffHours < 24) {
        return `${diffHours} hours ago`;
      } else {
        return timestamp.toLocaleDateString();
      }
    }
  }
}