import { Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

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

  constructor() {}

  /**
   * Connect to the WebSocket server
   */
  connect(): void {
    if (this.client?.connected) {
      return; // Already connected
    }
    
    // If client exists but not connected, check if it's activating
    if (this.client?.active) {
      return;
    }
    this.client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8081/ws'),
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

  ngOnDestroy(): void {
    this.disconnect();
  }
}
