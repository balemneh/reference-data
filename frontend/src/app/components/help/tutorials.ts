import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-help-tutorials',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="grid-container">
      <h1 class="margin-top-2">Video Tutorials</h1>
      <p class="text-base">Tutorials will be embedded here soon.</p>
    </div>
  `
})
export class HelpTutorialsComponent {}

