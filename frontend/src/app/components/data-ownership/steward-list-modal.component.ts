import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserDto } from '../../services/user-management.service';

@Component({
  selector: 'app-steward-list-modal',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div *ngIf="isOpen" class="cbp-modal-overlay cbp-fade-in" (click)="close.emit()">
      <div class="cbp-modal cbp-slide-up" (click)="$event.stopPropagation()" role="dialog" aria-modal="true" aria-labelledby="steward-modal-title">
        <div class="cbp-modal-header">
          <h2 class="cbp-modal-title" id="steward-modal-title">All Data Stewards</h2>
          <button class="cbp-modal-close" (click)="close.emit()" aria-label="Close modal">
            <svg class="cbp-modal-close-icon" aria-hidden="true">
              <use xlink:href="assets/img/sprite.svg#close"></use>
            </svg>
          </button>
        </div>
        <div class="cbp-modal-content">
          <table class="cbp-table">
            <thead class="cbp-table-head">
              <tr class="cbp-table-row cbp-table-row--header">
                <th class="cbp-table-cell">Name</th>
                <th class="cbp-table-cell">Username</th>
                <th class="cbp-table-cell">Department</th>
                <th class="cbp-table-cell cbp-table-cell--actions">Actions</th>
              </tr>
            </thead>
            <tbody class="cbp-table-body">
              <tr class="cbp-table-row cbp-table-row--data" *ngFor="let steward of stewards">
                <td class="cbp-table-cell cbp-table-cell--primary">
                  <div class="cbp-user-name">
                    <div class="cbp-user-name-text">{{ steward.firstName }} {{ steward.lastName }}</div>
                    <div class="cbp-user-email">{{ steward.email }}</div>
                  </div>
                </td>
                <td class="cbp-table-cell cbp-table-cell--code">
                  <span class="cbp-username">{{ steward.username }}</span>
                </td>
                <td class="cbp-table-cell">
                  <span class="cbp-department-text">{{ steward.department }}</span>
                </td>
                <td class="cbp-table-cell cbp-table-cell--actions">
                  <button class="usa-button" (click)="openOwnership.emit(steward)">Ownership</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="cbp-modal-footer">
          <button class="cbp-button cbp-button--secondary" (click)="close.emit()">Close</button>
        </div>
      </div>
    </div>
  `,
})
export class StewardListModalComponent {
  @Input() stewards: UserDto[] = [];
  @Input() isOpen = false;
  @Output() close = new EventEmitter<void>();
  @Output() openOwnership = new EventEmitter<UserDto>();
}
