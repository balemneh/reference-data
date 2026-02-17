import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-justification-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './justification-modal.component.html',
  styleUrls: ['./justification-modal.component.scss']
})
export class JustificationModalComponent {
  @Input() title = 'Provide Business Justification';
  @Input() description = 'Please provide a business justification for this change request.';
  @Input() formErrors: any = {};
  @Output() submitRequest = new EventEmitter<string>();
  @Output() cancelRequest = new EventEmitter<void>();

  justification = '';

  submit() {
    if (this.validate()) {
      this.submitRequest.emit(this.justification);
    }
  }

  cancel() {
    this.cancelRequest.emit();
  }

  private validate(): boolean {
    this.formErrors.justification = undefined;

    if (!this.justification?.trim()) {
      this.formErrors.justification = 'Business justification is required';
      return false;
    }

    if (this.justification.trim().length < 10) {
      this.formErrors.justification = 'Business justification must be at least 10 characters';
      return false;
    }

    return true;
  }
}
