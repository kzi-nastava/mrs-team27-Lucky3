import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './stat-card.component.html'
})
export class StatCardComponent {
  @Input() title: string = '';
  @Input() value: string | number = '';
  @Input() iconBgColor: string = 'bg-gray-800';
  @Input() iconColor: string = 'text-white';
  // We will use content projection or an input for the icon SVG
}
