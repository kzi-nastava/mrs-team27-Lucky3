import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BlockUserRequest, BlockUserResponse } from '../../model/user-blocking.model';
import { UserProfile } from "../../infrastructure/rest/user.service";

@Injectable({ providedIn: 'root' })
export class UserBlockingApiService {
  private readonly baseUrl = '/api/admin/users';

  constructor(private http: HttpClient) {}

  blockUser(req: BlockUserRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/block`, req);
  }

  unblockUser(userId: number): Observable<BlockUserResponse> {
    return this.http.post<BlockUserResponse>(`${this.baseUrl}/unblock/${userId}`, {});
  }

  getBlockedUsers(): Observable<UserProfile[]> {
    return this.http.get<UserProfile[]>(`${this.baseUrl}/blocked`);
  }

  // You need SOME way to get unblocked users.
  // Option A (recommended): add backend endpoint GET /api/admin/users/unblocked
  getUnblockedUsers(): Observable<UserProfile[]> {
    return this.http.get<UserProfile[]>(`${this.baseUrl}/unblocked`);
  }

  // Option B: if you already have GET /api/admin/users (all users), fetch & filter on frontend.
}
