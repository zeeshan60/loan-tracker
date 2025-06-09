import { Component, computed, inject, input, Signal, signal } from '@angular/core';
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
} from '@ionic/angular/standalone';
import { TransactionDetailsComponent } from '../transaction-details/transaction-details.component';
import { FriendsStore } from '../friends.store';
import { ShortenNamePipe } from '../../pipes/shorten-name.pipe';
import { DateFormatPipe } from '../../pipes/date-format.pipe';
import { DefineExpenseComponent } from '../../define-expense/define-expense.component';
import { SettleUpComponent } from './settle-up/settle-up.component';
import { AddFriendComponent } from '../../add-friend/add-friend.component';
import { OverallBalanceComponent } from '../overall-balance/overall-balance.component';
import { ModalService } from '../../modal.service';

@Component({
  selector: 'mr-friend-transactions',
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
    OverallBalanceComponent,
  ],
})
export class FriendTransactionsComponent {
  readonly friend = computed(() => this.friendsStore.selectedFriend());
  readonly isLoading = signal(false);
  readonly nav = inject(IonNav);
  readonly friendsStore = inject(FriendsStore);
  readonly transactions = this.friendsStore.selectedTransactions;
  readonly modalService = inject(ModalService);

  constructor() {}

  openTransactionDetails(transaction: any) {
    this.nav.push(TransactionDetailsComponent, {
      transactionId: transaction.transactionId,
      friend: this.friend()
    });
  }

  async addExpense() {
    this.modalService.showModal({
      component: DefineExpenseComponent,
      componentProps: { friend: this.friend() }
    })
  }

  async deleteFriend() {
    await this.friendsStore.deleteFriend(this.friend());
    await this.nav.pop();
  }

  async editFriendInfo() {
    this.modalService.showModal({
      component: AddFriendComponent,
      componentProps: {
        friend: this.friend(),
        isUpdating: true
      }
    })
  }

  async settleUp() {
    console.log(this.friend());
    this.modalService.showModal({
      component: SettleUpComponent,
      componentProps: {
        friend: this.friend()
      }
    })
  }
}
