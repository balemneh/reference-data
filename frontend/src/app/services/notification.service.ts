import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

export interface Notification {
  id: number;
  title: string;
  message: string;
  time: string;
  read: boolean;
  type: 'info' | 'success' | 'warning' | 'error';
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  private notifications: Notification[] = [
    {
      id: 1,
      title: 'New country code added',
      message: 'Country code "ZZ" has been added to ISO3166-1. Review and approve the change.',
      time: '2 hours ago',
      read: false,
      type: 'info'
    },
    {
      id: 2,
      title: 'Change request approved',
      message: 'Your request to update United States data has been approved. The changes are now live.',
      time: '1 day ago',
      read: false,
      type: 'success'
    },
    {
      id: 3,
      title: 'System maintenance scheduled',
      message: 'Planned maintenance window scheduled for Sunday 2AM-4AM EST. Service may be intermittently unavailable.',
      time: '2 days ago',
      read: true,
      type: 'warning'
    },
    {
      id: 4,
      title: 'Data import complete',
      message: 'Your data import of 1,234 records for "Ports of Entry" has completed successfully. View the new data.',
      time: '3 days ago',
      read: true,
      type: 'success'
    },
    {
      id: 5,
      title: 'Data import failed',
      message: 'Your data import of 567 records for "Airports" has failed. Please check the import logs for more details.',
      time: '3 days ago',
      read: true,
      type: 'error'
    },
    {
      id: 6,
      title: 'New user registered',
      message: 'A new user, John Doe, has registered. Please review their access permissions.',
      time: '4 days ago',
      read: false,
      type: 'info'
    },
    {
      id: 7,
      title: 'Security alert',
      message: 'Unusual activity detected from IP address 192.168.1.100. Investigate immediately.',
      time: '5 days ago',
      read: true,
      type: 'error'
    },
    {
      id: 8,
      title: 'Report generated',
      message: 'The monthly "Data Usage" report is now available for download.',
      time: '1 week ago',
      read: true,
      type: 'info'
    },
    {
      id: 9,
      title: 'Application update available',
      message: 'Version 2.1.0 of the Reference Data Service is now available. It includes new features and bug fixes.',
      time: '1 week ago',
      read: false,
      type: 'info'
    },
    {
      id: 10,
      title: 'Change request pending',
      message: 'A change request for "Currencies" is awaiting your approval.',
      time: '1 week ago',
      read: false,
      type: 'warning'
    }
  ];

  constructor() { }

  getAllNotifications(): Observable<Notification[]> {
    return of(this.notifications);
  }

  getRecentNotifications(): Observable<Notification[]> {
    return of(this.notifications.slice(0, 3));
  }
}
