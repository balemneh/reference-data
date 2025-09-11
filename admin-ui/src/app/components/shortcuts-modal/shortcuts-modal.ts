import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-shortcuts-modal',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="usa-modal" [class.is-visible]="visible" *ngIf="visible" (click)="onBackdrop($event)">
      <div class="usa-modal__content" role="dialog" aria-modal="true" aria-labelledby="shortcuts-title" (click)="$event.stopPropagation()">
        <div class="usa-modal__header">
          <h2 id="shortcuts-title" class="usa-modal__heading">Keyboard Shortcuts</h2>
          <button class="usa-modal__close" (click)="close.emit()" aria-label="Close modal">
            <svg class="usa-icon" aria-hidden="true" focusable="false" role="img">
              <use xlink:href="/assets/uswds/img/sprite.svg#close"></use>
            </svg>
          </button>
        </div>
        <div class="usa-modal__main">
          <ul class="usa-list">
            <li><strong>Ctrl</strong> + <strong>K</strong>: Open command palette</li>
            <li><strong>/</strong>: Focus search</li>
            <li><strong>g</strong> then <strong>d</strong>: Go to Dashboard</li>
            <li><strong>g</strong> then <strong>c</strong>: Go to Countries</li>
          </ul>
        </div>
        <div class="usa-modal__footer">
          <button class="usa-button" (click)="close.emit()">Close</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .usa-modal.is-visible { display: block; }
    .usa-modal { display: none; position: fixed; inset: 0; background: rgba(0,0,0,0.4); z-index: 1000; }
    .usa-modal__content { max-width: 640px; margin: 10vh auto; background: #fff; border-radius: 8px; box-shadow: 0 8px 24px rgba(0,0,0,0.2); }
    .usa-modal__header, .usa-modal__footer { padding: 1rem; border-bottom: 1px solid #eee; }
    .usa-modal__footer { border-top: 1px solid #eee; border-bottom: 0; text-align: right; }
    .usa-modal__main { padding: 1rem 1.25rem; }
  `]
})
export class ShortcutsModalComponent {
  @Input() visible = false;
  @Output() close = new EventEmitter<void>();
  onBackdrop(e: Event) { this.close.emit(); }
}

