import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Notification, NotificationService } from '../../services/notification.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="grid-container">
      <h1 class="margin-top-2 margin-left-2">Notification Center</h1>
      <div class="usa-list">
        <ng-container *ngIf="notifications$ | async as notifications">
          <div *ngIf="notifications.length === 0" class="usa-card margin-bottom-2">
            <div class="usa-card__container">
              <div class="usa-card__body text-center">
                <p>No notifications to display.</p>
              </div>
            </div>
          </div>
          <div *ngFor="let notification of notifications" class="usa-card margin-bottom-2">
            <div class="usa-card__container">
              <div class="usa-card__header">
                <h2 class="usa-card__heading">{{ notification.title }}</h2>
              </div>
              <div class="usa-card__body">
                <p>{{ notification.message }}</p>
              </div>
              <div class="usa-card__footer">
                <span class="text-base">{{ notification.time }}</span>
              </div>
            </div>
          </div>
        </ng-container>
      </div>
    </div>
  `,
})
export class NotificationsComponent implements OnInit {
  notifications$!: Observable<Notification[]>;

  constructor(private notificationService: NotificationService) {}

  ngOnInit(): void {
    this.notifications$ = this.notificationService.getAllNotifications();
  }
}

