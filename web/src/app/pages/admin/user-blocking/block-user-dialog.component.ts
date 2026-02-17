import { FormControl, Validators } from '@angular/forms';
import { UserProfile } from '../../../infrastructure/rest/user.service';
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog'; // correct [web:31][web:35]
import { ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-block-user-dialog',
  templateUrl: './block-user-dialog.component.html',
  imports: [CommonModule, ReactiveFormsModule]
})
export class BlockUserDialogComponent {
  reason = new FormControl<string>('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(500)]
  });

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: { user: UserProfile },
    private ref: MatDialogRef<BlockUserDialogComponent>
  ) {}

  cancel() {
    this.ref.close(undefined);
  }

  confirm() {
    if (this.reason.invalid) {
      this.reason.markAsTouched();
      return;
    }
    this.ref.close(this.reason.value);
  }
}
