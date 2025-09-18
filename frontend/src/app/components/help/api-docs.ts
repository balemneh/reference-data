import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-help-api-docs',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="grid-container">
      <h1 class="margin-top-2">API Documentation</h1>
      <p class="text-base">OpenAPI and integration guides coming soon.</p>
    </div>
  `
})
export class HelpApiDocsComponent {}

