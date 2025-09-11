import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="grid-container">
      <h1 class="margin-top-2">Notifications</h1>
      <p class="text-base">Notification center is under construction.</p>
    </div>
  `
})
export class NotificationsComponent {}

