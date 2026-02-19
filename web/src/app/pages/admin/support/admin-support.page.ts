import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { 
  SupportChatService, 
  SupportChatListItemResponse, 
  SupportChatResponse, 
  SupportMessageResponse 
} from '../../../infrastructure/rest/support-chat.service';
import { SocketService } from '../../../infrastructure/rest/socket.service';

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
export class AdminSupportPage implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;

  searchQuery = '';
  selectedChat: SupportChat | null = null;
  newMessage = '';
  chats: SupportChat[] = [];
  isLoading = true;
  isLoadingChat = false;
  error: string | null = null;
  private shouldScrollToBottom = false;

  private subscriptions: Subscription[] = [];
  private chatUpdateSubscription: Subscription | null = null;
  private messageSubscription: Subscription | null = null;

  constructor(
    private supportChatService: SupportChatService,
    private socketService: SocketService,
    private cdr: ChangeDetectorRef,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.loadChats();
    this.subscribeToUpdates();
  }

  ngAfterViewChecked(): void {
    if (this.shouldScrollToBottom) {
      this.scrollToBottom();
      this.shouldScrollToBottom = false;
    }
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.chatUpdateSubscription?.unsubscribe();
    this.messageSubscription?.unsubscribe();
  }

  private loadChats(): void {
    this.isLoading = true;
    this.error = null;

    const sub = this.supportChatService.getAllChats().subscribe({
      next: (chats: SupportChatListItemResponse[]) => {
        this.chats = chats.map(c => this.mapChatListItem(c));
        this.isLoading = false;
        this.cdr.detectChanges();
        
        // Check for chatId query param to auto-select chat
        this.route.queryParams.subscribe(params => {
          const chatId = params['chatId'];
          if (chatId) {
            const targetChat = this.chats.find(c => c.id === +chatId);
            if (targetChat) {
              this.selectChat(targetChat);
            }
          }
        });
      },
      error: (err) => {
        console.error('Error loading chats:', err);
        this.error = 'Failed to load support chats. Please try again.';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
    this.subscriptions.push(sub);
  }

  private subscribeToUpdates(): void {
    // Subscribe to chat list updates
    this.chatUpdateSubscription = this.socketService.subscribeToAdminChatUpdates().subscribe({
      next: (update: any) => {
        this.handleChatListUpdate(update);
      },
      error: (err) => console.error('Chat update subscription error:', err)
    });

    // Subscribe to new messages
    this.messageSubscription = this.socketService.subscribeToAdminMessages().subscribe({
      next: (message: SupportMessageResponse) => {
        this.handleNewMessage(message);
      },
      error: (err) => console.error('Message subscription error:', err)
    });
  }

  private handleChatListUpdate(update: SupportChatListItemResponse): void {
    const index = this.chats.findIndex(c => c.id === update.id);
    const updatedChat = this.mapChatListItem(update);

    if (index >= 0) {
      // Preserve existing messages if this is the selected chat
      if (this.selectedChat?.id === update.id) {
        updatedChat.messages = this.selectedChat.messages;
        this.selectedChat = updatedChat;
      }
      this.chats[index] = updatedChat;
    } else {
      // New chat - add to beginning
      this.chats.unshift(updatedChat);
    }

    // Re-sort by last message time
    this.chats.sort((a, b) => b.lastMessageTime.getTime() - a.lastMessageTime.getTime());
    this.cdr.detectChanges();
  }

  private handleNewMessage(message: SupportMessageResponse): void {
    // If this message is for the selected chat, add it
    if (this.selectedChat && message.chatId === this.selectedChat.id) {
      if (!this.selectedChat.messages.some(m => m.id === message.id)) {
        this.selectedChat.messages.push(this.mapMessage(message));
        this.shouldScrollToBottom = true;
        this.cdr.detectChanges();
      }
    }
  }

  private mapChatListItem(item: SupportChatListItemResponse): SupportChat {
    return {
      id: item.id,
      userName: item.userName,
      userEmail: item.userEmail,
      userType: item.userRole === 'DRIVER' ? 'driver' : 'passenger',
      lastMessage: item.lastMessage || '',
      lastMessageTime: new Date(item.lastMessageTime),
      unreadCount: item.unreadCount,
      messages: []
    };
  }

  private mapMessage(m: SupportMessageResponse): ChatMessage {
    return {
      id: m.id,
      sender: m.fromAdmin ? 'support' : 'user',
      senderName: m.senderName,
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

  get filteredChats(): SupportChat[] {
    if (!this.searchQuery.trim()) {
      return this.chats;
    }
    
    const query = this.searchQuery.toLowerCase();
    return this.chats.filter(chat => 
      chat.userName.toLowerCase().includes(query) ||
      chat.userEmail.toLowerCase().includes(query)
    );
  }

  get totalUnreadCount(): number {
    return this.chats.reduce((sum, chat) => sum + chat.unreadCount, 0);
  }

  selectChat(chat: SupportChat): void {
    if (this.selectedChat?.id === chat.id) return;

    this.isLoadingChat = true;
    
    const sub = this.supportChatService.getChatById(chat.id).subscribe({
      next: (fullChat: SupportChatResponse) => {
        this.selectedChat = {
          ...chat,
          messages: (fullChat.messages || []).map(m => this.mapMessage(m))
        };
        this.isLoadingChat = false;
        this.shouldScrollToBottom = true;
        this.cdr.detectChanges();

        // Mark as read
        if (chat.unreadCount > 0) {
          this.supportChatService.markChatAsRead(chat.id).subscribe();
          chat.unreadCount = 0;
        }
      },
      error: (err) => {
        console.error('Error loading chat:', err);
        this.isLoadingChat = false;
        this.cdr.detectChanges();
      }
    });
    this.subscriptions.push(sub);
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

  sendMessage(): void {
    if (!this.newMessage.trim() || !this.selectedChat) return;

    const messageContent = this.newMessage.trim();
    const chatId = this.selectedChat.id;
    this.newMessage = '';

    const sub = this.supportChatService.sendAdminMessage(chatId, messageContent).subscribe({
      next: (response: SupportMessageResponse) => {
        // Message will be added via WebSocket subscription
        // But add immediately for responsiveness if not already present
        if (this.selectedChat && !this.selectedChat.messages.some(m => m.id === response.id)) {
          this.selectedChat.messages.push(this.mapMessage(response));
          this.shouldScrollToBottom = true;
          this.cdr.detectChanges();
        }

        // Update chat list item
        const chatIndex = this.chats.findIndex(c => c.id === chatId);
        if (chatIndex >= 0) {
          this.chats[chatIndex].lastMessage = messageContent;
          this.chats[chatIndex].lastMessageTime = new Date();
        }
      },
      error: (err) => {
        console.error('Error sending message:', err);
        // Restore message on error
        this.newMessage = messageContent;
      }
    });
    this.subscriptions.push(sub);
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }
}
