import { Component, Input, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChangeRequestDiff, ValueDiff, DiffViewConfig } from '../../models/change-request.models';

interface DiffLine {
  lineNumber?: number;
  content: string;
  type: 'ADDED' | 'REMOVED' | 'MODIFIED' | 'UNCHANGED';
  field?: string;
  path?: string[];
}

@Component({
  selector: 'app-diff-viewer',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './diff-viewer.component.html',
  styleUrl: './diff-viewer.component.scss'
})
export class DiffViewerComponent implements OnInit, OnChanges {
  @Input() oldValues: any = {};
  @Input() newValues: any = {};
  @Input() diff?: ChangeRequestDiff;
  @Input() showLineNumbers = true;
  @Input() highlightChanges = true;
  @Input() collapseUnchanged = false;
  @Input() viewMode: 'SIDE_BY_SIDE' | 'UNIFIED' = 'SIDE_BY_SIDE';

  leftLines: DiffLine[] = [];
  rightLines: DiffLine[] = [];
  unifiedLines: DiffLine[] = [];

  config: DiffViewConfig = {
    showLineNumbers: true,
    highlightChanges: true,
    collapseUnchanged: false,
    viewMode: 'SIDE_BY_SIDE'
  };

  statistics = {
    totalChanges: 0,
    additions: 0,
    deletions: 0,
    modifications: 0
  };

  loading = false;
  error: string | null = null;

