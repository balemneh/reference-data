import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, CountryDto } from '../../services/api.service';

@Component({
  selector: 'app-add-country-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './add-country-modal.component.html',
  styleUrls: ['./add-country-modal.component.scss']
})
export class AddCountryModalComponent implements OnInit {
  @Output() closeModal = new EventEmitter<void>();
  @Output() countryCreated = new EventEmitter<void>();

  selectedCountry: Partial<CountryDto> = {};
  isEditMode = false;
  formErrors: any = {};
  businessJustification = '';
  availableCodeSystems = ['ISO3166-1', 'ISO3166-2', 'ISO3166-3', 'GENC', 'CBP-COUNTRY5'];

  constructor(private apiService: ApiService) { }

  ngOnInit(): void {
    this.isEditMode = true;
    this.selectedCountry = { isActive: true, codeSystem: 'ISO3166-1' };
  }

  saveCountry() {
    if (!this.validateForm()) return;

    this.submitChangeRequest();
  }

  submitChangeRequest() {
    if (this.businessJustification.trim().length < 10) {
      this.formErrors.justification = 'Justification must be at least 10 characters long.';
      return;
    }

    this.formErrors.justification = null;
    const justification = this.businessJustification;

    if (!this.selectedCountry) return;

    const countryData = this.selectedCountry as CountryDto;

    const changeRequest = {
      changeType: 'CREATE' as 'CREATE',
      entityType: 'COUNTRY' as 'COUNTRY',
      title: `Create country: ${countryData.countryName}`,
      requestor: 'current-user', // Would come from auth service
      justification: justification,
      proposedChanges: countryData
    };

    this.apiService.createChangeRequest(changeRequest).subscribe({
      next: () => {
        this.countryCreated.emit();
        this.close();
      },
      error: (err) => {
        this.formErrors.general = `Failed to submit change request: ${err.error?.detail || err.message}`;
      }
    });
  }

  validateForm(): boolean {
    this.formErrors = {};
    const country = this.selectedCountry;

    if (!country?.countryName?.trim()) {
      this.formErrors.countryName = 'Country Name is required.';
    }
    if (!country?.countryCode?.trim()) {
      this.formErrors.countryCode = 'Country Code is required.';
    }
    if (!country?.iso2Code?.trim()) {
      this.formErrors.iso2Code = 'ISO2 Code is required.';
    }
    if (country?.iso2Code?.trim().length !== 2) {
      this.formErrors.iso2Code = 'ISO2 Code must be 2 characters.';
    }
    if (!country?.iso3Code?.trim()) {
      this.formErrors.iso3Code = 'ISO3 Code is required.';
    }
    if (country?.iso3Code?.trim().length !== 3) {
      this.formErrors.iso3Code = 'ISO3 Code must be 3 characters.';
    }

    return Object.keys(this.formErrors).length === 0;
  }

  close() {
    this.closeModal.emit();
  }
}
