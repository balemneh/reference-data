import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { ImportExportHistory } from '../components/import-export/import-export';

@Injectable({
  providedIn: 'root'
})
export class OperationHistoryService {
  private readonly storageKey = 'operationHistory';
  private historySubject: BehaviorSubject<ImportExportHistory[]>;
  history$: Observable<ImportExportHistory[]>;

  constructor() {
    const storedHistory = sessionStorage.getItem(this.storageKey);
    const initialHistory = storedHistory ? JSON.parse(storedHistory) : [];
    this.historySubject = new BehaviorSubject<ImportExportHistory[]>(initialHistory);
    this.history$ = this.historySubject.asObservable();
  }

  getHistory(): ImportExportHistory[] {
    return this.historySubject.getValue();
  }

  addOperation(operation: ImportExportHistory) {
    const currentHistory = this.getHistory();
    const newHistory = [operation, ...currentHistory];
    this.historySubject.next(newHistory);
    sessionStorage.setItem(this.storageKey, JSON.stringify(newHistory));
  }

  updateOperation(id: string, updatedData: Partial<ImportExportHistory>) {
    const currentHistory = this.getHistory();
    const operationIndex = currentHistory.findIndex(op => op.id === id);
    if (operationIndex > -1) {
      const updatedOperation = { ...currentHistory[operationIndex], ...updatedData };
      const newHistory = [...currentHistory];
      newHistory[operationIndex] = updatedOperation;
      this.historySubject.next(newHistory);
      sessionStorage.setItem(this.storageKey, JSON.stringify(newHistory));
    }
  }

  removeOperation(id: string) {
    const currentHistory = this.getHistory();
    const newHistory = currentHistory.filter(op => op.id !== id);
    this.historySubject.next(newHistory);
    sessionStorage.setItem(this.storageKey, JSON.stringify(newHistory));
  }

  removeOperationByChangeRequestId(changeRequestId: string) {
    const currentHistory = this.getHistory();
    const operationToRemove = currentHistory.find(op => op.changeRequestId === changeRequestId);
    if (operationToRemove) {
      this.removeOperation(operationToRemove.id);
    }
  }
}
