import { Component, EventEmitter, Input, Output, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, ScheduledExport } from '../../services/api.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-schedule-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './schedule-modal.component.html',
  styleUrls: ['./schedule-modal.component.scss']
})
export class ScheduleModalComponent implements OnInit {
  @Input() showModal = false;
  @Input() schedule: ScheduledExport | null = null;
  @Output() close = new EventEmitter<void>();
  @Output() saved = new EventEmitter<ScheduledExport>();

  scheduleForm: any = {};
  
  cronOptions = [
    { value: '0 0 2 * * *', label: 'Daily at 2 AM' },
    { value: '0 0 0 * * 0', label: 'Weekly on Sunday' },
    { value: '0 0 0 * * 1', label: 'Weekly on Monday' },
    { value: '0 0 0 1 * *', label: 'Monthly on 1st' }
  ];

  constructor(
    private apiService: ApiService,
    private toastService: ToastService
  ) {}

  ngOnInit() {
    this.resetForm();
  }

  ngOnChanges() {
    if (this.schedule) {
      this.scheduleForm = { ...this.schedule };
    } else {
      this.resetForm();
    }
  }

  resetForm() {
    this.scheduleForm = {
      name: '',
      entityType: 'COUNTRIES',
      format: 'CSV',
      schedule: '0 0 2 * * *',
      enabled: true,
      recipients: ''
    };
  }

  saveSchedule() {
    if (!this.scheduleForm.name.trim()) {
      this.toastService.showError('Validation Error', 'Schedule name is required');
      return;
    }

    const operation = this.schedule 
      ? this.apiService.updateScheduledExport(this.schedule.id, this.scheduleForm)
      : this.apiService.createScheduledExport({ ...this.scheduleForm, createdBy: 'current-user' });

    operation.subscribe({
      next: (response) => {
        this.toastService.showSuccess('Schedule saved', 'Scheduled export saved successfully');
        this.saved.emit(response);
      },
      error: (error) => {
        this.toastService.showError('Error saving schedule', 'Could not save scheduled export. See console for details.');
        console.error('Error saving schedule:', error);
      }
    });
  }

  closeModal() {
    this.close.emit();
  }
}
