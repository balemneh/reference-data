import { Component, Input, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApprovalHistoryEntry } from '../../models/change-request.models';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-approval-history',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './approval-history.component.html',
  styleUrl: './approval-history.component.scss'
})
export class ApprovalHistoryComponent implements OnInit, OnChanges {
  @Input() changeRequestId!: string;
  @Input() history?: ApprovalHistoryEntry[];
  @Input() showTimeline = true;
  @Input() showDetails = true;
  @Input() maxItems?: number;

  historyEntries: ApprovalHistoryEntry[] = [];
  loading = false;
  error: string | null = null;
  expanded = new Set<string>();

  constructor(private apiService: ApiService) {}

  ngOnInit() {
    if (!this.history && this.changeRequestId) {
      this.loadHistory();
    } else if (this.history) {
      this.processHistory(this.history);
    }
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['changeRequestId'] && this.changeRequestId) {
      this.loadHistory();
    } else if (changes['history'] && this.history) {
      this.processHistory(this.history);
    }
  }

  private loadHistory() {
    if (!this.changeRequestId) return;

    this.loading = true;
    this.error = null;

    this.apiService.getChangeRequestHistory(this.changeRequestId).subscribe({
      next: (history) => {
        this.processHistory(history);
        this.loading = false;
      },
      error: (error) => {
        this.error = 'Failed to load approval history';
        this.loading = false;
        console.error('Error loading approval history:', error);
      }
    });
  }

  private processHistory(history: ApprovalHistoryEntry[]) {
    // Sort by date (newest first)
    this.historyEntries = [...history].sort((a, b) =>
      new Date(b.performedAt).getTime() - new Date(a.performedAt).getTime()
    );

    // Apply max items limit if specified
    if (this.maxItems && this.historyEntries.length > this.maxItems) {
      this.historyEntries = this.historyEntries.slice(0, this.maxItems);
    }
  }

  getActionIcon(action: ApprovalHistoryEntry['action']): string {
    switch (action) {
      case 'SUBMITTED': return 'send';
      case 'REVIEWED': return 'visibility';
      case 'APPROVED': return 'check_circle';
      case 'REJECTED': return 'cancel';
      case 'CANCELLED': return 'remove_circle';
      case 'APPLIED': return 'published_with_changes';
      default: return 'help_outline';
    }
  }

  getActionColor(action: ApprovalHistoryEntry['action']): string {
    switch (action) {
      case 'SUBMITTED': return 'blue';
      case 'REVIEWED': return 'purple';
      case 'APPROVED': return 'green';
      case 'REJECTED': return 'red';
      case 'CANCELLED': return 'gray';
      case 'APPLIED': return 'emerald';
      default: return 'gray';
    }
  }

  getActionLabel(action: ApprovalHistoryEntry['action']): string {
    switch (action) {
      case 'SUBMITTED': return 'Submitted';
      case 'REVIEWED': return 'Reviewed';
      case 'APPROVED': return 'Approved';
      case 'REJECTED': return 'Rejected';
      case 'CANCELLED': return 'Cancelled';
      case 'APPLIED': return 'Applied';
      default: return action;
    }
  }

  getStatusChangeText(entry: ApprovalHistoryEntry): string {
    if (entry.previousStatus && entry.newStatus) {
      return `Status changed from ${entry.previousStatus} to ${entry.newStatus}`;
    }
    return '';
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: true
    });
  }

  formatRelativeTime(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMinutes = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMinutes / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMinutes < 1) return 'Just now';
    if (diffMinutes < 60) return `${diffMinutes}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    if (diffDays < 30) return `${Math.floor(diffDays / 7)}w ago`;
    return `${Math.floor(diffDays / 30)}mo ago`;
  }

  toggleExpanded(entryId: string) {
    if (this.expanded.has(entryId)) {
      this.expanded.delete(entryId);
    } else {
      this.expanded.add(entryId);
    }
  }

  isExpanded(entryId: string): boolean {
    return this.expanded.has(entryId);
  }

  hasComments(entry: ApprovalHistoryEntry): boolean {
    return !!(entry.comments && entry.comments.trim());
  }

  getInitials(name: string): string {
    return name
      .split(' ')
      .map(part => part.charAt(0).toUpperCase())
      .slice(0, 2)
      .join('');
  }

  isEmpty(): boolean {
    return this.historyEntries.length === 0;
  }

  reload() {
    this.loadHistory();
  }

  // TrackBy function for better performance
  trackByEntryId(index: number, entry: ApprovalHistoryEntry): string {
    return entry.id;
  }
}