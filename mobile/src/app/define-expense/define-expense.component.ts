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
  IonHeader, IonIcon,
  IonInput, IonItem, IonLabel, IonList, IonModal, IonSelect, IonSelectOption, IonSpinner,
  IonTitle,
  IonToolbar,
} from '@ionic/angular/standalone';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntil } from 'rxjs';
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
import { AuthStore } from '../login/auth.store';
import { FakeDropdownComponent } from '../fake-dropdown/fake-dropdown.component';
import { CurrenciesModalComponent } from '../currencies-modal/currencies-modal.component';
import { ModalIndex, ModalService } from '../modal.service';

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
    IonIcon,
    FakeDropdownComponent,
  ],
})
export class DefineExpenseComponent extends ComponentDestroyedMixin() implements OnInit {
  readonly friend = model<FriendWithBalance|null>(null);
  readonly loading = signal(false);
  readonly isUpdating = input(false);
  readonly modalIndex = input.required<ModalIndex>();
  readonly transaction = input<Transaction>();
  readonly http = inject(HttpClient);
  readonly helperService = inject<HelperService>(HelperService);
  readonly modalService = inject(ModalService);
  readonly defineExpenseService = inject(DefineExpenseService);
  readonly formBuilder = inject(FormBuilder);
  readonly actionSheetCtrl = inject(ActionSheetController);
  readonly friendsStore = inject(FriendsStore);
  readonly authStore = inject(AuthStore);
  readonly SplitOption = SplitOptions;
  readonly supportedCurrencies = CURRENCIES;
  readonly router = inject(Router);
  readonly defineExpenseForm = this.formBuilder.group({
    description: this.formBuilder.nonNullable.control('', [Validators.required, Validators.maxLength(1000)]),
    currency: this.formBuilder.nonNullable.control(CURRENCIES[0], [Validators.required]),
    amount: this.formBuilder.nonNullable.control<number|null>(null, [Validators.required, Validators.min(1), Validators.max(99999999999)]),
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
        currency: this.transaction()?.amount.currency || formInitialValue.currency,
        type: this.transaction()?.splitType || formInitialValue.type,
        transactionDate: this.transaction()?.date,
      });
    } else {
      this.setAppropriateCurrency();
    }

    if (!this.friend()) {
      const role = await this.chooseFriend();
      if (role !== 'confirm') {
        this.modalService.dismiss(this.modalIndex());
      }
    }
  }

  /**
   * This method tries to find the most appropriate currency to pre-select for the transaction
   * @private
   */
  private setAppropriateCurrency() {
    let currency = CURRENCIES[0];
    if (this.authStore.user()?.currency) {
      currency = this.authStore.user()?.currency!;
    } else if (this.friend()?.otherBalances?.[0].currency) {
      currency = this.friend()?.otherBalances?.[0].currency!;
    } else if (CURRENCIES.includes(this.authStore.region()?.currency || '')) {
      currency = this.authStore.region()?.currency!;
    }
    this.defineExpenseForm.get('currency')?.setValue(currency);
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
      this.modalService.dismiss(this.modalIndex())
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
        this.modalService.dismiss(this.modalIndex(), updatedExpense, 'confirm');
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
      return null;
    }

    const modalIndex = await this.modalService.showModal({
      component: SelectFriendComponent,
      componentProps: {
        friend: this.friend()
      }
    })

    const { data: friend, role } = await this.modalService.onWillDismiss<FriendWithBalance>(modalIndex);
    if (role === 'confirm') {
      this.friend.set(friend);
    }
    return role;
  }

  async chooseCurrency() {
    const modalIndex = await this.modalService.showModal({
      component: CurrenciesModalComponent,
      handleBehavior: 'cycle',
      initialBreakpoint: 0.5,
      breakpoints: [0.25, 0.5, 0.75]
    })
    await this.modalService.onWillDismiss<string>(modalIndex)
      .then((value) => {
        if (value.role === 'confirm') {
          console.log(value.data); // todo
        }
      })
  }
}
