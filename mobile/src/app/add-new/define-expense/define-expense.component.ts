import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { IonicModule, NavParams } from '@ionic/angular';
import {
  IonBackButton, IonButton,
  IonButtons,
  IonContent,
  IonHeader, IonIcon,
  IonInput, IonItem, IonList, IonSelect, IonSelectOption, IonSpinner,
  IonTitle,
  IonToolbar,
} from '@ionic/angular/standalone';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HelperService } from '../../helper.service';

@Component({
  selector: 'app-define-expense',
  templateUrl: './define-expense.component.html',
  styleUrls: ['./define-expense.component.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    IonInput,
    ReactiveFormsModule,
    IonContent,
    IonHeader,
    IonToolbar,
    IonButtons,
    IonBackButton,
    IonTitle,
    IonSpinner,
    IonButton,
    IonSelect,
    IonSelectOption,
    IonIcon,
    IonList,
    IonItem,
  ],
})
export class DefineExpenseComponent  implements OnInit {
  readonly loading = signal(false);
  readonly helperService = inject(HelperService);
  readonly formBuilder = inject(FormBuilder);
  navProps = inject(NavParams).data;
  defineExpenseForm = this.formBuilder.group({
    description: this.formBuilder.control('', [Validators.required]),
    currency: ['PKR', Validators.required],
    expense: [null, [Validators.required]],
    whoOwesWho: this.formBuilder.nonNullable.control('amrood', [Validators.required]),
  });
  isOwed = () => {
    return ['amrood', 'oranges'].includes(this.defineExpenseForm.value.whoOwesWho!);
  };
  constructor() { }

  ngOnInit() {
    console.log(this.navProps);
  }

  async onSubmit() {
    if (this.defineExpenseForm.valid) {
      try {
        this.loading.set(true);
      } catch (e) {
        await this.helperService.showToast('Unable to add friend at the moment');
      } finally {
        this.loading.set(false);
      }
    } else {
      this.defineExpenseForm.markAllAsTouched();
      await this.helperService.showToast('Please fill in the correct values');
    }
  }
}
