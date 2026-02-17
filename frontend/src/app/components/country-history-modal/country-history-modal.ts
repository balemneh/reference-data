import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';
import { CountryDto } from '../../services/api.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-country-history-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './country-history-modal.html',
  styleUrls: ['./country-history-modal.scss']
})
export class CountryHistoryModalComponent implements OnChanges {
  @Input() country: CountryDto | null = null;
  @Input() history: any[] = [];
  @Output() closeModal = new EventEmitter<void>();

  searchTerm = '';
  filteredHistory: any[] = [];

  ngOnChanges(changes: SimpleChanges) {
    if (changes['history']) {
      this.filteredHistory = this.history;
    }
  }

  filterHistory() {
    if (!this.searchTerm) {
      this.filteredHistory = this.history;
      return;
    }

    const lowercasedTerm = this.searchTerm.toLowerCase();
    this.filteredHistory = this.history.filter(item =>
      item.changeType.toLowerCase().includes(lowercasedTerm) ||
      item.changedBy.toLowerCase().includes(lowercasedTerm) ||
      item.description.toLowerCase().includes(lowercasedTerm) ||
      (item.details.countryName && item.details.countryName.toLowerCase().includes(lowercasedTerm)) ||
      (item.details.iso2Code && item.details.iso2Code.toLowerCase().includes(lowercasedTerm))
    );
  }
  
trackByChangeId(index: number, item: any): string {
    return item.changeId;
  }
}