  ngOnInit() {
    this.updateConfig();
    this.computeDiff();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['oldValues'] || changes['newValues'] || changes['diff']) {
      this.computeDiff();
    }
    if (changes['viewMode'] || changes['showLineNumbers'] || changes['highlightChanges'] || changes['collapseUnchanged']) {
      this.updateConfig();
    }
  }

  private updateConfig() {
    this.config = {
      showLineNumbers: this.showLineNumbers,
      highlightChanges: this.highlightChanges,
      collapseUnchanged: this.collapseUnchanged,
      viewMode: this.viewMode
    };
  }

  private computeDiff() {
    this.loading = true;
    this.error = null;

    try {
      if (this.diff) {
        this.processDiffData(this.diff);
      } else {
        this.computeClientSideDiff();
      }
      this.computeStatistics();
    } catch (error) {
      this.error = 'Failed to compute diff: ' + (error as Error).message;
      console.error('Diff computation error:', error);
    } finally {
      this.loading = false;
    }
  }

  private processDiffData(diff: ChangeRequestDiff) {
    this.leftLines = [];
    this.rightLines = [];
    this.unifiedLines = [];

    const processedFields = new Set<string>();

    // Process each diff entry
    diff.diffs.forEach((valueDiff, index) => {
      const field = valueDiff.field;
      if (processedFields.has(field)) return;
      processedFields.add(field);

      this.addFieldDiff(valueDiff, index + 1);
    });

    // Add unchanged fields if not collapsed
    if (!this.collapseUnchanged) {
      this.addUnchangedFields(processedFields);
    }
  }

  private computeClientSideDiff() {
    const oldKeys = new Set(Object.keys(this.oldValues || {}));
    const newKeys = new Set(Object.keys(this.newValues || {}));
    const allKeys = new Set([...oldKeys, ...newKeys]);

    this.leftLines = [];
    this.rightLines = [];
    this.unifiedLines = [];

    let lineNumber = 1;

    allKeys.forEach(key => {
      const oldValue = this.oldValues?.[key];
      const newValue = this.newValues?.[key];

      if (!oldKeys.has(key)) {
        // Added field
        this.addDiffLine(lineNumber++, key, '', this.formatValue(newValue), 'ADDED');
      } else if (!newKeys.has(key)) {
        // Removed field
        this.addDiffLine(lineNumber++, key, this.formatValue(oldValue), '', 'REMOVED');
      } else if (this.isValueEqual(oldValue, newValue)) {
        // Unchanged field
        if (!this.collapseUnchanged) {
          this.addDiffLine(lineNumber++, key, this.formatValue(oldValue), this.formatValue(newValue), 'UNCHANGED');
        }
      } else {
        // Modified field
        this.addDiffLine(lineNumber++, key, this.formatValue(oldValue), this.formatValue(newValue), 'MODIFIED');
      }
    });
  }

  private addFieldDiff(valueDiff: ValueDiff, lineNumber: number) {
    const oldValue = this.formatValue(valueDiff.oldValue);
    const newValue = this.formatValue(valueDiff.newValue);

    this.addDiffLine(lineNumber, valueDiff.field, oldValue, newValue, valueDiff.type);
  }

  private addUnchangedFields(processedFields: Set<string>) {
    const allFields = new Set([
      ...Object.keys(this.oldValues || {}),
      ...Object.keys(this.newValues || {})
    ]);

    allFields.forEach(field => {
      if (!processedFields.has(field)) {
        const value = this.oldValues?.[field] ?? this.newValues?.[field];
        const formattedValue = this.formatValue(value);
        this.addDiffLine(
          this.leftLines.length + 1,
          field,
          formattedValue,
          formattedValue,
          'UNCHANGED'
        );
      }
    });
  }

  private addDiffLine(lineNumber: number, field: string, leftContent: string, rightContent: string, type: DiffLine['type']) {
    const leftLine: DiffLine = {
      lineNumber: this.showLineNumbers ? lineNumber : undefined,
      content: `${field}: ${leftContent}`,
      type: type === 'ADDED' ? 'UNCHANGED' : type,
      field,
      path: [field]
    };

    const rightLine: DiffLine = {
      lineNumber: this.showLineNumbers ? lineNumber : undefined,
      content: `${field}: ${rightContent}`,
      type: type === 'REMOVED' ? 'UNCHANGED' : type,
      field,
      path: [field]
    };

    const unifiedLine: DiffLine = {
      lineNumber: this.showLineNumbers ? lineNumber : undefined,
      content: this.getUnifiedContent(field, leftContent, rightContent, type),
      type,
      field,
      path: [field]
    };

    this.leftLines.push(leftLine);
    this.rightLines.push(rightLine);
    this.unifiedLines.push(unifiedLine);
  }

  private getUnifiedContent(field: string, leftContent: string, rightContent: string, type: DiffLine['type']): string {
    switch (type) {
      case 'ADDED':
        return `+ ${field}: ${rightContent}`;
      case 'REMOVED':
        return `- ${field}: ${leftContent}`;
      case 'MODIFIED':
        return `~ ${field}: ${leftContent} → ${rightContent}`;
      case 'UNCHANGED':
        return `  ${field}: ${leftContent}`;
      default:
        return `  ${field}: ${leftContent}`;
    }
  }

  private formatValue(value: any): string {
    if (value === null || value === undefined) {
      return '(empty)';
    }

    if (typeof value === 'object') {
      try {
        return JSON.stringify(value, null, 2);
      } catch {
        return String(value);
      }
    }

    if (typeof value === 'string' && value.trim() === '') {
      return '(empty)';
    }

    return String(value);
  }

  private isValueEqual(val1: any, val2: any): boolean {
    if (val1 === val2) return true;

    if (val1 === null || val1 === undefined || val2 === null || val2 === undefined) {
      return val1 === val2;
    }

    if (typeof val1 === 'object' && typeof val2 === 'object') {
      return JSON.stringify(val1) === JSON.stringify(val2);
    }

    return String(val1) === String(val2);
  }

  private computeStatistics() {
    this.statistics = {
      totalChanges: 0,
      additions: 0,
      deletions: 0,
      modifications: 0
    };

    const lines = this.viewMode === 'UNIFIED' ? this.unifiedLines : this.rightLines;

    lines.forEach(line => {
      switch (line.type) {
        case 'ADDED':
          this.statistics.additions++;
          this.statistics.totalChanges++;
          break;
        case 'REMOVED':
          this.statistics.deletions++;
          this.statistics.totalChanges++;
          break;
        case 'MODIFIED':
          this.statistics.modifications++;
          this.statistics.totalChanges++;
          break;
      }
    });
  }

  // UI event handlers
  toggleViewMode() {
    this.viewMode = this.viewMode === 'SIDE_BY_SIDE' ? 'UNIFIED' : 'SIDE_BY_SIDE';
    this.config.viewMode = this.viewMode;
  }

  toggleLineNumbers() {
    this.showLineNumbers = !this.showLineNumbers;
    this.config.showLineNumbers = this.showLineNumbers;
    this.computeDiff();
  }

  toggleHighlight() {
    this.highlightChanges = !this.highlightChanges;
    this.config.highlightChanges = this.highlightChanges;
  }

  toggleCollapseUnchanged() {
    this.collapseUnchanged = !this.collapseUnchanged;
    this.config.collapseUnchanged = this.collapseUnchanged;
    this.computeDiff();
  }

  // Utility methods for templates
  getLineClass(line: DiffLine): string {
    const classes = ['diff-line'];

    if (this.highlightChanges) {
      switch (line.type) {
        case 'ADDED':
          classes.push('diff-line--added');
          break;
        case 'REMOVED':
          classes.push('diff-line--removed');
          break;
        case 'MODIFIED':
          classes.push('diff-line--modified');
          break;
        case 'UNCHANGED':
          classes.push('diff-line--unchanged');
          break;
      }
    }

    return classes.join(' ');
  }

  getChangeTypeIcon(type: DiffLine['type']): string {
    switch (type) {
      case 'ADDED': return 'add_circle';
      case 'REMOVED': return 'remove_circle';
      case 'MODIFIED': return 'edit';
      case 'UNCHANGED': return 'radio_button_unchecked';
      default: return 'radio_button_unchecked';
    }
  }

  getChangeTypeLabel(type: DiffLine['type']): string {
    switch (type) {
      case 'ADDED': return 'Added';
      case 'REMOVED': return 'Removed';
      case 'MODIFIED': return 'Modified';
      case 'UNCHANGED': return 'Unchanged';
      default: return 'Unknown';
    }
  }

  hasChanges(): boolean {
    return this.statistics.totalChanges > 0;
  }

  isEmpty(): boolean {
    return this.leftLines.length === 0 && this.rightLines.length === 0;
  }

  // TrackBy functions for better performance
  trackByLineNumber(index: number, line: DiffLine): any {
    return line.lineNumber || index;
  }
}