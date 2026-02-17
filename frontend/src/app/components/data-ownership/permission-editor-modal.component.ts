import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserManagementService, UserDto } from '../../services/user-management.service';
import { ToastService } from '../../services/toast.service';

interface Role {
  name: string;
  description: string;
}

@Component({
  selector: 'app-permission-editor-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div *ngIf="isOpen" class="cbp-modal-overlay cbp-fade-in" (click)="close()">
      <div class="cbp-modal cbp-slide-up" (click)="$event.stopPropagation()" role="dialog" aria-modal="true" aria-labelledby="permission-modal-title">
        <div class="cbp-modal-header">
          <h2 class="cbp-modal-title" id="permission-modal-title">Edit Data Ownership</h2>
          <button class="cbp-modal-close" (click)="close()" aria-label="Close modal">
            <svg class="cbp-modal-close-icon" aria-hidden="true">
              <use xlink:href="assets/img/sprite.svg#close"></use>
            </svg>
          </button>
        </div>
        <div class="cbp-modal-content">
          <div *ngIf="steward">
            <p class="margin-bottom-2">
              Manage data ownership for <strong>{{ steward.firstName }} {{ steward.lastName }}</strong> ({{ steward.username }}).
            </p>
            <div class="grid-row grid-gap">
              <div class="grid-col-6">
                <h3>Assigned Roles</h3>
                <table class="usa-table usa-table--striped">
                  <tbody>
                    <tr *ngFor="let role of assignedRoles">
                      <td>{{ role.description }}</td>
                      <td>
                        <button class="usa-button usa-button--outline" (click)="unassignRole(role.name)">Unassign</button>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <div class="grid-col-6">
                <h3>Available Roles</h3>
                <table class="usa-table usa-table--striped">
                  <tbody>
                    <tr *ngFor="let role of unassignedRoles">
                      <td>{{ role.description }}</td>
                      <td>
                        <button class="usa-button" (click)="assignRole(role.name)">Add</button>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
        <div class="cbp-modal-footer">
          <button class="cbp-button cbp-button--secondary" (click)="close()">Cancel</button>
          <button class="cbp-button cbp-button--primary" (click)="savePermissions()">Save Changes</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .grid-row {
      display: flex;
    }
    .grid-col-6 {
      width: 50%;
      padding: 0 10px;
    }
  `]
})
export class PermissionEditorModalComponent implements OnInit, OnChanges {
  @Input() steward: UserDto | null = null;
  @Input() isOpen = false;
  @Output() modalClose = new EventEmitter<boolean>();

  allRoles: Role[] = [
    { name: 'country-owner', description: 'Country Ownership' },
    { name: 'airport-owner', description: 'Airport Ownership' },
    { name: 'port-owner', description: 'Port Ownership' }
  ];

  assignedRoles: Role[] = [];
  unassignedRoles: Role[] = [];

  constructor(
    private userManagementService: UserManagementService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    if (this.steward) {
      this.loadPermissions();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['steward'] && changes['steward'].currentValue) {
      this.loadPermissions();
    }
  }

  loadPermissions(): void {
    if (!this.steward) return;
    this.userManagementService.getUserPermissions(this.steward.id).subscribe(
      (permissions) => {
        console.log('Permissions from backend:', permissions);
        this.assignedRoles = this.allRoles.filter(role => permissions.includes(role.name));
        this.unassignedRoles = this.allRoles.filter(role => !permissions.includes(role.name));
        console.log('Assigned roles:', this.assignedRoles);
        console.log('Unassigned roles:', this.unassignedRoles);
      },
      (error) => {
        this.toastService.showError('Error', 'Failed to load user permissions.');
        console.error('Failed to load user permissions:', error);
      }
    );
  }

  assignRole(roleName: string): void {
    const roleToAssign = this.unassignedRoles.find(role => role.name === roleName);
    if (roleToAssign) {
      this.assignedRoles.push(roleToAssign);
      this.unassignedRoles = this.unassignedRoles.filter(role => role.name !== roleName);
    }
  }

  unassignRole(roleName: string): void {
    const roleToUnassign = this.assignedRoles.find(role => role.name === roleName);
    if (roleToUnassign) {
      this.unassignedRoles.push(roleToUnassign);
      this.assignedRoles = this.assignedRoles.filter(role => role.name !== roleName);
    }
  }

  savePermissions(): void {
    if (!this.steward) return;
    const permissionNames = this.assignedRoles.map(role => role.name);
    this.userManagementService.updateUserPermissions(this.steward.id, permissionNames).subscribe(
      () => {
        this.toastService.showSuccess('Success', 'Permissions updated successfully.');
        this.modalClose.emit(true);
      },
      (error) => {
        this.toastService.showError('Error', 'Failed to update permissions.');
        console.error('Failed to update permissions:', error);
      }
    );
  }

  close(): void {
    this.modalClose.emit(false);
  }
}
