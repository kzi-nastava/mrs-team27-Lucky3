export type NotificationType = 'panic' | 'ride' | 'support' | 'system';

export interface AppNotification {
  id: string;
  type: NotificationType;
  title: string;
  message: string;
  timestamp: string;
  read: boolean;
  route?: string;
  data?: Record<string, any>;
}
