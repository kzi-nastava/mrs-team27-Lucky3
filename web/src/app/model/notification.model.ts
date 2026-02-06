export type NotificationType = 'panic' | 'ride' | 'support' | 'system' | 'driver_assignment' | 'ride_invite' | 'ride_finished' | 'ride_created' | 'ride_cancelled' | 'ride_scheduled_reminder';

export interface AppNotification {
  id: string;
  type: NotificationType;
  title: string;
  message: string;
  timestamp: string;
  read: boolean;
  route?: string;
  data?: Record<string, any>;
  /** "NORMAL" or "CRITICAL" — CRITICAL triggers siren sound */
  priority?: string;
}

/** Shape of the DTO sent by the backend (REST + WebSocket). */
export interface BackendNotificationResponse {
  id: number;
  text: string;
  timestamp: string;
  type: 'RIDE_STATUS' | 'RIDE_INVITE' | 'PANIC' | 'SUPPORT' | 'DRIVER_ASSIGNMENT' | 'RIDE_FINISHED' | 'RIDE_CREATED' | 'RIDE_CANCELLED' | 'RIDE_SCHEDULED_REMINDER';
  recipientId: number;
  recipientName: string;
  read: boolean;
  relatedEntityId: number | null;
  priority: 'NORMAL' | 'CRITICAL';
}
