import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class CommandPaletteService {
  private isOpenSubject = new BehaviorSubject<boolean>(false);
  public isOpen$: Observable<boolean> = this.isOpenSubject.asObservable();
  
  constructor() {}
  
  open(): void {
    this.isOpenSubject.next(true);
  }
  
  close(): void {
    this.isOpenSubject.next(false);
  }
  
  toggle(): void {
    this.isOpenSubject.next(!this.isOpenSubject.value);
  }
  
  isOpen(): boolean {
    return this.isOpenSubject.value;
  }
}