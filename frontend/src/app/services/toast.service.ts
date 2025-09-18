import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, timer } from 'rxjs';

export interface Toast {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message?: string;
  duration?: number;
  action?: {
    label: string;
    handler: () => void;
  };
  dismissible?: boolean;
  persistent?: boolean;
  timestamp: Date;
}

export interface ToastOptions {
  type?: Toast['type'];
  title: string;
  message?: string;
  duration?: number;
  action?: Toast['action'];
  dismissible?: boolean;
  persistent?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private readonly DEFAULT_DURATION = 5000; // 5 seconds
  private readonly MAX_TOASTS = 5;
  
  private toastsSubject = new BehaviorSubject<Toast[]>([]);
  public toasts$: Observable<Toast[]> = this.toastsSubject.asObservable();
  
  private toasts: Toast[] = [];
  
  constructor() {}
  
  private generateId(): string {
    return Date.now().toString(36) + Math.random().toString(36).substr(2);
  }
  
  private updateToasts(): void {
    this.toastsSubject.next([...this.toasts]);
  }
  
  private addToast(options: ToastOptions): string {
    const toast: Toast = {
      id: this.generateId(),
      type: options.type || 'info',
      title: options.title,
      message: options.message,
      duration: options.persistent ? 0 : (options.duration || this.DEFAULT_DURATION),
      action: options.action,
      dismissible: options.dismissible !== false,
      persistent: options.persistent || false,
      timestamp: new Date()
    };
    
    // Remove oldest toast if at max capacity
    if (this.toasts.length >= this.MAX_TOASTS) {
      this.toasts.shift();
    }
    
    this.toasts.push(toast);
    this.updateToasts();
    
    // Auto-dismiss after duration if not persistent
    if (!toast.persistent && toast.duration && toast.duration > 0) {
      timer(toast.duration).subscribe(() => {
        this.dismiss(toast.id);
      });
    }
    
    return toast.id;
  }
  
  public success(options: Omit<ToastOptions, 'type'>): string {
    return this.addToast({ ...options, type: 'success' });
  }
  
  public error(options: Omit<ToastOptions, 'type'>): string {
    return this.addToast({ ...options, type: 'error', persistent: true });
  }
  
  public warning(options: Omit<ToastOptions, 'type'>): string {
    return this.addToast({ ...options, type: 'warning' });
  }
  
  public info(options: Omit<ToastOptions, 'type'>): string {
    return this.addToast({ ...options, type: 'info' });
  }
  
  public dismiss(id: string): void {
    const index = this.toasts.findIndex(toast => toast.id === id);
    if (index > -1) {
      this.toasts.splice(index, 1);
      this.updateToasts();
    }
  }
  
  public dismissAll(): void {
    this.toasts = [];
    this.updateToasts();
  }
  
  public dismissByType(type: Toast['type']): void {
    this.toasts = this.toasts.filter(toast => toast.type !== type);
    this.updateToasts();
  }
  
  // Convenience methods for common scenarios
  public showSuccess(title: string, message?: string): string {
    return this.success({ title, message });
  }
  
  public showError(title: string, message?: string): string {
    return this.error({ title, message });
  }
  
  public showWarning(title: string, message?: string): string {
    return this.warning({ title, message });
  }
  
  public showInfo(title: string, message?: string): string {
    return this.info({ title, message });
  }
  
  // API operation helpers
  public showSaveSuccess(entity: string = 'Item'): string {
    return this.success({
      title: 'Save Successful',
      message: `${entity} has been saved successfully.`
    });
  }
  
  public showSaveError(entity: string = 'Item', error?: string): string {
    return this.error({
      title: 'Save Failed',
      message: error || `Failed to save ${entity}. Please try again.`
    });
  }
  
  public showDeleteSuccess(entity: string = 'Item'): string {
    return this.success({
      title: 'Delete Successful',
      message: `${entity} has been deleted successfully.`
    });
  }
  
  public showDeleteError(entity: string = 'Item', error?: string): string {
    return this.error({
      title: 'Delete Failed',
      message: error || `Failed to delete ${entity}. Please try again.`
    });
  }
  
  public showLoadError(entity: string = 'Data', error?: string): string {
    return this.error({
      title: 'Load Failed',
      message: error || `Failed to load ${entity}. Please refresh the page.`,
      action: {
        label: 'Refresh',
        handler: () => window.location.reload()
      }
    });
  }
  
  public showNetworkError(): string {
    return this.error({
      title: 'Network Error',
      message: 'Unable to connect to the server. Please check your connection.',
      action: {
        label: 'Retry',
        handler: () => window.location.reload()
      }
    });
  }
  
  public showValidationError(message: string): string {
    return this.warning({
      title: 'Validation Error',
      message
    });
  }
  
  public showPermissionError(): string {
    return this.error({
      title: 'Access Denied',
      message: 'You do not have permission to perform this action.'
    });
  }
  
  // Undo functionality
  public showUndoAction(title: string, undoAction: () => void, duration: number = 8000): string {
    return this.info({
      title,
      duration,
      action: {
        label: 'Undo',
        handler: undoAction
      }
    });
  }
  
  // System maintenance notifications
  public showMaintenanceNotice(message?: string): string {
    return this.warning({
      title: 'System Maintenance',
      message: message || 'System maintenance is scheduled. Some features may be temporarily unavailable.',
      persistent: true
    });
  }
  
  // Progress notifications
  public showProgress(title: string, message?: string): string {
    return this.info({
      title,
      message,
      persistent: true,
      dismissible: false
    });
  }
  
  public updateProgress(id: string, message: string): void {
    const toast = this.toasts.find(t => t.id === id);
    if (toast) {
      toast.message = message;
      this.updateToasts();
    }
  }
  
  public completeProgress(id: string, successTitle?: string): void {
    const toast = this.toasts.find(t => t.id === id);
    if (toast) {
      this.dismiss(id);
      if (successTitle) {
        this.showSuccess(successTitle);
      }
    }
  }
}