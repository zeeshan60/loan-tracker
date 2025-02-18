import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { NavParams } from '@ionic/angular';
import {
  IonBackButton, IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonInput, IonItem, IonList, IonSelect, IonSelectOption, IonSpinner,
  IonTitle,
  IonToolbar,
} from '@ionic/angular/standalone';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HelperService } from '../../helper.service';
import { delay, firstValueFrom, timer } from 'rxjs';
import { Router } from '@angular/router';
import { FriendsStore } from '../../friends/friends.store';

export enum SplitOptions {
  YouPaidSplitEqually,
  TheyPaidSplitEqually,
  TheyOweYouAll,
  YouOweThemAll
}

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
    IonList,
    IonItem,
  ],
})
export class DefineExpenseComponent  implements OnInit {
  readonly loading = signal(false);
  readonly helperService = inject(HelperService);
  readonly formBuilder = inject(FormBuilder);
  readonly friendsStore = inject(FriendsStore);
  readonly SplitOption = SplitOptions;
  readonly supportedCurrencies = ['PKR', 'USD', 'SGD'];
  readonly router = inject(Router);
  navProps = inject(NavParams).data;
  defineExpenseForm = this.formBuilder.group({
    description: this.formBuilder.nonNullable.control('', [Validators.required, Validators.maxLength(1000)]),
    currency: this.formBuilder.nonNullable.control('PKR', [Validators.required]),
    expense: this.formBuilder.nonNullable.control(null, [Validators.required, Validators.min(1)]),
    whoOwesWho: this.formBuilder.nonNullable.control(SplitOptions.YouPaidSplitEqually, [Validators.required]),
  });
  isOwed = () => {
    return [SplitOptions.YouPaidSplitEqually, SplitOptions.TheyOweYouAll]
      .includes(this.defineExpenseForm.value.whoOwesWho!);
  };
  constructor() { }

  ngOnInit() {
  }

  async onSubmit() {
    if (this.defineExpenseForm.valid) {
      try {
        this.loading.set(true);
        await this.saveExpense(this.defineExpenseForm.getRawValue());
        await this.friendsStore.loadFriends();
        this.router.navigate(['/'])
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

  async saveExpense(formValue: {
    description: string,
    currency: string,
    expense: number|null,
    whoOwesWho: SplitOptions
  }) {
    return firstValueFrom(timer(1000));
  }
}
