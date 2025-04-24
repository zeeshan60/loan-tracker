import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import {
  IonButton,
  IonButtons, IonContent,
  IonHeader, IonInput, IonItem, IonList, IonSelect, IonSelectOption,
  IonSpinner,
  IonTitle,
  IonToolbar,
} from '@ionic/angular/standalone';
import { FriendsStore } from '../../friends.store';
import { FriendWithBalance } from '../../model';
import { ShortenNamePipe } from '../../../pipes/shorten-name.pipe';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { SplitOptions } from '../../../define-expense/define-expense.component';
import { HttpClient } from '@angular/common/http';
import { HelperService } from '../../../helper.service';
import { Router } from '@angular/router';
import { ComponentDestroyedMixin } from '../../../component-destroyed.mixin';
import { takeUntil } from 'rxjs';
import { NgClass } from '@angular/common';
import { ModalIndex, ModalService } from '../../../modal.service';

@Component({
  selector: 'app-settle-up',
  templateUrl: './settle-up.component.html',
  styleUrls: ['./settle-up.component.scss'],
  standalone: true,
  imports: [
    IonButton,
    IonButtons,
    IonHeader,
    IonSpinner,
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
  ],
})
export class SettleUpComponent extends ComponentDestroyedMixin() implements OnInit {
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
    this.friend().otherBalances?.forEach(balance => {
      obj[balance.currency] = { isOwed: balance.isOwed, amount: balance.amount }
    })
    return obj;
  })
  readonly router = inject(Router);
  readonly settleUpForm = this.formBuilder.group({
    balance: this.formBuilder.nonNullable.control({ currency: 'PKR', isOwed: true, amount: 0}, [Validators.required]),
    amount: this.formBuilder.nonNullable.control<number|null>(null, [Validators.required, Validators.min(1), Validators.max(100000000)]),
  });

  constructor() {
    super();
  }

  ngOnInit() {
    this.settleUpForm.get('balance')?.valueChanges
      .pipe(
        takeUntil(this.componentDestroyed)
      )
      .subscribe((currency) => {
        this.settleUpForm.get('amount')?.setValue(this.otherBalances()[currency.currency].amount)
      });
    this.settleUpForm.get('balance')?.setValue(this.friend().otherBalances?.[0] || { currency: 'PKR', isOwed: true, amount: 0})
  }

  async closePopup() {
    this.modalService.dismiss(this.modalIndex());
  }

  async submit() {
    if (this.settleUpForm.valid) {
      try {
        this.loading.set(true);
        const transaction = {
          currency: this.settleUpForm.get('balance')!.value.currency,
          amount: this.settleUpForm.get('amount')!.value,
          type: this.settleUpForm.get('balance')!.value.isOwed ? SplitOptions.TheyPaidToSettle : SplitOptions.YouPaidToSettle,
          transactionDate: (new Date()).toISOString(),
          description: 'settlement'
        }
        await this.friendsStore.settleUp(this.friend(), transaction);
        this.modalService.dismiss(this.modalIndex(), 'done', 'confirm');
      } catch (e) {} finally {
        this.loading.set(false);
      }
    } else {
      this.settleUpForm.markAllAsTouched();
      await this.helperService.showToast('Please fill in the correct values');
    }
  }
}
