import { Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { environment } from '../../../env/environment';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { SupportMessageResponse } from './support-chat.service';
import { PanicResponse } from './panic.service';
import { BackendNotificationResponse } from '../../model/notification.model';

export interface SocketState {
  connected: boolean;
  error: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class SocketService implements OnDestroy {
  private client: Client | null = null;
  private socketState = new BehaviorSubject<SocketState>({ connected: false, error: null });

  // Observable streams for different topics
  private vehiclesSubject = new BehaviorSubject<any[]>([]);

  // Support chat subjects
  private supportChatMessageSubject = new BehaviorSubject<SupportMessageResponse | null>(null);
  private adminChatListUpdateSubject = new BehaviorSubject<any | null>(null);
  private adminNewMessageSubject = new BehaviorSubject<SupportMessageResponse | null>(null);

  // Active subscriptions for cleanup
  private supportChatSubscription: StompSubscription | null = null;
  private adminChatsSubscription: StompSubscription | null = null;
  private adminMessagesSubscription: StompSubscription | null = null;
  private panicSubscription: StompSubscription | null = null;
  private userNotificationSubscription: StompSubscription | null = null;

  constructor() {}

  /**
   * Get JWT token from localStorage
   */
  private getToken(): string | null {
    const userJson = localStorage.getItem('user');
    if (userJson) {
      try {
        const user = JSON.parse(userJson);
        return user.accessToken || null;
      } catch {
        return null;
      }
    }
    return null;
  }

  /**
   * Connect to the WebSocket server with JWT authentication
   */
  connect(): void {
    if (this.client?.connected) {
      return; // Already connected
    }
    
    // If client exists but not connected, check if it's activating
    if (this.client?.active) {
      return;
    }

    const token = this.getToken();

    this.client = new Client({
      webSocketFactory: () => new SockJS(environment.wsHost),
      connectHeaders: token ? { 'Authorization': `Bearer ${token}` } : {},
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (str) => {
        // Uncomment for debugging: console.log('STOMP: ' + str);
      }
    });

    this.client.onConnect = () => {
      console.log('WebSocket connected');
      this.socketState.next({ connected: true, error: null });
      
      // Subscribe to vehicle updates
      this.subscribeToVehicles();
    };

    this.client.onStompError = (frame) => {
      console.error('STOMP error:', frame.headers['message']);
      this.socketState.next({ connected: false, error: frame.headers['message'] });
    };

    this.client.onDisconnect = () => {
      console.log('WebSocket disconnected');
      this.socketState.next({ connected: false, error: null });
    };

    this.client.activate();
  }

  /**
   * Disconnect from the WebSocket server
   */
  disconnect(): void {
    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
  }

  /**
   * Subscribe to vehicle location updates
   */
  private subscribeToVehicles(): void {
    if (!this.client?.connected) return;

    this.client.subscribe('/topic/vehicles', (message: IMessage) => {
      try {
        const vehicles = JSON.parse(message.body);
        this.vehiclesSubject.next(vehicles);
      } catch (e) {
        console.error('Error parsing vehicle data:', e);
      }
    });
  }

  /**
   * Get observable for vehicle updates
   */
  getVehicleUpdates(): Observable<any[]> {
    return this.vehiclesSubject.asObservable();
  }

  /**
   * Subscribe to location updates for a specific vehicle
   * @param vehicleId The ID of the vehicle to track
   */
  getVehicleLocationUpdates(vehicleId: number): Observable<any> {
    return new Observable(observer => {
      let stompSub: { unsubscribe: () => void } | null = null;
      let stateSub: { unsubscribe: () => void } | null = null;
      
      const subscribeToStomp = () => {
        if (!this.client?.connected) {
          return;
        }
        
        try {
          stompSub = this.client.subscribe(`/topic/vehicle/${vehicleId}`, (message: IMessage) => {
            try {
              const location = JSON.parse(message.body);
              observer.next(location);
            } catch (e) {
              console.error('Error parsing vehicle location:', e);
            }
          });
        } catch (error) {
           console.error('Error subscribing to vehicle:', error);
        }
      };

      // If already connected, subscribe immediately
      if (this.client?.connected) {
        subscribeToStomp();
      } else {
        // Auto-connect if not connected
        this.connect();
        
        // Wait for connection then subscribe
        stateSub = this.socketState.subscribe(state => {
          if (state.connected && !stompSub) {
            subscribeToStomp();
          }
        });
      }

      // Cleanup function
      return () => {
        if (stompSub) {
          stompSub.unsubscribe();
          stompSub = null;
        }
        if (stateSub) {
          stateSub.unsubscribe();
          stateSub = null;
        }
      };
    });
  }

  /**
   * Subscribe to ride status updates for a specific ride.
   * @param rideId The ID of the ride to track
   */
  getRideUpdates(rideId: number): Observable<any> {
    return new Observable(observer => {
      let stompSub: { unsubscribe: () => void } | null = null;
      let stateSub: { unsubscribe: () => void } | null = null;
      
      const subscribeToStomp = () => {
        if (!this.client?.connected) {
          return;
        }
        
        try {
          stompSub = this.client.subscribe(`/topic/ride/${rideId}`, (message: IMessage) => {
            try {
              const rideUpdate = JSON.parse(message.body);
              observer.next(rideUpdate);
            } catch (e) {
              console.error('Error parsing ride update:', e);
            }
          });
        } catch (error) {
           console.error('Error subscribing to ride updates:', error);
        }
      };

      // If already connected, subscribe immediately
      if (this.client?.connected) {
        subscribeToStomp();
      } else {
        // Auto-connect if not connected
        this.connect();
        
        // Wait for connection then subscribe
        stateSub = this.socketState.subscribe(state => {
          if (state.connected && !stompSub) {
            subscribeToStomp();
          }
        });
      }

      // Cleanup function
      return () => {
        if (stompSub) {
          stompSub.unsubscribe();
          stompSub = null;
        }
        if (stateSub) {
          stateSub.unsubscribe();
          stateSub = null;
        }
      };
    });
  }

  /**
   * Get connection state observable
   */
  getConnectionState(): Observable<SocketState> {
    return this.socketState.asObservable();
  }

  /**
   * Check if currently connected
   */
  isConnected(): boolean {
    return this.client?.connected ?? false;
  }

  // ==================== Support Chat Methods ====================

  /**
   * Subscribe to messages for a specific support chat (for users).
   * @param chatId The support chat ID
   */
  subscribeToSupportChat(chatId: number): Observable<SupportMessageResponse> {
    return new Observable(observer => {
      let stompSub: StompSubscription | null = null;
      let stateSub: Subscription | null = null;

      const subscribeToStomp = () => {
        if (!this.client?.connected) return;

        try {
          // Unsubscribe from previous chat if any
          if (this.supportChatSubscription) {
            this.supportChatSubscription.unsubscribe();
          }

          stompSub = this.client.subscribe(`/topic/support/chat/${chatId}`, (message: IMessage) => {
            try {
              const msg: SupportMessageResponse = JSON.parse(message.body);
              observer.next(msg);
            } catch (e) {
              console.error('Error parsing support message:', e);
            }
          });
          this.supportChatSubscription = stompSub;
        } catch (error) {
          console.error('Error subscribing to support chat:', error);
        }
      };

      // If already connected, subscribe immediately
      if (this.client?.connected) {
        subscribeToStomp();
      } else {
        // Auto-connect if not connected
        this.connect();
        
        // Wait for connection then subscribe
        stateSub = this.socketState.subscribe(state => {
          if (state.connected && !stompSub) {
            subscribeToStomp();
          }
        });
      }

      // Cleanup function
      return () => {
        if (stompSub) {
          stompSub.unsubscribe();
          this.supportChatSubscription = null;
        }
        if (stateSub) {
          stateSub.unsubscribe();
        }
      };
    });
  }

  /**
   * Subscribe to admin chat list updates.
   * Receives updates when new messages arrive or unread counts change.
   */
  subscribeToAdminChatUpdates(): Observable<any> {
    return new Observable(observer => {
      let stompSub: StompSubscription | null = null;
      let stateSub: Subscription | null = null;

      const subscribeToStomp = () => {
        if (!this.client?.connected) return;

        try {
          // Unsubscribe from previous subscription if any
          if (this.adminChatsSubscription) {
            this.adminChatsSubscription.unsubscribe();
          }

          stompSub = this.client.subscribe('/topic/support/admin/chats', (message: IMessage) => {
            try {
              const update = JSON.parse(message.body);
              observer.next(update);
            } catch (e) {
              console.error('Error parsing admin chat update:', e);
            }
          });
          this.adminChatsSubscription = stompSub;
        } catch (error) {
          console.error('Error subscribing to admin chat updates:', error);
        }
      };

      // If already connected, subscribe immediately
      if (this.client?.connected) {
        subscribeToStomp();
      } else {
        this.connect();
        stateSub = this.socketState.subscribe(state => {
          if (state.connected && !stompSub) {
            subscribeToStomp();
          }
        });
      }

      return () => {
        if (stompSub) {
          stompSub.unsubscribe();
          this.adminChatsSubscription = null;
        }
        if (stateSub) {
          stateSub.unsubscribe();
        }
      };
    });
  }

  /**
   * Subscribe to admin new message notifications.
   * Receives all new messages across all chats for real-time updates.
   */
  subscribeToAdminMessages(): Observable<SupportMessageResponse> {
    return new Observable(observer => {
      let stompSub: StompSubscription | null = null;
      let stateSub: Subscription | null = null;

      const subscribeToStomp = () => {
        if (!this.client?.connected) return;

        try {
          if (this.adminMessagesSubscription) {
            this.adminMessagesSubscription.unsubscribe();
          }

          stompSub = this.client.subscribe('/topic/support/admin/messages', (message: IMessage) => {
            try {
              const msg: SupportMessageResponse = JSON.parse(message.body);
              observer.next(msg);
            } catch (e) {
              console.error('Error parsing admin message notification:', e);
            }
          });
          this.adminMessagesSubscription = stompSub;
        } catch (error) {
          console.error('Error subscribing to admin messages:', error);
        }
      };

      if (this.client?.connected) {
        subscribeToStomp();
      } else {
        this.connect();
        stateSub = this.socketState.subscribe(state => {
          if (state.connected && !stompSub) {
            subscribeToStomp();
          }
        });
      }

      return () => {
        if (stompSub) {
          stompSub.unsubscribe();
          this.adminMessagesSubscription = null;
        }
        if (stateSub) {
          stateSub.unsubscribe();
        }
      };
    });
  }

  ngOnDestroy(): void {
    this.disconnect();
  }

  // ==================== Panic Alert Methods ====================

  /**
   * Subscribe to real-time panic alerts (for admin pages).
   * Receives new panic events as they happen.
   */
  subscribeToPanicAlerts(): Observable<PanicResponse> {
    return new Observable(observer => {
      let stompSub: StompSubscription | null = null;
      let stateSub: Subscription | null = null;

      const subscribeToStomp = () => {
        if (!this.client?.connected) return;

        try {
          // Unsubscribe from previous subscription if any
          if (this.panicSubscription) {
            this.panicSubscription.unsubscribe();
          }

          stompSub = this.client.subscribe('/topic/panic', (message: IMessage) => {
            try {
              const panic: PanicResponse = JSON.parse(message.body);
              observer.next(panic);
            } catch (e) {
              console.error('Error parsing panic alert:', e);
            }
          });
          this.panicSubscription = stompSub;
        } catch (error) {
          console.error('Error subscribing to panic alerts:', error);
        }
      };

      // If already connected, subscribe immediately
      if (this.client?.connected) {
        subscribeToStomp();
      } else {
        this.connect();
        stateSub = this.socketState.subscribe(state => {
          if (state.connected && !stompSub) {
            subscribeToStomp();
          }
        });
      }

      return () => {
        if (stompSub) {
          stompSub.unsubscribe();
          this.panicSubscription = null;
        }
        if (stateSub) {
          stateSub.unsubscribe();
        }
      };
    });
  }

  /**
   * Subscribe to the per-user notification queue.
   * Backend pushes to /user/{userId}/queue/notifications after persisting.
   */
  subscribeToUserNotifications(userId: number): Observable<BackendNotificationResponse> {
    return new Observable(observer => {
      let stompSub: StompSubscription | null = null;
      let stateSub: Subscription | null = null;

      const subscribeToStomp = () => {
        if (!this.client?.connected) return;

        try {
          if (this.userNotificationSubscription) {
            this.userNotificationSubscription.unsubscribe();
          }

          stompSub = this.client.subscribe(
            `/user/${userId}/queue/notifications`,
            (message: IMessage) => {
              try {
                const notification: BackendNotificationResponse = JSON.parse(message.body);
                observer.next(notification);
              } catch (e) {
                console.error('Error parsing user notification:', e);
              }
            }
          );
          this.userNotificationSubscription = stompSub;
        } catch (error) {
          console.error('Error subscribing to user notifications:', error);
        }
      };

      if (this.client?.connected) {
        subscribeToStomp();
      } else {
        this.connect();
        stateSub = this.socketState.subscribe(state => {
          if (state.connected && !stompSub) {
            subscribeToStomp();
          }
        });
      }

      return () => {
        if (stompSub) {
          stompSub.unsubscribe();
          this.userNotificationSubscription = null;
        }
        if (stateSub) {
          stateSub.unsubscribe();
        }
      };
    });
  }
}
