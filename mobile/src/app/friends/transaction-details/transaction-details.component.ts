import { Component, computed, inject, input, model } from '@angular/core';
import {
  IonAvatar,
  IonBackButton, IonButton,
  IonButtons,
  IonContent,
  IonHeader, IonIcon, IonItem, IonLabel,
  IonList, IonNav,
  IonTitle,
  IonToolbar, ModalController,
} from '@ionic/angular/standalone';
import { FriendWithBalance, HistoryChangeType, Transaction } from '../model';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { ShortenNamePipe } from '../../pipes/shorten-name.pipe';
import { DefineExpenseComponent, SplitOptions } from '../../define-expense/define-expense.component';
import { FriendsStore } from '../friends.store';
import { HelperService } from '../../helper.service';

const historyChangeType = {
  [HistoryChangeType.DESCRIPTION]: "description",
  [HistoryChangeType.TOTAL_AMOUNT]: "total amount",
  [HistoryChangeType.CURRENCY]: "currency",
  [HistoryChangeType.SPLIT_TYPE]: "split type",
  [HistoryChangeType.DELETED]: "deleted",
  [HistoryChangeType.TRANSACTION_DATE]: "transaction date"
}

@Component({
    selector: 'app-transaction-details',
    templateUrl: './transaction-details.component.html',
    styleUrls: ['./transaction-details.component.scss'],
    standalone: true,
  imports: [
    IonBackButton,
    IonButtons,
    IonContent,
    IonHeader,
    IonList,
    IonTitle,
    IonToolbar,
    IonLabel,
    CurrencyPipe,
    IonAvatar,
    IonIcon,
    IonItem,
    ShortenNamePipe,
    IonButton,
    DatePipe,
  ],
})
export class TransactionDetailsComponent {
  readonly SplitOptions = SplitOptions;
  readonly friendsStore = inject(FriendsStore);
  readonly helperService = inject(HelperService);
  readonly modalCtrl = inject(ModalController);
  readonly nav = inject(IonNav);
  readonly transactionId = input.required<string>();
  readonly friend = input.required<FriendWithBalance>();
  readonly latestTransaction = computed(() => {
    let found: Transaction | undefined;
    this.friendsStore.selectedTransactions().some((transactionsByMonth) => {
      return transactionsByMonth.transactions.some((transaction) => {
        if (transaction.transactionId === this.transactionId()) {
          found = transaction;
          return true;
        }
        return false;
      })
    })
    return found;
  })
  protected readonly historyChangeType = historyChangeType;
  protected readonly HistoryChangeType = HistoryChangeType;
  protected splitOptionsText : {[key: string]: string} = {
    [SplitOptions.YouPaidSplitEqually]: 'You paid split equally',
    [SplitOptions.TheyPaidSplitEqually]: 'They paid split equally',
    [SplitOptions.TheyOweYouAll]: 'They owe you all',
    [SplitOptions.YouOweThemAll]: 'You owe them all',
    [SplitOptions.TheyPaidToSettle]: 'They paid to settle',
    [SplitOptions.YouPaidToSettle]: 'You paid to settle',
  }

  constructor() {
  }

  async deleteTransaction() {
    const response = await this.helperService.showConfirmAlert()
    if (response.role === 'confirm') {
      try {
        await this.friendsStore.deleteTransaction(this.transactionId());
        this.nav.pop();
      } catch (e) {}
    }
  }

  async updateTransaction() {
    const modal = await this.modalCtrl.create({
      component: DefineExpenseComponent,
      componentProps: {
        friend: this.friend(),
        isUpdating: true,
        transaction: this.latestTransaction(),
      }
    })
    modal.present();
  }
}
