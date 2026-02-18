
export interface BlockUserRequest {
  email: string;
  reason: string;
}

export interface BlockUserResponse {
  userId: number;
  email: string;
  name: string;
  surname: string;
  isBlocked: boolean;
  blockReason: string;
  message: string;
}
