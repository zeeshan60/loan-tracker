import { Component, computed, DestroyRef, inject, input, OnInit, signal } from '@angular/core';
import {
  IonButton,
  IonButtons, IonContent,
  IonHeader, IonInput, IonItem, IonList, IonSelect, IonSelectOption,
  IonTitle,
  IonToolbar,
  IonLabel,
  IonIcon,
} from '@ionic/angular/standalone';
import { FriendsStore } from '../../friends.store';
import { FriendWithBalance } from '../../model';
import { ShortenNamePipe } from '../../../pipes/shorten-name.pipe';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { SplitOptionsEnum } from '../../../define-expense/define-expense.component';
import { HttpClient } from '@angular/common/http';
import { HelperService } from '../../../helper.service';
import { Router } from '@angular/router';
import { DecimalPipe, NgClass } from '@angular/common';
import { ModalIndex, ModalService } from '../../../modal.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'mr-settle-up',
  templateUrl: './settle-up.component.html',
  styleUrls: ['./settle-up.component.scss'],
  standalone: true,
  imports: [
    IonButton,
    IonButtons,
    IonHeader,
    IonTitle,
    IonToolbar,
    ShortenNamePipe,
    IonContent,
    IonInput,
    IonItem,
    IonList,
    IonSelect,
    IonSelectOption,
    ReactiveFormsModule,
    NgClass,
    DecimalPipe,
    IonLabel,
    IonIcon,
  ],
})
export class SettleUpComponent implements OnInit {
  destroyRef = inject(DestroyRef);
  modalIndex = input.required<ModalIndex>();
  modalService = inject(ModalService);
  friendsStore = inject(FriendsStore);
  friend = input.required<FriendWithBalance>();
  readonly loading = signal(false);
  readonly http = inject(HttpClient);
  readonly helperService = inject<HelperService>(HelperService);
  readonly formBuilder = inject(FormBuilder);
  readonly otherBalances = computed(() => {
    const obj: { [key: string]: { isOwed: boolean, amount: number } } = {};
    this.friend().otherBalances?.forEach(({amount: balance}) => {
      obj[balance.currency] = { isOwed: balance.isOwed, amount: balance.amount }
    })
    return obj;
  })
  readonly router = inject(Router);
  readonly settleUpForm = this.formBuilder.group({
    balance: this.formBuilder.nonNullable.control({ currency: 'PKR', isOwed: true, amount: 0}, [Validators.required]),
    amount: this.formBuilder.nonNullable.control<number|null>(null, [Validators.required, Validators.min(1)]),
  });

  ngOnInit() {
    this.settleUpForm.get('balance')?.valueChanges
      .pipe(
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((balance) => {
        this.settleUpForm.get('amount')?.setValue(this.otherBalances()[balance.currency].amount)
        this.settleUpForm.get('amount').clearValidators()
        this.settleUpForm.get('amount').addValidators([Validators.required, Validators.min(1), Validators.max(balance.amount)])
        this.settleUpForm.get('amount').updateValueAndValidity();
      });
    this.settleUpForm.get('balance')?.setValue(this.friend().otherBalances?.[0]?.amount || { currency: 'PKR', isOwed: true, amount: 0})
  }

  async closePopup() {
    this.modalService.dismiss(this.modalIndex());
  }

  async submit() {
    if (!this.settleUpForm.valid) {
      this.settleUpForm.markAllAsTouched();
      await this.helperService.showToast('Please fill in the correct values');
      return;
    }

    const confirmation = await this.helperService.showConfirmAlert(
      `You are going to settle up everything with ${this.friend().name}.`, 'Yes'
    )
    if (confirmation.role == 'confirm') {
      const transaction = {
        currency: this.settleUpForm.get('balance')!.value.currency,
        amount: this.settleUpForm.get('amount')!.value,
        type: this.settleUpForm.get('balance')!.value.isOwed ? SplitOptionsEnum.TheyPaidToSettle : SplitOptionsEnum.YouPaidToSettle,
        transactionDate: (new Date()).toISOString(),
        description: 'settlement'
      }
      await this.friendsStore.settleUp(this.friend(), transaction);
      this.modalService.dismiss(this.modalIndex(), 'done', 'confirm');
    }
  }
}
