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

interface SupportChat {
  id: number;
  userName: string;
  userEmail: string;
  userType: 'passenger' | 'driver';
  status: 'pending' | 'active' | 'resolved';
  subject: string;
  lastMessage: string;
  lastMessageTime: Date;
  unreadCount: number;
  messages: ChatMessage[];
}

@Component({
  selector: 'app-admin-support',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-support.page.html',
})
export class AdminSupportPage {
  searchQuery = '';
  activeFilter: 'all' | 'pending' | 'active' | 'resolved' = 'all';
  selectedChat: SupportChat | null = null;
  newMessage = '';

  chats: SupportChat[] = [
    {
      id: 1,
      userName: 'Sarah Johnson',
      userEmail: 'sarah.j@email.com',
      userType: 'passenger',
      status: 'pending',
      subject: 'Refund request for overcharged ride',
      lastMessage: 'TextTextTextTextTextText',
      lastMessageTime: new Date(Date.now() - 2 * 60 * 1000), // 2 min ago
      unreadCount: 2,
      messages: [
        {
          id: 1,
          sender: 'support',
          senderName: 'Support Agent',
          message: 'TextTextTextTextTextTextTextTextTextTextTextTextTextText TextTextTextText',
          timestamp: new Date(2026, 1, 4, 11, 31)
        },
        {
          id: 2,
          sender: 'support',
          senderName: 'Support Agent',
          message: 'TextTextTextTextTextTextTextTextTextTextTextTextTextText TextTextTextText',
          timestamp: new Date(2026, 1, 4, 11, 33)
        },
        {
          id: 3,
          sender: 'user',
          senderName: 'Sarah Johnson',
          message: 'TextTextTextTextTextTextTextTextTextTextTextText',
          timestamp: new Date(2026, 1, 4, 11, 35)
        },
        {
          id: 4,
          sender: 'user',
          senderName: 'Sarah Johnson',
          message: 'TextTextTextTextText',
          timestamp: new Date(2026, 1, 4, 11, 48)
        }
      ]
    },
    {
      id: 2,
      userName: 'James Wilson',
      userEmail: 'james.w@email.com',
      userType: 'driver',
      status: 'active',
      subject: 'Payment not received',
      lastMessage: 'TextTextTextTextTextText',
      lastMessageTime: new Date(Date.now() - 15 * 60 * 1000), // 15 min ago
      unreadCount: 1,
      messages: [
        {
          id: 1,
          sender: 'user',
          senderName: 'James Wilson',
          message: 'I haven\'t received my payment for last week\'s rides.',
          timestamp: new Date(2026, 1, 4, 10, 15)
        },
        {
          id: 2,
          sender: 'support',
          senderName: 'Support Agent',
          message: 'Let me check your account. Can you provide your driver ID?',
          timestamp: new Date(2026, 1, 4, 10, 20)
        }
      ]
    },
    {
      id: 3,
      userName: 'Michael Chen',
      userEmail: 'michael.c@email.com',
      userType: 'passenger',
      status: 'resolved',
      subject: 'Lost item in vehicle',
      lastMessage: 'TextTextTextTextTextText',
      lastMessageTime: new Date(Date.now() - 60 * 60 * 1000), // 1 hour ago
      unreadCount: 0,
      messages: [
        {
          id: 1,
          sender: 'user',
          senderName: 'Michael Chen',
          message: 'I left my bag in the car during my last ride.',
          timestamp: new Date(2026, 1, 4, 9, 0)
        },
        {
          id: 2,
          sender: 'support',
          senderName: 'Support Agent',
          message: 'We\'ve contacted the driver and arranged for the return of your item.',
          timestamp: new Date(2026, 1, 4, 9, 30)
        }
      ]
    },
    {
      id: 4,
      userName: 'Emily Rodriguez',
      userEmail: 'emily.r@email.com',
      userType: 'passenger',
      status: 'pending',
      subject: 'App not working properly',
      lastMessage: 'The app keeps crashing when I try to book',
      lastMessageTime: new Date(Date.now() - 5 * 60 * 1000), // 5 min ago
      unreadCount: 1,
      messages: [
        {
          id: 1,
          sender: 'user',
          senderName: 'Emily Rodriguez',
          message: 'The app keeps crashing when I try to book a ride.',
          timestamp: new Date(2026, 1, 4, 11, 55)
        }
      ]
    }
  ];

  get filteredChats(): SupportChat[] {
    let filtered = this.chats;
    
    if (this.activeFilter !== 'all') {
      filtered = filtered.filter(chat => chat.status === this.activeFilter);
    }
    
    if (this.searchQuery.trim()) {
      const query = this.searchQuery.toLowerCase();
      filtered = filtered.filter(chat => 
        chat.userName.toLowerCase().includes(query) ||
        chat.userEmail.toLowerCase().includes(query) ||
        chat.subject.toLowerCase().includes(query)
      );
    }
    
    return filtered;
  }

  get filterCounts(): { all: number; pending: number; active: number; resolved: number } {
    return {
      all: this.chats.length,
      pending: this.chats.filter(c => c.status === 'pending').length,
      active: this.chats.filter(c => c.status === 'active').length,
      resolved: this.chats.filter(c => c.status === 'resolved').length
    };
  }

  selectChat(chat: SupportChat): void {
    this.selectedChat = chat;
    // Mark as read
    chat.unreadCount = 0;
  }

  formatTime(date: Date): string {
    return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
  }

  formatRelativeTime(date: Date): string {
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins} min ago`;
    if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
    return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
  }

  markResolved(): void {
    if (this.selectedChat) {
      this.selectedChat.status = 'resolved';
    }
  }

  sendMessage(): void {
    if (!this.newMessage.trim() || !this.selectedChat) return;
    
    this.selectedChat.messages.push({
      id: this.selectedChat.messages.length + 1,
      sender: 'support',
      senderName: 'Support Agent',
      message: this.newMessage.trim(),
      timestamp: new Date()
    });
    
    this.selectedChat.lastMessage = this.newMessage.trim();
    this.selectedChat.lastMessageTime = new Date();
    
    // Change status to active if pending
    if (this.selectedChat.status === 'pending') {
      this.selectedChat.status = 'active';
    }
    
    this.newMessage = '';
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }
}
