import { Component, EventEmitter, Input, Output, OnInit, OnChanges } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { LinkedPassengersData } from '../model/link-passengers.interface';

@Component({
  selector: 'app-link-passenger-form',
  templateUrl: './link-passenger-form.component.html',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  host: {
    'class': 'pointer-events-auto block'
  }
})
export class LinkPassengerFormComponent implements OnInit, OnChanges {
  @Input() showLinkForm: boolean = false;
  @Output() toggleForm = new EventEmitter<void>();
  @Output() linkPassengersRequest = new EventEmitter<LinkedPassengersData>();
  @Input() hideButton: boolean = false;

  linkForm: FormGroup;

  constructor(private fb: FormBuilder) {
    this.linkForm = this.fb.group({
      emails: this.fb.array([this.createEmailControl()])
    });
  }

  ngOnInit(): void {
    //console.log('LinkPassengerFormComponent initialized');
  }

  ngOnChanges(): void {
    //console.log('showLinkForm changed to:', this.showLinkForm);
  }

  handleButtonClick(): void {
    //console.log('BUTTON CLICKED - showLinkForm before:', this.showLinkForm);
    this.toggleForm.emit();
    //console.log('toggleForm emitted');
  }

  get emailsArray(): FormArray {
    return this.linkForm.get('emails') as FormArray;
  }

  createEmailControl(): FormGroup {
    return this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  addEmail(): void {
    console.log('Add email clicked');
    this.emailsArray.push(this.createEmailControl());
  }

  removeEmail(index: number): void {
    console.log('Remove email clicked:', index);
    if (this.emailsArray.length > 1) {
      this.emailsArray.removeAt(index);
    }
  }

  onSubmit(): void {
    if (this.linkForm.valid) {
      const emails = this.emailsArray.controls.map(
        control => control.get('email')?.value
      );
      
      this.linkPassengersRequest.emit({ emails });
      this.closeForm();
    }
  }

  closeForm(): void {
    console.log('Close form clicked');
    this.toggleForm.emit();
    this.resetForm();
  }

  resetForm(): void {
    this.emailsArray.clear();
    this.emailsArray.push(this.createEmailControl());
  }
}
