import { Injectable } from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ChangeRequestService {
  private pendingRequestsCount = new BehaviorSubject<number>(0);
  pendingRequestsCount$ = this.pendingRequestsCount.asObservable();

  private changeRequestCreated = new BehaviorSubject<number>(0);
  changeRequestCreated$ = this.changeRequestCreated.asObservable();

  updatePendingRequestsCount(count: number) {
    this.pendingRequestsCount.next(count);
  }

  notifyChangeRequestCreated() {
    this.changeRequestCreated.next(this.changeRequestCreated.value + 1);
  }
}
