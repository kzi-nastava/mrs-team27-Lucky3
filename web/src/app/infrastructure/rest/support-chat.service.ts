import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../env/environment';

/**
 * Response DTO for a single support chat message.
 */
export interface SupportMessageResponse {
  id: number;
  chatId: number;
  senderId: number;
  senderName: string;
  content: string;
  timestamp: string;
  fromAdmin: boolean;
}

/**
 * Response DTO for a support chat (with messages).
 */
export interface SupportChatResponse {
  id: number;
  userId: number;
  userName: string;
  userEmail: string;
  userRole: 'PASSENGER' | 'DRIVER' | 'ADMIN';
  lastMessage: string;
  lastMessageTime: string;
  unreadCount: number;
  createdAt: string;
  messages: SupportMessageResponse[];
}

/**
 * Lightweight response DTO for support chat list (without messages).
 */
export interface SupportChatListItemResponse {
  id: number;
  userId: number;
  userName: string;
  userEmail: string;
  userRole: 'PASSENGER' | 'DRIVER' | 'ADMIN';
  lastMessage: string;
  lastMessageTime: string;
  unreadCount: number;
}

/**
 * Request DTO for sending a support message.
 */
export interface SupportMessageRequest {
  content: string;
}

/**
 * Service for Support Chat REST API operations.
 */
@Injectable({
  providedIn: 'root'
})
export class SupportChatService {
  private readonly baseUrl = `${environment.apiHost}support`;

  constructor(private http: HttpClient) {}

  // ==================== User Endpoints ====================

  /**
   * Get current user's support chat.
   * Creates a new chat if one doesn't exist.
   */
  getMyChat(): Observable<SupportChatResponse> {
    return this.http.get<SupportChatResponse>(`${this.baseUrl}/chat`);
  }

  /**
   * Send a message to support as a user.
   */
  sendUserMessage(content: string): Observable<SupportMessageResponse> {
    const request: SupportMessageRequest = { content };
    return this.http.post<SupportMessageResponse>(`${this.baseUrl}/chat/message`, request);
  }

  // ==================== Admin Endpoints ====================

  /**
   * Get all support chats for admin view.
   * Returns chats ordered by last message time (newest first).
   */
  getAllChats(): Observable<SupportChatListItemResponse[]> {
    return this.http.get<SupportChatListItemResponse[]>(`${this.baseUrl}/admin/chats`);
  }

  /**
   * Get a specific chat with all messages (admin only).
   */
  getChatById(chatId: number): Observable<SupportChatResponse> {
    return this.http.get<SupportChatResponse>(`${this.baseUrl}/admin/chat/${chatId}`);
  }

  /**
   * Get messages for a specific chat (admin only).
   */
  getChatMessages(chatId: number): Observable<SupportMessageResponse[]> {
    return this.http.get<SupportMessageResponse[]>(`${this.baseUrl}/admin/chat/${chatId}/messages`);
  }

  /**
   * Send a message to a user's support chat as admin.
   */
  sendAdminMessage(chatId: number, content: string): Observable<SupportMessageResponse> {
    const request: SupportMessageRequest = { content };
    return this.http.post<SupportMessageResponse>(`${this.baseUrl}/admin/chat/${chatId}/message`, request);
  }

  /**
   * Mark a chat as read (resets unread count).
   */
  markChatAsRead(chatId: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/admin/chat/${chatId}/read`, {});
  }
}
