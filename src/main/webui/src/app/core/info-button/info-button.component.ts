import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-info-button',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="info-button" [attr.aria-label]="tooltipText" role="button" tabindex="0">
      ℹ️
      <span class="info-tooltip">{{ tooltipText }}</span>
    </span>
  `,
  styleUrl: './info-button.component.scss'
})
export class InfoButtonComponent {
  @Input({ required: true }) tooltipText!: string;
}
