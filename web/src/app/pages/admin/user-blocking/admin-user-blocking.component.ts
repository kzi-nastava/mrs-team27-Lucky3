import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject, combineLatest, map, of, switchMap, catchError, finalize } from 'rxjs';
import { UserBlockingService } from '../../../infrastructure/rest/user-blocking.service';
import { UserProfile } from '../../../infrastructure/rest/user.service';
import { BlockUserRequest, BlockUserResponse } from '../../../model/user-blocking.model';
import { BlockUserDialogComponent } from './block-user-dialog.component';
import { NgClass, NgIf, NgFor, AsyncPipe } from '@angular/common';

@Component({
  selector: 'app-admin-user-blocking',
  templateUrl: './admin-user-blocking.component.html',
  imports: [NgClass, NgIf, NgFor, AsyncPipe],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdminUserBlockingComponent {
  displayedColumns = ['photo', 'name', 'email', 'role', 'reason', 'actions'];

  activeTab: 'unblocked' | 'blocked' = 'unblocked';

  private readonly reload$ = new BehaviorSubject<void>(undefined);
  private readonly busyEmails = new Set<string>();

  unblockedUsers$ = this.reload$.pipe(
    switchMap(() => this.api.getUnblockedUsers()),
    catchError(() => of([] as UserProfile[]))
  );

  blockedUsers$ = this.reload$.pipe(
    switchMap(() => this.api.getBlockedUsers()),
    catchError(() => of([] as UserProfile[]))
  );

  vm$ = combineLatest([this.unblockedUsers$, this.blockedUsers$]).pipe(
    map(([unblocked, blocked]) => ({ unblocked, blocked }))
  );

  constructor(
    private api: UserBlockingService,
    private dialog: MatDialog,
    private snack: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  trackByUserEmail(_: number, u: UserProfile): string {
    return u.email;
  }

  isBusy(email: string): boolean {
    return this.busyEmails.has(email);
  }

  private setBusy(email: string, busy: boolean): void {
    if (busy) this.busyEmails.add(email);
    else this.busyEmails.delete(email);
    this.cdr.markForCheck();
  }

  refresh(): void {
    this.reload$.next();
  }

  block(user: UserProfile): void {
    const ref = this.dialog.open(BlockUserDialogComponent, {
      width: '520px',
      data: { user }
    });

    ref.afterClosed().subscribe((reason: string | undefined) => {
      const trimmed = (reason ?? '').trim();
      if (!trimmed) return;

      this.setBusy(user.email, true);

      const request: BlockUserRequest = { email: user.email, reason: trimmed };

      this.api.blockUser(request).pipe(
        finalize(() => this.setBusy(user.email, false))
      ).subscribe({
        next: () => {
          this.snack.open(`Blocked ${user.name} ${user.surname}`, 'OK', { duration: 2500 });
          this.refresh();
        },
        error: () => {
          this.snack.open('Failed to block user', 'Dismiss', { duration: 3500 });
        }
      });
    });
  }

  unblock(user: UserProfile): void {
    this.setBusy(user.email, true);

    this.api.unblockUser(user.email).pipe(
      finalize(() => this.setBusy(user.email, false))
    ).subscribe({
      next: (res: BlockUserResponse) => {
        this.snack.open(res.message || `Unblocked ${user.name} ${user.surname}`, 'OK', { duration: 2500 });
        this.refresh();
      },
      error: () => {
        this.snack.open('Failed to unblock user', 'Dismiss', { duration: 3500 });
      }
    });
  }
}