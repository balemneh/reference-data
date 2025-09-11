import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-help-user-guide',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="grid-container">
      <h1 class="margin-top-2">User Guide</h1>
      <p class="text-base">Documentation coming soon.</p>
    </div>
  `
})
export class HelpUserGuideComponent {}

