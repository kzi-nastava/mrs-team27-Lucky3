import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { SupportChatService, SupportMessageResponse, SupportChatResponse } from '../../infrastructure/rest/support-chat.service';
import { SocketService } from '../../infrastructure/rest/socket.service';

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
export class SupportPage implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;

  newMessage = '';
  messages: ChatMessage[] = [];
  chatId: number | null = null;
  isLoading = true;
  error: string | null = null;
  private shouldScrollToBottom = false;

  private socketSubscription: Subscription | null = null;
  private chatSubscription: Subscription | null = null;

  constructor(
    private supportChatService: SupportChatService,
    private socketService: SocketService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadChat();
  }

  ngAfterViewChecked(): void {
    if (this.shouldScrollToBottom) {
      this.scrollToBottom();
      this.shouldScrollToBottom = false;
    }
  }

  ngOnDestroy(): void {
    this.socketSubscription?.unsubscribe();
    this.chatSubscription?.unsubscribe();
  }

  loadChat(): void {
    this.isLoading = true;
    this.error = null;

    this.chatSubscription = this.supportChatService.getMyChat().subscribe({
      next: (chat: SupportChatResponse) => {
        this.chatId = chat.id;
        this.messages = this.mapMessages(chat.messages || []);
        this.isLoading = false;
        this.shouldScrollToBottom = true;
        this.cdr.detectChanges();

        // Subscribe to real-time updates for this chat
        this.subscribeToChat(chat.id);
      },
      error: (err) => {
        console.error('Error loading chat:', err);
        this.error = 'Failed to load support chat. Please try again.';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  private subscribeToChat(chatId: number): void {
    this.socketSubscription?.unsubscribe();
    
    this.socketSubscription = this.socketService.subscribeToSupportChat(chatId).subscribe({
      next: (message: SupportMessageResponse) => {
        // Only add if message doesn't already exist (avoid duplicates)
        if (!this.messages.some(m => m.id === message.id)) {
          this.messages.push(this.mapSingleMessage(message));
          this.shouldScrollToBottom = true;
          this.cdr.detectChanges();
        }
      },
      error: (err) => {
        console.error('WebSocket subscription error:', err);
      }
    });
  }

  private mapMessages(messages: SupportMessageResponse[]): ChatMessage[] {
    return messages.map(m => this.mapSingleMessage(m));
  }

  private mapSingleMessage(m: SupportMessageResponse): ChatMessage {
    return {
      id: m.id,
      sender: m.fromAdmin ? 'support' : 'user',
      senderName: m.fromAdmin ? 'Support Team' : 'You',
      message: m.content,
      timestamp: new Date(m.timestamp)
    };
  }

  private scrollToBottom(): void {
    try {
      if (this.messagesContainer) {
        const el = this.messagesContainer.nativeElement;
        el.scrollTop = el.scrollHeight;
      }
    } catch (err) {
      console.error('Error scrolling to bottom:', err);
    }
  }

  formatTime(date: Date): string {
    return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
  }

  sendMessage(): void {
    if (!this.newMessage.trim()) return;

    const messageContent = this.newMessage.trim();
    this.newMessage = '';

    this.supportChatService.sendUserMessage(messageContent).subscribe({
      next: (response: SupportMessageResponse) => {
        // Message will be added via WebSocket subscription
        // But add immediately for responsiveness if not already present
        if (!this.messages.some(m => m.id === response.id)) {
          this.messages.push(this.mapSingleMessage(response));
          this.shouldScrollToBottom = true;
          this.cdr.detectChanges();
        }
      },
      error: (err) => {
        console.error('Error sending message:', err);
        // Restore message on error
        this.newMessage = messageContent;
      }
    });
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }
}
