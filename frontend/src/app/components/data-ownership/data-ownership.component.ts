import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserManagementService, UserDto } from '../../services/user-management.service';
import { PermissionEditorModalComponent } from './permission-editor-modal.component';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-data-ownership',
  standalone: true,
  imports: [CommonModule, PermissionEditorModalComponent],
  template: `
    <div class="container">
      <h1>Data Ownership</h1>
      <p>Manage data ownership for Data Stewards.</p>
      <table class="usa-table usa-table--striped">
        <thead>
          <tr>
            <th>Username</th>
            <th>Name</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let steward of stewards">
            <td>{{ steward.username }}</td>
            <td>{{ steward.firstName }} {{ steward.lastName }}</td>
            <td>
              <button class="usa-button" (click)="openPermissionModal(steward)">Manage Permissions</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <app-permission-editor-modal
      [isOpen]="isModalOpen"
      [steward]="selectedSteward"
      (modalClose)="onModalClose($event)"
    ></app-permission-editor-modal>
  `,
})
export class DataOwnershipComponent implements OnInit {
  stewards: UserDto[] = [];
  isModalOpen = false;
  selectedSteward: UserDto | null = null;

  constructor(
    private userManagementService: UserManagementService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.loadStewards();
  }

  loadStewards(): void {
    this.userManagementService.getStewards().subscribe(
      (stewards) => {
        this.stewards = stewards;
      },
      (error) => {
        this.toastService.showError('Error', 'Failed to load data stewards.');
        console.error('Failed to load data stewards:', error);
      }
    );
  }

  openPermissionModal(steward: UserDto): void {
    this.selectedSteward = steward;
    this.isModalOpen = true;
  }

  onModalClose(needsRefresh: boolean): void {
    this.isModalOpen = false;
    this.selectedSteward = null;
    if (needsRefresh) {
      this.loadStewards();
    }
  }
}
