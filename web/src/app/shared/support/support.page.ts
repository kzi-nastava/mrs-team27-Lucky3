import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

interface ChatMessage {
  id: number;
  sender: 'user' | 'support';
  senderName: string;
  message: string;
  timestamp: Date;
}

@Component({
  selector: 'app-support',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './support.page.html',
})
export class SupportPage {
  newMessage = '';
  
  messages: ChatMessage[] = [
    {
      id: 1,
      sender: 'user',
      senderName: 'You',
      message: 'Hello, I have a question about my recent ride',
      timestamp: new Date(2026, 1, 4, 15, 30)
    },
    {
      id: 2,
      sender: 'support',
      senderName: 'Support Team',
      message: 'Hello! I\'d be happy to help you. Could you please provide your ride ID?',
      timestamp: new Date(2026, 1, 4, 15, 31)
    },
    {
      id: 3,
      sender: 'user',
      senderName: 'You',
      message: 'Yes, it\'s Ride#1234',
      timestamp: new Date(2026, 1, 4, 15, 32)
    },
    {
      id: 4,
      sender: 'support',
      senderName: 'Support Team',
      message: 'Thank you. I can see your ride from December 21st. What seems to be the issue?',
      timestamp: new Date(2026, 1, 4, 15, 33)
    },
    {
      id: 5,
      sender: 'user',
      senderName: 'You',
      message: 'The driver took a longer route than necessary and I was charged more',
      timestamp: new Date(2026, 1, 4, 15, 34)
    }
  ];

  formatTime(date: Date): string {
    return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
  }

  sendMessage(): void {
    if (!this.newMessage.trim()) return;
    
    this.messages.push({
      id: this.messages.length + 1,
      sender: 'user',
      senderName: 'You',
      message: this.newMessage.trim(),
      timestamp: new Date()
    });
    
    this.newMessage = '';
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }
}
