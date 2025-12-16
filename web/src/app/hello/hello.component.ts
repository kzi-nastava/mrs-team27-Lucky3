import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-hello',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="p-4 max-w-md mx-auto bg-white rounded-xl shadow-md space-y-4">
      <h2 class="text-xl font-bold text-gray-900">Hello Component</h2>
      <p class="text-gray-500">This is a basic GET route example.</p>
      <button
        class="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-700 transition"
        (click)="showNotification()">
        Click Me
      </button>

      @if (notification()) {
        <div class="bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded mt-4">
          {{ notification() }}
        </div>
      }
    </div>
  `
})
export class HelloComponent {
  notification = signal<string>('');

  showNotification() {
    this.notification.set('Button clicked! 🎉');
  }
}

