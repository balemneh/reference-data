import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChangeRequestService } from '../../services/change-request.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-pending-requests-tracker',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="cbp-badge-container">
      <span class="cbp-badge cbp-badge--warning" *ngIf="pendingRequestsCount > 0">
        {{ pendingRequestsCount }} Pending
      </span>
      <span class="cbp-badge cbp-badge--success" *ngIf="pendingRequestsCount === 0">
        All Caught Up
      </span>
    </div>
  `
})
export class PendingRequestsTrackerComponent implements OnInit, OnDestroy {
  pendingRequestsCount = 0;
  private destroy$ = new Subject<void>();

  constructor(private changeRequestService: ChangeRequestService) {}

  ngOnInit() {
    this.changeRequestService.pendingRequestsCount$
      .pipe(takeUntil(this.destroy$))
      .subscribe(count => {
        this.pendingRequestsCount = count;
      });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
