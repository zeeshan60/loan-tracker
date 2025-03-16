import { Component, inject, input, OnInit } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import {
  IonAvatar,
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel,
  IonList,
  IonNav,
  IonTitle,
  IonToolbar,
  ModalController,
} from '@ionic/angular/standalone';
import { FriendWithBalance } from '../model';
import { TransactionDetailsComponent } from '../transaction-details/transaction-details.component';
import { FriendsStore } from '../friends.store';
import { shortName } from '../../utility-functions';
import { ShortenNamePipe } from '../../pipes/shorten-name.pipe';
import { DateFormatPipe } from '../../pipes/date-format.pipe';
import { DefineExpenseComponent, SplitOptions } from '../../define-expense/define-expense.component';

@Component({
  selector: 'app-friend-transactions',
  templateUrl: './friend-transactions.component.html',
  styleUrls: ['./friend-transactions.component.scss'],
  standalone: true,
  imports: [
    CurrencyPipe,
    IonAvatar,
    IonButton,
    IonButtons,
    IonContent,
    IonHeader,
    IonIcon,
    IonItem,
    IonLabel,
    IonList,
    IonTitle,
    IonToolbar,
    IonBackButton,
    ShortenNamePipe,
    DateFormatPipe,
  ],
})
export class FriendTransactionsComponent {
  readonly friend = input.required<FriendWithBalance>();
  readonly nav = inject(IonNav);
  readonly friendsStore = inject(FriendsStore);
  readonly transactions = this.friendsStore.selectedTransactions;
  readonly modalCtrl = inject(ModalController);
  protected readonly shortName = shortName;

  constructor() {}

  openTransactionDetails(transaction: any) {
    this.nav.push(TransactionDetailsComponent, {
      transaction,
      friend: this.friend()
    });
  }

  async addExpense() {
    const modal = await this.modalCtrl.create({
      component: DefineExpenseComponent,
      componentProps: { friend: this.friend() }
    })
    modal.present();
  }

  ionViewWillEnter() {
    this.friendsStore.setSelectedFriend(this.friend());
  }

  settleUp() {
    this.friendsStore.settleUp(this.friend(), {
      amount: this.friend().mainBalance!.amount,
      currency: this.friend().mainBalance!.currency,
      type: this.friend().mainBalance!.isOwed ? SplitOptions.TheyPaidToSettle : SplitOptions.YouPaidToSettle,
      transactionDate: (new Date()).toISOString(),
      description: 'settlement'
    })
  }
}
