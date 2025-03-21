import {
  ChangeDetectionStrategy,
  Component,
  inject,
  input,
  model,
  OnInit,
  signal,
} from '@angular/core';
import {
  ActionSheetController,
  IonButton,
  IonButtons,
  IonContent, IonDatetime, IonDatetimeButton,
  IonHeader,
  IonInput, IonItem, IonLabel, IonList, IonModal, IonSelect, IonSelectOption, IonSpinner,
  IonTitle,
  IonToolbar, ModalController,
} from '@ionic/angular/standalone';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom, takeUntil } from 'rxjs';
import { Router } from '@angular/router';
import { HelperService } from '../helper.service';
import { FriendsStore } from '../friends/friends.store';
import { FriendWithBalance, Transaction } from '../friends/model';
import { SelectFriendComponent } from './select-friend/select-friend.component';
import { HttpClient } from '@angular/common/http';
import { CURRENCIES, PRIVATE_API } from '../constants';
import { ShortenNamePipe } from '../pipes/shorten-name.pipe';
import { toObservable } from '@angular/core/rxjs-interop';
import { ComponentDestroyedMixin } from '../component-destroyed.mixin';
import { DefineExpenseService } from './define-expense.service';

export enum SplitOptions {
  YouPaidSplitEqually = 'YouPaidSplitEqually',
  TheyPaidSplitEqually = 'TheyPaidSplitEqually',
  TheyOweYouAll = 'TheyOweYouAll',
  YouOweThemAll = 'YouOweThemAll',
  TheyPaidToSettle = 'TheyPaidToSettle',
  YouPaidToSettle = 'YouPaidToSettle',
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
    IonTitle,
    IonSpinner,
    IonButton,
    IonSelect,
    IonSelectOption,
    IonList,
    IonItem,
    IonLabel,
    ShortenNamePipe,
    IonModal,
    IonDatetimeButton,
    IonDatetime,
  ],
})
export class DefineExpenseComponent extends ComponentDestroyedMixin() implements OnInit {
  readonly friend = model<FriendWithBalance|null>(null);
  readonly loading = signal(false);
  readonly isUpdating = input(false);
  readonly transaction = input<Transaction>();
  readonly http = inject(HttpClient);
  readonly modalCtrl = inject(ModalController);
  readonly helperService = inject<HelperService>(HelperService);
  readonly defineExpenseService = inject(DefineExpenseService);
  readonly formBuilder = inject(FormBuilder);
  readonly actionSheetCtrl = inject(ActionSheetController);
  readonly friendsStore = inject(FriendsStore);
  readonly SplitOption = SplitOptions;
  readonly supportedCurrencies = CURRENCIES;
  readonly router = inject(Router);
  readonly defineExpenseForm = this.formBuilder.group({
    description: this.formBuilder.nonNullable.control('', [Validators.required, Validators.maxLength(1000)]),
    currency: this.formBuilder.nonNullable.control('PKR', [Validators.required]),
    amount: this.formBuilder.nonNullable.control<number|null>(null, [Validators.required, Validators.min(1), Validators.max(100000000)]),
    type: this.formBuilder.nonNullable.control({ value: SplitOptions.YouPaidSplitEqually, disabled: !this.friend() }, [Validators.required]),
    transactionDate: this.formBuilder.nonNullable.control((new Date()).toISOString(), [Validators.required]),
  });
  isOwed = () => {
    return [SplitOptions.YouPaidSplitEqually, SplitOptions.TheyOweYouAll]
      .includes(this.defineExpenseForm.value.type!);
  };
  constructor() {
    super();

    toObservable(this.friend)
      .pipe(
        takeUntil(this.componentDestroyed)
      )
      .subscribe(value => {
        if (!value) {
          this.defineExpenseForm.get('type')?.disable();
        } else {
          this.defineExpenseForm.get('type')?.enable();
        }
      })
  }

  async ngOnInit() {
    if (this.isUpdating()) {
      const formInitialValue = this.defineExpenseForm.getRawValue();
      this.defineExpenseForm.patchValue({
        description: this.transaction()?.description || formInitialValue.description,
        amount: this.transaction()?.totalAmount || formInitialValue.amount,
        currency: this.transaction()?.amountResponse.currency || formInitialValue.currency,
        type: this.transaction()?.splitType || formInitialValue.type,
        transactionDate: this.transaction()?.date || formInitialValue.transactionDate,
      });
    }

    if (!this.friend()) {
      const role = await this.chooseFriend();
      if (role !== 'confirm') {
        this.defineExpenseService.defineExpenseModalInstance?.dismiss();
      }
    }
  }

  canDismiss = async () => {
    const actionSheet = await this.actionSheetCtrl.create({
      header: 'Are you sure?',
      buttons: [
        {
          text: 'Yes',
          role: 'confirm',
        },
        {
          text: 'No',
          role: 'cancel',
        },
      ],
    });
    actionSheet.present();
    const { role } = await actionSheet.onWillDismiss();
    return role === 'confirm';
  };

  async closePopup() {
    if (!this.defineExpenseForm.dirty || await this.canDismiss()) {
      this.modalCtrl.dismiss();
    }
  }

  async onSubmit() {
    if (!this.friend()) {
      await this.helperService.showToast('Please select a friend first.');
      return;
    }
    if (this.defineExpenseForm.valid) {
      try {
        this.loading.set(true);
        const updatedExpense = await this.friendsStore.addUpdateExpense(
          this.friend()!,
          this.defineExpenseForm.getRawValue(),
          this.isUpdating() ? this.transaction() : undefined
        );
        this.modalCtrl.dismiss(updatedExpense, 'confirm');
      } catch (e) {} finally {
        this.loading.set(false);
      }
    } else {
      this.defineExpenseForm.markAllAsTouched();
      await this.helperService.showToast('Please fill in the correct values');
    }
  }

  async chooseFriend() {
    if (this.isUpdating()) {
      return;
    }
    this.defineExpenseService.selectFriendModalInstance = await this.modalCtrl.create({
      component: SelectFriendComponent,
      componentProps: {
        friend: this.friend()
      }
    })
    this.defineExpenseService.selectFriendModalInstance.present();

    const { data, role } = await this.defineExpenseService.selectFriendModalInstance.onWillDismiss();
    if (role === 'confirm') {
      this.friend.set(data['friend']);
    }
    return role;
  }

  async saveExpense(formValue: {
    description: string,
    currency: string,
    amount: number|null,
    type: SplitOptions,
    transactionDate: string,
  }) {
    formValue.transactionDate = (new Date(formValue.transactionDate)).toISOString();
    if (this.isUpdating()) {
      return firstValueFrom(
        this.http.put(`${PRIVATE_API}/transactions/update/transactionId/${this.transaction()?.transactionId}`, {
          ...formValue,
        })
      );
    } else {
      return firstValueFrom(this.http.post(`${PRIVATE_API}/transactions/add`, {
        ...formValue,
        recipientId: this.friend()!.friendId
      }));
    }
  }
}
